package samplebot;

import java.util.*;
import java.util.regex.*;

import samplebot.commands.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;
import com.cericlabs.jcnlib.util.*;
import com.cericlabs.jcnlib.util.commands.*;
import com.cericlabs.jcnlib.util.arena.*;



public class BotActions extends BotCore {

	/**
	 * The owner of this bot. You should change this to your Subspace username so you can shutdown
	 * the bot on your own (or do any other administrative stuff you may add later).
	 */
	public static final String BOT_OWNER = "Ceiu";

////////////////////////////////////////////////////////////////////////////////////////////////////

	public BotActions() {
		// First, set our host and user configuration.
		// In this case, we only set the host and port of the zone we want to connect to. If we want
		// (or need to), we can also set the default arena to join upon connection here by adding a
		// third parameter. For instance:
		//
		//     supet.setHostConfig(HOST, PORT, "#priv_arena");
		//
		super.setHostConfig("208.122.59.226", 5005); // Fun fact: 208.122.59.226:5005 is SSCE Hyperspace
		super.setUserConfig("UB-SampleBot", "BOT PASSWORD HERE");

		// Additionally, if we need to add any connection decorators to our connection, we should
		// do that here. A common decorator is the MessageThrottle. Other decorators can be added
		// here as need be (such as the MessagePreprocessor).
		//
		// WARNING: Never set the connection to null. Doing so will cause BotCore's run method to
		// throw a NullPointerException, as it expects it to be non-null at all times. Further,
		// creating a new connection rather than decorating the base connection will cause issues
		// with normal termination of the bot.
		//
		// tl;dr: Only decorate the connection. Never replace or null it.
		//
		super.connection = new MessageThrottle(super.connection);
		//super.connection = new MessagePreprocessor(super.connection);

		// Next, we can register our commands. Here we use the CommandManager created by the BotCore
		// superclass for easy registration.
		//
		// NOTE: Registering commands in this way is fine if you never intend to remove them.
		// However, in the case of bots that have plugins (or commands that aren't always active),
		// this isn't the best way to go. Instead you'll want to create references to the command
		// objects so they can be removed later. For example:
		//
		//     HelpCommand help = new HelpCommand();
		//     this.command_manager.registerCommand("help", help);
		//     ...
		//     this.command_manager.removeCommand("help", help);
		//
		// Commands required in HS...
		super.command_manager.registerCommand("help", new HelpCommand(super.connection, super.command_manager));
		super.command_manager.registerCommand("about", new AboutCommand(super.connection));
		super.command_manager.registerCommand("owner", new OwnerCommand(super.connection));
		super.command_manager.registerCommand("shutdown", new ShutdownCommand(super.connection, super.event_dispatcher, super.arena_manager, super.timer));

		// Other stuff...
		super.command_manager.registerCommand("info", new InfoCommand(super.connection, super.arena_manager));


		// Log writer!
		// The log writer is a utility class that will log standard events to the specified writer.
		// In this case, we're just sending it to the console. However, if you wanted to log
		// everything to a file, this could be achieved by creating a FileWriter instead:
		//
		//     log_writer = new LogWriter(new FileWriter(new File("C:\\log.txt")));
		//
		LogWriter log_writer = new LogWriter(new java.io.OutputStreamWriter(System.out), 10, 79);


		// We need to register the log writer for it to recieve events to log. We can do this in
		// one of two ways: Lazy registration (handle everything), or specific event registration
		// (for only handling specific events).
		//
		// For lazy registration, we can simply use the registerHandlerEx method of the
		// EventDispatcher, like so:
		//
		//     dispatcher.registerHandlerEx(log_writer);
		//
		// However, if we only wanted to register player deaths and inbound messages, then we could
		// use the registerHandler method with the specific classes, like so:
		//
		//     dispatcher.registerHandler(PlayerDeath.class, log_writer);
		//     dispatcher.registerHandler(InboundChatMessage.class, log_writer);
		//
		// Either method is equally valid. Which method you'll want to use will depend on what the
		// bot will be doing.
		//
		super.event_dispatcher.registerHandlerEx(log_writer);

		// We don't care about the player death events. We can remove it and still handle everything
		// else.
		//
		super.event_dispatcher.removeHandler(PlayerDeath.class, log_writer);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Event Handlers
//
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	@SuppressWarnings("unchecked")
	public void handleEvent(InboundChatMessage event) {
		// Allow our super class to handle the event...
		super.handleEvent(event);

		// Display text in console...

		// Handle event...
		switch(event.getChatType()) {
			case PUBLIC:
			case PUBLIC_MACRO:
				// Store the public message for later use. Take a look at the InfoCommand to see
				// how this data is used.
				PlayerData pdata = super.arena_manager.getPlayer(event.getPlayer());

				if(pdata == null)
					System.out.printf("PData is null for %s\n", event.getPlayer());
				else
					pdata.put("chat", event.getMessage());

				break;

			case PRIVATE:
				//super.connection.sendPrivateMessage(event.getPlayer(), "Helko friend! :D");
				break;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}
