package com.cericlabs.util.locks;

import java.util.*;


/**
 * The ReadWriteLock class implements a simple read/write lock which allows threads to acquire
 * read locks so long as no thread has a write lock, and vice versa. Threads holding a read or
 * write lock can acquire the opposite lock only if they are the only thread holding said lock.
 *
 * @author Chris "Ceiu" Rog
 */
public class ReadWriteSignalLock {

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Mutable integer class for maintaining lock counts without object cycling. */
	private static final class MutableInt {
		public int value;

		public MutableInt(int initial_value) {
			this.value = initial_value;
		}

		public int increment() {
			return ++this.value;
		}

		public int decrement() {
			return --this.value;
		}

		public int setValue(int value) {
			return (this.value = value);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////

	/** Mini lock for managing lock counts and such.
	 *	Note: Operations are NOT synchronized. This should be done by the
	 *		  parent class.
	 */
	private static final class MiniLock {
		private final int max_owners;
		private final Map<Thread, MutableInt> owners;

		public MiniLock(int max_owners) {
			if(max_owners < 0)
				throw new IllegalArgumentException("max_owners");

			this.max_owners = max_owners;
			this.owners = new HashMap<Thread, MutableInt>();
		}

		public boolean acquire(Thread thread) {
			MutableInt counter = this.owners.get(thread);

			if(counter == null) {
				if(this.max_owners > 0 && this.owners.size() >= this.max_owners)
					return false;

				this.owners.put(thread, (counter = new MutableInt(0)));
			}

			counter.increment();
			return true;
		}

		public boolean acquire(Thread thread, int lock_level) {
			MutableInt counter = this.owners.get(thread);

			if(counter == null) {
				if(this.max_owners > 0 && this.owners.size() >= this.max_owners)
					return false;

				this.owners.put(thread, (counter = new MutableInt(0)));
			}

			counter.setValue(lock_level);
			return true;
		}

		public boolean release(Thread thread) {
			MutableInt counter = this.owners.get(thread);
			if(counter == null)	throw new IllegalMonitorStateException();

			if(counter.decrement() <= 0) {
				this.owners.remove(thread);
				return true;
			}

			return false;
		}

		public int releaseAll(Thread thread) {
			MutableInt counter = this.owners.remove(thread);
			return counter != null ? counter.value : 0;
		}

		public int getLockLevel(Thread thread) {
			MutableInt counter = this.owners.get(thread);
			return counter != null ? counter.value : 0;
		}

		public int getOwnerCount() {
			return this.owners.size();
		}
	}


////////////////////////////////////////////////////////////////////////////////////////////////////

	private final MiniLock read_lock;
	private final MiniLock write_lock;
	private int waiting_threads;

	private final Map<Thread, MutableInt> signals;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new ReadWriteLock.
	 */
	public ReadWriteSignalLock() {
		this.read_lock = new MiniLock(0);
		this.write_lock = new MiniLock(1);
		this.waiting_threads = 0;

		this.signals = new HashMap<Thread, MutableInt>();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Attempts to acquire a read lock, waiting for the lock to be acquired. This method is
	 * identical to calling acquireReadLock with the wait parameter set to true.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireReadLock() {
		return this.acquireReadLock(true);
	}

	/**
	 * Attempts to acquire a read lock, optionally waiting for the lock to be acquired. If the
	 * wait parameter is set to false, this method will fail immediately if the lock is unable to
	 * be acquired.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireReadLock(boolean wait) {
		Thread cthread = Thread.currentThread();

		// Wait for write lock...
		while(this.write_lock.getOwnerCount() > 0 && this.write_lock.getLockLevel(cthread) == 0) {
			if(!wait) return false;
			try { this.wait(); } catch(InterruptedException e) { /* Do nothing. */ }
		}

		// Acquire read lock...
		while(!this.read_lock.acquire(cthread)) {
			if(!wait) return false;
			try { this.wait(); } catch(InterruptedException e) { /* Do nothing. */ }
		}

		// We got it!
		return true;
	}

