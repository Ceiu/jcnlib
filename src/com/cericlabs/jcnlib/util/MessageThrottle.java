package com.cericlabs.jcnlib.util;

import java.util.*;
import java.util.regex.*;

import com.cericlabs.jcnlib.*;


/**
 * The MessageThrottle is a connection decorator that provides provides chat message throttling.
 * Bots which do not have special privileges yet still send lots of text should consider
 * augmenting connections with this class.
 * <p/>
 * Note: When a connection is lost, any remaining queued messages are thrown out. Keep this in
 * mind when sending large amounts of messages.
 *
 * @author Chris "Ceiu" Rog
 */
public class MessageThrottle extends CNConnection implements StateListener {

	/** Default message burst limit. */
	public static final int DEF_MSG_BURST = 3;

	/** Default message limit. */
	public static final int DEF_MSG_LIMIT = 6;

	/** Default message delay. */
	public static final int DEF_MSG_DELAY = 1250;

	/** Message identification regex. */
	private static final Pattern MSG_REGEX = Pattern.compile("(?i:SEND:.+)");

////////////////////////////////////////////////////////////////////////////////////////////////////

	private ThrottleWorker worker;
	private boolean throttling;
	private boolean clustering;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new MessageThrottle using the default throttle settings.
	 *
	 * @param connection
	 *	The connection to augment.
	 */
	public MessageThrottle(CNConnection connection) {
		this(connection, DEF_MSG_BURST, DEF_MSG_LIMIT, DEF_MSG_DELAY);
	}

	/**
	 * Creates a new connection using the specified throttle settings, bound to the given local
	 * address. Specifying an address to bind to is only necessary on machines that have multiple
	 * network addresses and must specify which to use.
	 *
	 * @param connection
	 *	The connection to augment.
	 *
	 * @param msg_burst
	 *	The number of messages this connection will send in a burst (without delay).
	 *
	 * @param msg_limit
	 *	The maximum number of messages this connection can send before inserting delays.
	 *
	 * @param msg_delay
	 *	The amount of time (in milliseconds) to wait between each burst/message.
	 *
	 * @throws IllegalArgumentException
	 *	if any of the throttle settings are negative.
	 */
	public MessageThrottle(CNConnection connection, int msg_burst, int msg_limit, long msg_delay) {
		super(connection);

		this.init(msg_burst, msg_limit, msg_delay);
	}

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Initializes this object.
	 */
	private void init(int msg_burst, int msg_limit, long msg_delay) {
		if(msg_burst < 0 || msg_limit < 0 || msg_delay < 0)
			throw new IllegalArgumentException();

		// Initialize queue & variables...
		this.worker = new ThrottleWorker(msg_burst, msg_limit, msg_delay);
		this.throttling = true;
		this.clustering = false;

		this.registerStateListener(this);

		// Start throttle worker...
		this.worker.start();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public void handleStateChange(CNConnection connection, State state) {
		if(connection != this)
			return; // Update for a connection we don't care about.

		synchronized(this.worker) {
			this.worker.notifyAll();
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sends the specified message with a delay as specified by the current configuration of this
	 * throttle.
	 *
	 * @param message
	 *	The message to send.
	 *
	 * @return
	 *	True if the message was sent or was scheduled to be sent; false otherwise.
	 */
	public boolean sendMessage(String message) {
		if(message == null || message.length() == 0)
			return false;


		if(this.throttling || this.worker.msg_queue.size() > 0) {
			if(MessageThrottle.MSG_REGEX.matcher(message).matches()) {
				synchronized(this.worker) {
					this.worker.msg_queue.add(message);
					if(!this.clustering) this.worker.notifyAll();

					return true;
				}
			}
		}

		return super.sendMessage(message);
	}

	/**
	 * Begins a message cluster. While clustering, messages will be queued, but not sent. Once a
	 * message cluster is completed, they can be sent by using the sendCluster() method.
	 */
	public void beginCluster() {
		this.clustering = true;
	}

	/**
	 * Sends a message cluster. If there is not an active cluster, or no messages were added to the
	 * cluster, this method will immediately return.
	 */
	public void sendCluster() {
		synchronized(this.worker) {
			this.clustering = false;
			if(this.worker.msg_queue.size() > 0) this.worker.notifyAll();
		}
	}


	/**
	 * Enables the throttle.
	 */
	public void enableThrottle() {
		this.throttling = true;
	}

	/**
	 * Disables the throttle.
	 */
	public void disableThrottle() {
		this.throttling = false;
	}

	/**
	 * Enables or disables the throttling functionality.
	 *
	 * @param enabled
	 *	True if the throttle should be enabled, false otherwise.
	 */
	public void setThrottle(boolean enabled) {
		this.throttling = enabled;
	}

	/**
	 * Checks if the throttle is currently enabled.
	 *
	 * @return
	 *	True if the throttle is enabled; false otherwise.
	 */
	public boolean getThrottleState() {
		return this.throttling;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Processes queued/throttled messages.
	 */
	private final class ThrottleWorker extends Thread {

		private int msg_count;
		public List<String> msg_queue;

		private int msg_burst;
		private int msg_limit;
		private long msg_delay;

		//////////////////////////////////////////////////

		public ThrottleWorker(int msg_burst, int msg_limit, long msg_delay) {
			super("jcnlib::throttle_worker");

			if(msg_burst < 0 || msg_limit < 0 || msg_delay < 0)
				throw new IllegalArgumentException();

			this.msg_count = 0;
			this.msg_queue = new LinkedList<String>();

			this.msg_burst = msg_burst;
			this.msg_limit = msg_limit;
			this.msg_delay = msg_delay;
		}

		//////////////////////////////////////////////////

		public synchronized void run() {
			long delay = 0;
			long delay_start = System.currentTimeMillis();
			int count;

			try {
				while(MessageThrottle.this.getState() != CNConnection.State.CLOSED) {
					// Wait for a connection...
					this.wait();

					// Check for connection...
					while(MessageThrottle.this.getState() == CNConnection.State.CONNECTED) {
						// Wait for messages...
						this.wait();

						// Do decrements that occured while we were waiting...
						this.msg_count -= ((System.currentTimeMillis() - delay_start) >>> 10);
						if(this.msg_count < 0) this.msg_count = 0;

						// Send messages until the queue is empty!
						while((count = this.msg_queue.size()) > 0) {
							// Burst!
							for(int i = 0; (i < count) && (this.msg_burst == 0 || i < this.msg_burst) && (this.msg_limit == 0 || this.msg_count < this.msg_limit); ++i) {
								MessageThrottle.super.sendMessage(this.msg_queue.remove(0));

								++this.msg_count;
							}

							// Configured delay...
							delay_start = System.currentTimeMillis();
							while((delay = this.msg_delay - (System.currentTimeMillis() - delay_start)) > 0) this.wait(delay);
							--this.msg_count;
						}
					}

					// Disconnected. Clear remaining messages to not screw with subsequent sessions...
					this.msg_queue.clear();
				}
			} catch(Exception e) {
				if(JCNLib.SHOW_ERRORS) e.printStackTrace();
			}

			// We'll never be able to send a message again. Better close this connection...
			MessageThrottle.this.close();
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}
