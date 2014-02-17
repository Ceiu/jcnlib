package samplebot;

import java.util.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;
import com.cericlabs.jcnlib.util.commands.*;
import com.cericlabs.jcnlib.util.arena.*;
import com.cericlabs.jcnlib.util.EventDispatcher;
import com.cericlabs.jcnlib.util.preprocessor.CNConnectionPreprocessor;



/**
 * The BotCore class is a basic framework class for quickly creating simple chatnet
 * programs. This class implements the standard functionality that many chatnet-enabled programs
 * create or eventually implement (joining an arena, creating common objects, etc.).
 * <p/>
 * This class can be extended to add additional event handlers, commands and any other basic
 * functionality.
 *
 * @author Chris "Ceiu" Rog
 */
public abstract class BotCore
	implements
		Runnable,

		StateListener,

		EnteredArena.Handler,
		InboundChatMessage.Handler,
		LoginResponse.Handler,
		PlayerDeath.Handler,
		PlayerEntered.Handler,
		PlayerLeft.Handler,
		PlayerUpdate.Handler {

////////////////////////////////////////////////////////////////////////////////////////////////////

	// CNConnection & Event dispatcher...
	private final CNConnection base_connection;		// Base connection. Used to send un/pw.

	protected volatile CNConnection connection;
	protected final EventDispatcher event_dispatcher;

	// Managers...
	protected final ArenaManager<Object, Object> arena_manager;
	protected final CommandManager command_manager;

	// Utilities...
	protected final Timer timer;

	// Internal state stuff...
	private boolean host_config;	// True when the host config has been set.
	private boolean user_config;	// Trhe when the user credentials have been set.

	private String host;			// The host we're going to attempt to connect to.
	private int port;				// The port we're going to attempp to connect on.
	private String arena;			// Default arena to join upon login.

	private String username;		// Username to connect with.
	private String password;		// Password to connect with.

////////////////////////////////////////////////////////////////////////////////////////////////////

	public BotCore() {
		// Create connection and dispatcher...
		this.base_connection = new SimpleCNConnection();
		CNConnectionPreprocessor preprocessor = new CNConnectionPreprocessor(this.base_connection);

		this.connection = preprocessor;
		this.event_dispatcher = new EventDispatcher();

		// Create managers...
		this.arena_manager = new ArenaManager<Object, Object>();
		this.command_manager = new CommandManager();

		// Create utilities...
		this.timer = new Timer("bc::timer", true);

		// Register stuff!
		preprocessor.registerPreprocessor(this.arena_manager);
		preprocessor.registerPreprocessor(this.event_dispatcher);

		this.event_dispatcher.registerHandlerEx(this.command_manager);
		this.event_dispatcher.registerHandlerEx(this);

		// Set default state values...
		this.host_config = false;
		this.user_config = false;

		this.host = null;
		this.port = 0;
		this.arena = null;

		this.username = null;
		this.password = null;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	// Configuration stuff...
	protected final void setHostConfig(String host, int port) {
		this.setHostConfig(host, port, null);
	}

	protected final void setHostConfig(String host, int port, String default_arena) {
		if(host == null)
			throw new IllegalArgumentException("host");

		if(port < 0 || port > 65535)
			throw new IllegalArgumentException("port");

		this.host = host;
		this.port = port;
		this.arena = default_arena;

		this.host_config = true;
	}

	protected final void setUserConfig(String username, String password) {
		if(username == null)
			throw new IllegalArgumentException("username");

		if(password == null)
			throw new IllegalArgumentException("password");

		this.username = username;
		this.password = password;

		this.user_config = true;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void run() {
		// Verify that we've been configured...
		if(!this.host_config) {
			System.out.println("Host information not configured. Aborting.");
			return;
		}

		if(!this.user_config) {
			System.out.println("User information not configured. Aborting.");
			return;
		}

		String message;

		// Keep connecting until we're closed...
		while(this.connection.getState() != CNConnection.State.CLOSED) {

			// Connect!
			System.out.printf("Connecting to %s:%d... ", this.host, this.port);
			if(this.connection.connect(this.host, this.port)) {

				// Login...
				System.out.printf("Connected!\nLogging in as \"%s\"... ", this.username);
				this.base_connection.login(this.username, this.password);

				// Process messages...
				while((message = this.connection.receiveMessage()) != null) {
					// We can do any core-level processing on the message here.
					//
					// NOTE: At this point the ArenaManager and EventDispatcher have both already seen
					// the message. If your processing needs to be done before them, it needs to be
					// added as a preprocessor and registered before either of the afore mentioned
					// objects.
				}

				// Message was null (something bad likely happened). Disconnect and try again, perhaps...
				this.connection.disconnect();
				System.out.println("Disconnected.");
			} else {
				System.out.println("Failed.");
			}

			// Reconnect if our connection hasn't been closed...
			if(this.connection.getState() != CNConnection.State.CLOSED) {
				System.out.println("Reconnecting in 10 seconds...");

				try {
					Thread.sleep(10000);
				} catch(InterruptedException e) {
					// We were interrupted. Break out early and don't attempt to reconnect.
					e.printStackTrace();
					break;
				}

				System.out.println();
			} else {
				System.out.println("Connection closed.");
			}
		}

		System.out.println("System shutdown.");
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void handleStateChange(CNConnection connection, CNConnection.State state) {
		// Do nothing.
	}

	/**
	 * Handles the EnteredArena event and stores the name of the arena to use in the event the bot
	 * has to reconnect.
	 */
	@Override
	public void handleEvent(EnteredArena event) {
		if(event == null)
			return; // Invalid event.

		// Store the name of the last arena we entered...
		this.arena = event.getArenaName();

		System.out.printf("Entered arena: %s\n", this.arena);
	}

	@Override
	public void handleEvent(InboundChatMessage event) {
		// Do nothing.
	}

	/**
	 * Handles the LoginResponse event and either joins the default arena or closes the connection,
	 * depending on whether or not the login attempt was successful.
	 * <p/>
	 * Subclasses overriding this event handler must handle the event completely, or remember to
	 * call this method as part of the handler.
	 *
	 * @param event
	 *	The LoginResponse event to process
	 */
	@Override
	public void handleEvent(LoginResponse event) {
		if(event == null)
			return; // Invalid event.

		if(event.accepted()) {
			System.out.printf("Success! Logged in as: %s.\n", event.getUsername());

			if(this.arena != null) {
				System.out.printf("Joining arena \"%s\"...\n", this.arena);
				event.connection.changeArena(this.arena);
			} else {
				System.out.printf("Joining the default arena...\n", this.arena);
				event.connection.changeArena();
			}
		} else {
			System.out.printf("Failed: %s\n", event.getResponse());
			event.connection.close();
		}
	}

	@Override
	public void handleEvent(PlayerDeath event) {
		// Do nothing.
	}

	@Override
	public void handleEvent(PlayerEntered event) {
		// Do nothing.
	}

	@Override
	public void handleEvent(PlayerLeft event) {
		// Do nothing.
	}

	@Override
	public void handleEvent(PlayerUpdate event) {
		// Do nothing.
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}