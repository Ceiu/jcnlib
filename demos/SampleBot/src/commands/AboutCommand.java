package samplebot.commands;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.commands.*;



/**
 * The AboutCommand class defines behaviors for handling the "!about" command. This command is
 * required in Hyperspace.
 *
 * This command only responds to private messages and responds to the player with a simple
 * private message explaining the bot's purpose.
 */
public class AboutCommand extends SimpleCommand {

////////////////////////////////////////////////////////////////////////////////////////////////////

	public AboutCommand(CNConnection connection) {
		super(connection);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void handleCommand(ChatType chat_type, String player, String command, String arguments) {
		if(chat_type != ChatType.PRIVATE)
			return; // We only care about private messages...

		// Respond to the player...
		super.connection.sendPrivateMessage(player, "I am a jcnlib sample bot. You can download me at: http://www.cericlabs.com/projects/jcnlib");
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}