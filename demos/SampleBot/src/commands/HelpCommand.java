package samplebot.commands;

import java.util.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.commands.*;



/**
 * The HelpCommand class defines the behaviors for responding to a player sending the bot
 * the message "!help".
 *
 * The command only responds to private messages, and will respond with a list of registered
 * commands.
 */
public class HelpCommand extends SimpleCommand {

	private final CommandManager command_manager;

////////////////////////////////////////////////////////////////////////////////////////////////////

	public HelpCommand(CNConnection connection, CommandManager command_manager) {
		super(connection);

		if(command_manager == null)
			throw new IllegalArgumentException("command_manager");

		this.command_manager = command_manager;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void handleCommand(ChatType chat_type, String player, String command, String arguments) {
		if(chat_type != ChatType.PRIVATE)
			return; // We only care about private messages...


		// Create a buffer for building a string (StringBuilder is a mutable String).
		StringBuilder buffer = new StringBuilder();

		// Obtain a list of registered commands.
		//
		// Note: The BotActions.this.<whatever> is how you reference the specific instance of
		// BotActions from within a private class. It looks a little goofy, but that's
		// how it's done in Java.
		List<String> commands = this.command_manager.getCommands();

		for(String cmd; commands.size() > 0; ) {
			// Get a command from the list...
			cmd = commands.remove(0);

			// Append it to our buffer...
			buffer.append('!').append(cmd);

			// Check if there are any other commands in the list.
			// If there are, we need to add a comma to separate the commands.
			if(commands.size() > 0)
				buffer.append(", ");
		}

		// Send message!
		super.connection.sendPrivateMessage(player, "Available commands: %s", buffer.toString());
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}