	/**
	 * Attempts to acquire a read lock, waiting for the lock to be acquired. This method is
	 * identical to calling acquireReadLock with the wait parameter set to true.
	 *
	 * @throws InterruptedException
	 *	if the current thread is interrupted while attempting to acquire a lock.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireReadLockInt() throws InterruptedException {
		return this.acquireReadLockInt(true);
	}

	/**
	 * Attempts to acquire a read lock, optionally waiting for the lock to be acquired. If the
	 * wait parameter is set to false, this method will fail immediately if the lock is unable to
	 * be acquired.
	 *
	 * @throws InterruptedException
	 *	if the current thread is interrupted while attempting to acquire a lock.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireReadLockInt(boolean wait) throws InterruptedException {
		Thread cthread = Thread.currentThread();

		// Wait for write lock...
		while(this.write_lock.getOwnerCount() > 0 && this.write_lock.getLockLevel(cthread) == 0) {
			if(!wait) return false;
			this.wait();
		}

		// Acquire read lock...
		while(!this.read_lock.acquire(cthread)) {
			if(!wait) return false;
			this.wait();
		}

		// We got it!
		return true;
	}

	/**
	 * Releases a read lock owned by the current thread.
	 *
	 * @throws IllegalMonitorStateException
	 *	If the current thread does not hold any write locks.
	 */
	public synchronized void releaseReadLock() {
		Thread cthread = Thread.currentThread();

		// Release read lock. If the lock was fully released, notify any waiting threads.
		if(this.read_lock.release(cthread))
			this.notifyAll();
	}

