package samplebot.commands;

import samplebot.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.commands.*;
import com.cericlabs.jcnlib.util.arena.*;



/**
 * The OwnerCommand class defines behaviors for handling the "!owner" command. This command is
 * required in Hyperspace.
 *
 * This command only responds to private messages and responds to the player by listing the
 * player specified in the BOT_OWNER variable defined above (around line 20).
 */
public class OwnerCommand extends SimpleCommand {

////////////////////////////////////////////////////////////////////////////////////////////////////

	public OwnerCommand(CNConnection connection) {
		super(connection);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void handleCommand(ChatType chat_type, String player, String command, String arguments) {
		if(chat_type != ChatType.PRIVATE)
			return; // We only care about private messages...

		// Respond to the player...
		super.connection.sendPrivateMessage(player, "Owner: %s", BotActions.BOT_OWNER);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}