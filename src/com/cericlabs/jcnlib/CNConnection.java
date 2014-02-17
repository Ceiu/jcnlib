package com.cericlabs.jcnlib;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;



/**
 * The CNConnection object is the core of the jcnlib library. All connections are maintained
 * by instances of this class.
 *
 * @author Chris "Ceiu" Rog
 */
public abstract class CNConnection {

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Enum for identifying the status of a CNConnection object. */
	public static enum State {
		CONNECTED,
		DISCONNECTED,
		CLOSED;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Extends the state listener framework to support connection decorators.
	 */
	private final class StateListenerEx implements StateListener {

		private final List<StateListener> listeners;

		//////////////////////////////////////////////////

		public StateListenerEx() {
			this.listeners = new CopyOnWriteArrayList<StateListener>();
		}

		//////////////////////////////////////////////////

		public void handleStateChange(CNConnection connection, CNConnection.State state) {
			if(connection != CNConnection.this.connection)
				return;

			for(StateListener listener : this.listeners)
				listener.handleStateChange(CNConnection.this, state);
		}

		//////////////////////////////////////////////////

		public boolean registerListener(StateListener listener) {
			if(listener == null)
				throw new IllegalArgumentException("listener");

			return !this.listeners.contains(listener) && this.listeners.add(listener);
		}

