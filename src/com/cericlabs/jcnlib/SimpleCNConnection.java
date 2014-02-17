package com.cericlabs.jcnlib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.util.locks.*;


/**
 * The SimpleCNConnection implements the standard functionality of the CNConnection. Applications
 * that require additional functionality should either extend or decorate this class.
 *
 * @author Chris "Ceiu" Rog
 */
public class SimpleCNConnection extends CNConnection {

	/** Default size of the input buffer */
	private static final int INPUT_BUFFER_SIZE = 1024;

	// State management...
	private State state;					// Connection state
	private ReadWriteSignalLock lock;		// Status read/write lock
	private List<StateListener> listeners;	// State listeners

	// Socket variables...
	private Socket socket;					// Socket! May be null.

	private SocketAddress bind_addr;		// The address/port we should bind to.
	private int sock_timeout;				// Read timeout value.

	private InputStream reader;				// Socket input stream
	private OutputStream writer;			// Socket output stream

	// Buffer variables...
	private byte[] buffer;					// Input buffer
	private int buf_index, buf_size;		// Index/size of data in buffer (index = scan position, size = available)

////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new basic chatnet connection bound to a system-assigned address.
	 */
	public SimpleCNConnection() {
		this(null);
	}

	/**
	 * Creates a new basic chatnet connection bound to the address specified.
	 *
	 * @param bind_addr
	 *	The local address to bind to. Only necessary if the machine jcnlib is running on has
	 *	multiple network adapters and must select which to use to connect.
	 */
	public SimpleCNConnection(SocketAddress bind_addr) {
		// State stuff...
		this.state = State.DISCONNECTED;
		this.lock = new ReadWriteSignalLock();
		this.listeners = new CopyOnWriteArrayList<StateListener>();

		// Socket stuff...
		this.socket = null;

		this.bind_addr = bind_addr;
		this.sock_timeout = JCNLib.CONNECTION_TIMEOUT;

		this.reader = null;
		this.writer = null;

		// Input buffer stuff...
		this.buffer = new byte[INPUT_BUFFER_SIZE];
	}

////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean connect(String host, int port) {
		return this.connect(new InetSocketAddress(host, port));
	}

	@Override
	public boolean connect(InetSocketAddress address) {
		if(address == null)
			throw new IllegalArgumentException("address");

		boolean rla = false;
		boolean wla = false;

		try {
			// Acquire write lock...
			while(!(rla = this.lock.acquireReadLock()));

			if(this.state != State.DISCONNECTED)
				throw new IllegalStateException();

			// Acquire write lock...
			while(!(wla = this.lock.acquireWriteLock()));

			// Create & configure socket...
			this.socket = new Socket();
			if(this.bind_addr != null) this.socket.bind(this.bind_addr);
			if(this.sock_timeout >= 0) this.socket.setSoTimeout(this.sock_timeout);

			// Connect!
			this.socket.connect(address);

			// Get writer stream...
			this.reader = this.socket.getInputStream();
			this.writer = this.socket.getOutputStream();

			// Reset buffer...
			this.buf_index = 0;
			this.buf_size = 0;

			// Update state
			this.state = State.CONNECTED;

			// Return!
			return true;
		} catch(ConnectException e) {
			// Unable to connect. Null everything and prepare for another connection attempt...
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();

			try {
				if(this.socket != null)
					this.socket.close();
			} catch(IOException e2) {
				if(JCNLib.SHOW_ERRORS) e2.printStackTrace(); // Sigh...
			}

			this.socket = null;
			this.reader = null;
			this.writer = null;
		} catch(IOException e) {
			// Unexpected exception occurred and left us in an undefined state. Close for safety.
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();

			this.close();
		} finally {
			// Release write lock...
			if(wla) this.lock.releaseWriteLock();

			// Update listeners and waiting threads...
			if(this.state == State.CONNECTED) {
				for(StateListener listener : this.listeners) listener.handleStateChange(this, state);
				this.lock.postSignal();
			}

			// Release read lock...
			if(rla) this.lock.releaseReadLock();
		}

		// Return!
		return false;
	}

	@Override
	public boolean disconnect() {
		boolean rla = false;
		boolean wla = false;

		try {
			// Acquire read lock
			while(!(rla = this.lock.acquireReadLock()));

			if(this.state != State.CONNECTED)
				return false;

			// Update state
			this.state = State.DISCONNECTED;

			// Close socket...
			this.socket.close();

			// Acquire write lock...
			while(!(wla = this.lock.acquireWriteLock()));

			// Null old variables and complete the process.
			this.socket = null;
			this.reader = null;
			this.writer = null;

			// Return!
			return true;
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace(); // Yikes.
			this.close();
		} finally {
			// Release write lock...
			if(wla) this.lock.releaseWriteLock();

			// Update listeners and waiting threads...
			if(this.state == State.DISCONNECTED) {
				for(StateListener listener : this.listeners) listener.handleStateChange(this, state);
				this.lock.postSignal();
			}

			// Release read lock...
			if(rla) this.lock.releaseReadLock();
		}

		// :(
		return false;
	}

	@Override
	public boolean close() {
		boolean rla = false;
		boolean wla = false;

		try {
			// Acquire read lock
			while(!(rla = this.lock.acquireReadLock()));

			// Check if we're already closed...
			if(this.state == State.CLOSED)
				return false;

			// Disconnect!
			this.disconnect();

			// Acquire write lock
			while(!(wla = this.lock.acquireWriteLock()));

			// Update state
			this.state = State.CLOSED;

			// Return!
			return true;
		} finally {
			// Release write lock...
			if(wla) this.lock.releaseWriteLock();

			// Update listeners and waiting threads...
			if(this.state == State.CLOSED) {
				for(StateListener listener : this.listeners) listener.handleStateChange(this, state);
				this.lock.postSignal();
			}

			// Release read lock...
			if(rla) this.lock.releaseReadLock();
		}
	}

	@Override
	public void join() throws InterruptedException {
		boolean rla = false;

		try {
			// Acquire read lock
			while(!(rla = this.lock.acquireReadLock()));

			// Verify state...
			if(this.state == State.CLOSED)
				throw new IllegalStateException();

			// Wait for state change (eternally)...
			for(State init_state = this.state; init_state == this.state; this.lock.waitForSignal(0));
		} finally {
			if(rla) this.lock.releaseReadLock();
		}
	}

	@Override
	public boolean join(long timeout) throws InterruptedException {
		boolean rla = false;

		try {
			// Acquire read lock
			while(!(rla = this.lock.acquireReadLock()));

			// Verify state...
			if(this.state == State.CLOSED)
				throw new IllegalStateException();

			State init_state = this.state;
			long calltime = System.currentTimeMillis();

			// Wait for connection state change!
			while((timeout = timeout - (System.currentTimeMillis() - calltime)) > 0) {
				this.lock.waitForSignal(timeout);

				if(this.state != init_state)
					return true;
			}

			// State did not change during specified timeout.
			return false;
		} finally {
			if(rla) this.lock.releaseReadLock();
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public State getState() {
		return this.state;
	}

	@Override
	public boolean execStateDependentProcess(State state, Runnable process) {
		if(state == null)
			throw new IllegalArgumentException("state");

		if(process == null)
			throw new IllegalArgumentException("process");

		boolean rla = false;

		try {
			// Acquire read lock...
			while(!(rla = this.lock.acquireReadLock()));

			// Run process if state matches...
			if(this.state == state) {
				process.run();

				return true;
			}
		} finally {
			// Release lock...
			if(rla) this.lock.releaseReadLock();
		}

		// State didn't match. :(
		return false;
	}

	@Override
	public boolean registerStateListener(StateListener listener) {
		if(listener == null)
			throw new IllegalArgumentException("listener");

		return !this.listeners.contains(listener) && this.listeners.add(listener);
	}

	@Override
	public boolean removeStateListener(StateListener listener) {
		if(listener == null)
			throw new IllegalArgumentException("listener");

		return this.listeners.remove(listener);
	}

////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean sendMessage(String message) {
		if(message == null || message.length() == 0)
			return false;

		boolean rla = false;

		try {
			// Acquire read lock
			while(!(rla = this.lock.acquireReadLock()));

			if(this.state != State.CONNECTED)
				return false;

			try {
				this.writer.write(message.getBytes());
				this.writer.write(10);

				return true;
			} catch(IOException e) {
				if(JCNLib.SHOW_ERRORS) e.printStackTrace();
				//this.close();
			}

			// Something bad happened. We should reconnect.
			this.disconnect();
			return false;
		} finally {
			if(rla) this.lock.releaseReadLock();
		}
	}

	@Override
	public String receiveMessage() {
		boolean rla = false;

		try {
			while(!(rla = this.lock.acquireReadLock()));

			if(this.state != State.CONNECTED)
				return null;

			try {
				int bytes = 0; // Bytes read

				do {
					// Recalculate buffer size & Check buffer for data...
					for(this.buf_size += bytes; this.buf_index < this.buf_size; ++this.buf_index) {
						if(this.buffer[this.buf_index] == '\n' || this.buffer[this.buf_index] == '\r') {
							String msg = new String(this.buffer, 0, this.buf_index++);
							System.arraycopy(this.buffer, this.buf_index, this.buffer, 0, this.buf_size - this.buf_index);

							this.buf_size -= this.buf_index;
							this.buf_index = 0;

							return msg;
						}
					}

					// Attempt to read more bytes from the socket...
				} while((bytes = this.reader.read(this.buffer, this.buf_size, INPUT_BUFFER_SIZE - this.buf_size)) != -1);
			} catch(SocketTimeoutException e) {
				// Too slow...

			} catch(IOException e) {
				if(JCNLib.SHOW_ERRORS) e.printStackTrace();
				//this.close();
			}

			// Something bad happened...
			this.disconnect();
			return null;
		} finally {
			if(rla) this.lock.releaseReadLock();
		}
	}
}