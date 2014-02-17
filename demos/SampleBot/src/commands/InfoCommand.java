package samplebot.commands;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.commands.*;
import com.cericlabs.jcnlib.util.arena.*;



/**
 * The InfoCommand class defines behaviors for handling the "!info" command. This command is NOT
 * required in Hyperspace, but demonstrates use of the ArenaManager's PlayerData.
 *
 * This command responds to the player with some basic information about the player they
 * specify. In the event a player is not specified, it will give information about this bot.
 */
public class InfoCommand extends SimpleCommand {

	private final ArenaManager arena_manager;

////////////////////////////////////////////////////////////////////////////////////////////////////

	public InfoCommand(CNConnection connection, ArenaManager arena_manager) {
		super(connection);

		if(arena_manager == null)
			throw new IllegalArgumentException("arena_manager");

		this.arena_manager = arena_manager;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void handleCommand(ChatType chat_type, String player, String command, String arguments) {
		if(chat_type != ChatType.PRIVATE)
			return; // We only care about private messages...

		// This command shows a very simple way of using the PlayerData class. We retrieve the
		// player data by calling the ArenaManager's getPlayer method with the name of the
		// player (case-insensitive). Alternatively, the findPlayer method may be used with a
		// regular expression. This allows partial names and such, but requires that the regular
		// expression is valid. Certain names (Like "8^)->-<") may contain regular expresion
		// control characters which need to be properly escaped before they can be used.

		// Once the player data is obtained, you can use it like a standard HashMap object.
		//
		// NOTE: With the default configuration, the ArenaManager does not retain player data
		// for players if either they or the bot leaves the arena. This behavior can be changed
		// by using the retainPlayerData method to set the retention policy.
		//
		// NOTE: Though we're not using them here, the ArenaManager and PlayerData class do
		// support generics.

		PlayerData pdata;

		if(arguments != null) {
			pdata = this.arena_manager.findPlayer("%s.*", arguments);
		} else {
			pdata = this.arena_manager.getPlayer();
		}

		if(pdata != null) {
			Object chat = pdata.get("chat");

			super.connection.sendPrivateMessage(player, "Player %s is currently on freq %d in a %s.", pdata.getPlayerName(), pdata.getFrequency(), pdata.getShip());
			if(chat != null) super.connection.sendPrivateMessage(player, "Their last public message was: %s", chat);
		} else {
			super.connection.sendPrivateMessage(player, "Couldn't find a player matching the name \"%s\".", arguments);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}