		public boolean removeListener(StateListener listener) {
			if(listener == null)
				throw new IllegalArgumentException("listener");

			return this.listeners.remove(listener);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final CNConnection connection;
	private final StateListenerEx sl_ext;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a root CNConnection that implements all of the basic functionality.
	 */
	public CNConnection() {
		this.connection = null;		// Root connection has no use for these.
		this.sl_ext = null;			// ^
	}

	/**
	 * Creates a new CNConnection that augments the specified connection.
	 *
	 * @param connection
	 *	The original connection to augment. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified connection is null.
	 */
	public CNConnection(CNConnection connection) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		this.connection = connection;

		this.sl_ext = new StateListenerEx();
		this.connection.registerStateListener(this.sl_ext);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Attempts to connect to a chatnet server at the host/port specified.
	 *
	 * @param host
	 *	The host to connect to. This can be either a hostname or an ip address.
	 *
	 * @param port
	 *	The port to connect on.
	 *
	 * @throws IllegalStateException
	 *	if this connection is already connected or has been closed.
	 *
	 * @return true if the connection is successful; false otherwise.
	 */
	public boolean connect(String host, int port) {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.connect(host, port);
	}

	/**
	 * Attempts to connect to a chatnet server at the address specified.
	 *
	 * @param address
	 *	An InetSocketAddress representing the host/port to connect to.
	 *
	 * @throws IllegalArgumentException
	 *	if objAddress is null.
	 *
	 * @throws IllegalStateException
	 *	if this connection is already connected or has been closed.
	 *
	 * @return true if the connection is successful; false otherwise.
	 */
	public boolean connect(InetSocketAddress address) {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.connect(address);
	}

	/**
	 * Disconnects this connection. Calls to this method while the connection is offline are
	 * silently ignored.
	 *
	 * @throws IllegalStateException
	 */
	public boolean disconnect() {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.disconnect();
	}

	/**
	 * Closes this connection. Once a connection is closed, it can no longer be used to connect and
	 * a new connection must be made. Calls made to this method after the connection has been
	 * closed are silently ignored.
	 *
	 * @return
	 *	True if the connection was closed as a result of a call to this method; false otherwise.
	 */
	public boolean close() {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.close();
	}

	/**
	 * Joins on this connection. The calling thread will resume once the connection has
	 * changed state.
	 *
	 * @throws IllegalStateException
	 *	If this method is called after the connection has been closed.
	 */
	public void join() throws InterruptedException {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		this.connection.join();
	}

	/**
	 * Joins on this connection. The calling thread will resume once the connection status has
	 * changed or the specified timeout has elapsed. If the timeout value is zero or negative, this
	 * method will return false without any delay.
	 *
	 * @param timeout
	 *	The timeout to wait in milliseconds.
	 *
	 * @throws IllegalStateException
	 *	If this method is called after the connection has been closed.
	 *
	 * @return
	 *	True if the connection status changed before the specified timeout elapsed; false
	 *	otherwise.
	 */
	public boolean join(long timeout) throws InterruptedException {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.join(timeout);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the current state of this connection.
	 *
	 * @return
	 *	This connection's current state.
	 */
	public State getState() {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.getState();
	}

	/**
	 * Performs an operation which temporarily locks the state of this connection.
	 * <p/>
	 * This method can be used to execute code that is dependent on the state of this connection
	 * remaining constant. For instance, to run code that must execute while this connection is
	 * connected, this method would be called as follows:
	 * <tt>
	 * connection.executeSDProcess(CNConnection.State.CONNECTED, new Runnable(){
	 *   public void run() {
	 *     // code here
	 *   }
	 * });
	 * </tt>
	 *
	 * During a call to this method, calls made by other threads that would result in a change of
	 * state are blocked until this method returns.
	 *
	 * @param state
	 *	The state the connection must be in to execute the given process.
	 *
	 * @param process
	 *	The process to execute if the state is as expected.
	 *
	 * @return
	 *	True if the state matched and the process was executed; false otherwise.
	 */
	public boolean execStateDependentProcess(State state, Runnable process) {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.execStateDependentProcess(state, process);
	}

	/**
	 * Registers the specified StateListener with this connection. The listener will be notified
	 * whenever the state of this connection changes.
	 *
	 * @param listener
	 *	The StateListener to register with this connection.
	 *
	 * @return
	 *	True if the listener was registered successfully; false otherwise.
	 */
	public boolean registerStateListener(StateListener listener) {
		if(this.sl_ext == null)
			throw new UnsupportedOperationException();

		return this.sl_ext.registerListener(listener);
	}

	/**
	 * Removes the specified StateListener from this connection. The listener will no longer be
	 * notified when this connection's state changes.
	 *
	 * @param listener
	 *	The StateListener to remove from this connection.
	 *
	 * @return
	 *	True if the listener was reigstered successfully; false otherwise.
	 */
	public boolean removeStateListener(StateListener listener) {
		if(this.sl_ext == null)
			throw new UnsupportedOperationException();

		return this.sl_ext.removeListener(listener);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sends a message. This method automatically adds a line break to the end of all messages. If
	 * this connection object is not connected, this method immediately returns false. If an
	 * exception occurs while writing the message, this connection will be closed.
	 *
	 * @param message
	 *	The chatnet message to send.
	 *
	 * @return
	 *	true if the message was sent successfully; false otherwise.
	 */
	public boolean sendMessage(String message) {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.sendMessage(message);
	}

	/**
	 * Attempts to receive a message from the server. This operation will block until at least one
	 * message is received from the server, the connection is closed, or an error occurs.
	 *
	 * @return
	 *	A message received from the server, or null if the read operation timed out or an error
	 *	occured.
	 */
	public String receiveMessage() {
		if(this.connection == null)
			throw new UnsupportedOperationException();

		return this.connection.receiveMessage();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sends a login message to the server.
	 *
	 * @param username
	 *	The username to use.
	 *
	 * @param password
	 *	The password to use.
	 *
	 * @throws IllegalArgumentException
	 *	If the username or password are null.
	 */
	public void login(String username, String password) {
		if(username == null || password == null)
			throw new IllegalArgumentException();

		this.sendMessage(String.format("LOGIN:1;jcnlib v%d.%d.%d:%s:%s", JCNLib.Version.MAJOR, JCNLib.Version.MINOR, JCNLib.Version.RELEASE, username, password));
	}

	/**
	 * Sends an arena change request to the server, with no arena specified. This will change to a
	 * server-assigned public arena.
	 */
	public void changeArena() {
		this.sendMessage("GO:");
	}

	/**
	 * Sends an arena change request to the server, using the specified public arena number. This
	 * will change to the public arena number specified, unless the server denies the request.
	 *
	 * @param arena
	 *	The public arena number to change to.
	 */
	public void changeArena(int arena) {
		if(arena < 0 || arena > 255)
			throw new IllegalArgumentException("arena");

		this.sendMessage("GO:" + arena);
	}

	/**
	 * Sends an arena change request to the server, using the specified arena name. This will
	 * change to the named arena (such as "duel" or "#private"), unless the server denies the
	 * request.
	 *
	 * @param arena
	 *	The arena name to change to. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified arena is null.
	 */
	public void changeArena(String arena) {
		if(arena == null)
			throw new IllegalArgumentException("arena");

		this.sendMessage("GO:" + arena);
	}

	/**
	 * Sends a frequency change request to the server.
	 *
	 * @param frequency
	 *	The frequency to change to. Must be between 0 and 9999, inclusively.
	 *
	 * @throws IllegalArgumentException
	 *	if frequency is less than 0 or greater than 9999.
	 */
	public void changeFrequency(int frequency) {
		if(frequency < 0 || frequency > 9999)
			throw new IllegalArgumentException("frequency");

		this.sendMessage("CHANGEFREQ:" + frequency);
	}

	/**
	 * Sends a formatted public message.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendPublicMessage(String message, Object... argv) {
		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:PUB:" + message);
	}

	/**
	 * Sends a formatted message as a public macro. Public macros are identical to public messages,
	 * except that players who have disabled public macros will not see it.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendPublicMacro(String message, Object... argv) {
		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:PUBM:" + message);
	}

	/**
	 * Sends a formatted message as a public command. The message will not be sent to other
	 * players, even if it is not a valid command.
	 *
	 * @param command
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendPublicCommand(String command, Object... argv) {
		if(command == null)
			throw new IllegalArgumentException("command");

		if(argv.length > 0)
			command = String.format(command, argv);

		this.sendMessage("SEND:CMD:" + command);
	}

	/**
	 * Sends a formatted private message to the specified player.
	 * <br/>Equivalent to <tt>:player:message</tt> in Continuum.
	 *
	 * @param player
	 *	The player to send the message to; cannot be null.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified player or message is null.
	 */
	public void sendPrivateMessage(String player, String message, Object... argv) {
		if(player == null)
			throw new IllegalArgumentException("player");

		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:PRIV:" + player + ":" + message);
	}

	/**
	 * Sends a formatted private message to the specified player as a command. The message will not
	 * be seen by the target player, even if it is not a valid command.
	 *
	 * @param player
	 *	The player to send the message to; cannot be null.
	 *
	 * @param command
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified player or message is null.
	 */
	public void sendPrivateCommand(String player, String command, Object... argv) {
		if(player == null)
			throw new IllegalArgumentException("player");

		if(command == null)
			throw new IllegalArgumentException("command");

		if(argv.length > 0)
			command = String.format(command, argv);

		this.sendMessage("SEND:PRIVCMD:" + player + ":" + command);
	}

	/**
	 * Sends a formatted message to an entire frequency. If the frequency is an enemy frequency,
	 * the message will be displayed in Green> Blue. Otherwise the entire message will be yellow.
	 * <br/>Equivalent to <tt>"message</tt> in Continuum.
	 *
	 * @param frequency
	 *	The frequency to send the message to.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendFrequencyMessage(int frequency, String message, Object... argv) {
		if(frequency < 0 || frequency > 9999)
			throw new IllegalArgumentException("frequency");

		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:FREQ:" + frequency + ":" + message);
	}

	/**
	 * Sends a formatted message to the specified chat channel.
	 * <br/>Equivalent to <tt>;1;message</tt> in Continuum.
	 *
	 * @param channel
	 *	The channel to send the message to. Subbill currently limits this to 10.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendChatMessage(int channel, String message, Object... argv) {
		if(channel < 0)
			throw new IllegalArgumentException("channel");

		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:CHAT:" + channel + ";" + message);
	}

	/**
	 * Sends a formatted message to the moderator chat.
	 * <br/>Equivalent to <tt>\message</tt> in Continuum.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	public void sendStaffMessage(String message, Object... argv) {
		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:MOD:" + message);
	}

	/**
	 * Sends a formatted message to the specified squad.
	 * <br/>Equivalent to <tt>:#squad:message</tt> in Continuum.
	 *
	 * @param squad
	 *	The squad to send the message to; cannot be null.
	 *
	 * @param message
	 *	The message to send; cannot be null.
	 *
	 * @param argv
	 *	Optional. A list of arguments used to format the message.
	 *
	 * @throws IllegalArgumentException
	 *	If either the squad or message is null.
	 */
	public void sendSquadMessage(String squad, String message, Object... argv) {
		if(squad == null)
			throw new IllegalArgumentException("squad");

		if(message == null)
			throw new IllegalArgumentException("message");

		if(argv.length > 0)
			message = String.format(message, argv);

		this.sendMessage("SEND:SQUAD:" + squad + ":" + message);
	}

	/**
	 * Sends a no-op message to the server. The server will send one of these if the client
	 * has not sent or received anything for three minutes. It would probably be a good idea if
	 * the client did the same.
	 */
	public void sendNoOp() {
		this.sendMessage("NOOP");
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}