	/**
	 * Returns the read lock level of the specified thread holds.
	 *
	 * @param thread
	 *	The thread for which to check the read level.
	 *
	 * @return
	 *	The thread's current read level.
	 */
	public synchronized int getReadLockLevel(Thread thread) {
		if(thread == null)
			throw new IllegalArgumentException("thread");

		return this.read_lock.getLockLevel(thread);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Attempts to acquire a write lock, waiting for the lock to be acquired. This method is
	 * identical to calling acquireWriteLock with the wait parameter set to true.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireWriteLock() {
		return this.acquireWriteLock(true);
	}

	/**
	 * Attempts to acquire a write lock, optionally waiting for the lock to be acquired. If the
	 * wait parameter is set to false, this method will fail immediately if the lock is unable to
	 * be acquired.
	 *
	 * @param wait
	 *	Whether or not the current thread should wait until the lock is acquired.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireWriteLock(boolean wait) {
		try {
			// Increment the number of threads waiting for a write lock...
			++this.waiting_threads;

			Thread cthread = Thread.currentThread();

			// Wait for readers to release their locks...
			while(this.read_lock.getOwnerCount() > (this.read_lock.getLockLevel(cthread) > 0 ? 1 : 0)) {
				if(!wait) return false;
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing. */ }
			}

			// Acquire write lock...
			while(!this.write_lock.acquire(cthread)) {
				if(!wait) return false;
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing. */ }
			}

			// We got it!
			return true;
		} finally {
			// Clear acquiring flag...
			--this.waiting_threads;
		}
	}

	/**
	 * Attempts to acquire a write lock, waiting for the lock to be acquired. This method is
	 * identical to calling acquireWriteLock with the wait parameter set to true.
	 *
	 * @throws InterruptedException
	 *	if the current thread is interrupted while attempting to acquire a lock.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireWriteLockInt() throws InterruptedException {
		return this.acquireWriteLockInt(true);
	}

	/**
	 * Attempts to acquire a write lock, optionally waiting for the lock to be acquired. If the
	 * wait parameter is set to false, this method will fail immediately if the lock is unable to
	 * be acquired.
	 *
	 * @param wait
	 *	Whether or not the current thread should wait until the lock is acquired.
	 *
	 * @throws InterruptedException
	 *	if the current thread is interrupted while attempting to acquire a lock.
	 *
	 * @return
	 *	True if the lock was acquired; false otherwise.
	 */
	public synchronized boolean acquireWriteLockInt(boolean wait) throws InterruptedException {
		try {
			// Increment the number of threads waiting for a write lock...
			++this.waiting_threads;

			Thread cthread = Thread.currentThread();

			// Wait for readers to release their locks...
			while(this.read_lock.getOwnerCount() > (this.read_lock.getLockLevel(cthread) > 0 ? 1 : 0)) {
				if(!wait) return false;
				this.wait();
			}

			// Acquire write lock...
			while(!this.write_lock.acquire(cthread)) {
				if(!wait) return false;
				this.wait();
			}

			// We got it!
			return true;
		} finally {
			// Clear acquiring flag...
			--this.waiting_threads;
		}
	}

	/**
	 * Releases a write lock owned by the current thread.
	 *
	 * @throws IllegalMonitorStateException
	 *	If the current thread does not hold any write locks.
	 */
	public synchronized void releaseWriteLock() {
		Thread cthread = Thread.currentThread();

		// Release write lock. If the lock was fully released, notify any waiting threads.
		if(this.write_lock.release(cthread))
			this.notifyAll();
	}

	/**
	 * Returns the write lock level of the specified thread holds.
	 *
	 * @param thread
	 *	The thread for which to check the write level.
	 *
	 * @return
	 *	The thread's current write level.
	 */
	public synchronized int getWriteLockLevel(Thread thread) {
		if(thread == null)
			throw new IllegalArgumentException("thread");

		return this.write_lock.getLockLevel(thread);
	}

	/**
	 * Gets the number of threads that are waiting to acquire a write lock.
	 *
	 * @return
	 *	The number of threads attempting to get a write lock.
	 */
	public synchronized int getWaitingThreadCount() {
		return this.waiting_threads;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public synchronized boolean waitForSignal() {
		return this.waitForSignal(0);
	}

	public synchronized boolean waitForSignal(long duration) {
		if(duration < 0)
			throw new IllegalArgumentException("duration");

		// Temporarily give up our locks...
		Thread cthread = Thread.currentThread();

		int read_level = this.read_lock.releaseAll(cthread);
		int write_level = this.write_lock.releaseAll(cthread);

		if(read_level + write_level > 0) this.notifyAll();

		// Wait for a signal...
		MutableInt signal = new MutableInt(0);
		boolean inf_wait = (duration == 0);
		long calltime = System.currentTimeMillis();

		this.signals.put(cthread, signal);

		while(signal.value == 0 && (inf_wait || (duration = duration - (System.currentTimeMillis() - calltime)) > 0)) {
			try { this.wait(duration); } catch(InterruptedException e) { /* Do nothing */ }
		}

		this.signals.remove(cthread);

		// Reacquire locks and resume...
		if(write_level > 0) {
			while(!this.write_lock.acquire(cthread, write_level)) {
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing */ }
			}
		}

		if(read_level > 0) {
			while(!this.read_lock.acquire(cthread, read_level)) {
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing */ }
			}
		}

		// Return!
		return (signal.value > 0);
	}

	public synchronized boolean waitForSignalInt() throws InterruptedException {
		return this.waitForSignalInt(0);
	}

	public synchronized boolean waitForSignalInt(long duration) throws InterruptedException {
		if(duration < 0)
			throw new IllegalArgumentException("duration");

		// Temporarily give up our locks...
		Thread cthread = Thread.currentThread();

		int read_level = this.read_lock.releaseAll(cthread);
		int write_level = this.write_lock.releaseAll(cthread);

		if(read_level + write_level > 0) this.notifyAll();

		// Wait for a signal...
		MutableInt signal = new MutableInt(0);
		boolean inf_wait = (duration == 0);
		long calltime = System.currentTimeMillis();

		this.signals.put(cthread, signal);

		while(signal.value == 0 && (inf_wait || (duration = duration - (System.currentTimeMillis() - calltime)) > 0))
			this.wait(duration);

		this.signals.remove(cthread);

		// Reacquire locks and resume...
		if(write_level > 0)
			while(!this.write_lock.acquire(cthread, write_level))
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing */ }

		if(read_level > 0)
			while(!this.read_lock.acquire(cthread, read_level))
				try { this.wait(); } catch(InterruptedException e) { /* Do nothing */ }

		// Return!
		return (signal.value > 0);
	}

	public synchronized void postSignal() {
		for(MutableInt signal : this.signals.values()) signal.increment();
		this.notifyAll();
	}

}