package samplebot.commands;

import samplebot.*;

import java.util.*;
import java.util.regex.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;
import com.cericlabs.jcnlib.util.*;
import com.cericlabs.jcnlib.util.arena.*;



/**
 * The ShutdownCommand class defines behaviors for handling the "!shutdown" command. This
 * command is required in Hyperspace.
 *
 * The command only responds to private messages, and checks that the player issuing the command
 * is either the owner or a moderator in Hyperspace.
 *
 * This command is rather complicated compared to other commands, primarily because it relies on
 * obtaining information from the server rather than information it already has.
 */
public class ShutdownCommand extends SimpleCommand implements InboundChatMessage.Handler {

	private static final Pattern LISTMOD_REGEX = Pattern.compile("\\A: (.{20}) (.{10}) (.{10,})\\s*\\z");

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final EventDispatcher event_dispatcher;
	private final ArenaManager arena_manager;
	private final Timer timer;

	private String player;
	private String arena;

	private boolean ready;
	private boolean in_check;
	private boolean parsing_lm;

////////////////////////////////////////////////////////////////////////////////////////////////////

	public ShutdownCommand(CNConnection connection, EventDispatcher event_dispatcher, ArenaManager arena_manager, Timer timer) {
		super(connection);

		if(event_dispatcher == null)
			throw new IllegalArgumentException("event_dispatcher");

		if(arena_manager == null)
			throw new IllegalArgumentException("arena_manager");

		if(timer == null)
			throw new IllegalArgumentException("timer");

		this.event_dispatcher = event_dispatcher;
		this.arena_manager = arena_manager;
		this.timer = timer;

		this.reset();
		this.ready = true;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private synchronized void reset() {
		this.event_dispatcher.removeHandler(InboundChatMessage.class, this);

		this.player = null;
		this.arena = null;

		this.in_check = false;
		this.parsing_lm = false;
	}

	private synchronized void completeShutdown(String player) {
		if(player == null)
			throw new IllegalArgumentException();

		// Perform the shutdown operation.
		System.out.printf("Shutdown authorized by: %s\n", player);
		super.connection.sendPrivateMessage(player, "Access verified. Shutting down...");

		// Wait a moment for the message to send...
		try { Thread.sleep(2500); } catch(InterruptedException e) { }
		super.connection.close();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Command handler. Initiates the shutdown operation by verifying the user is the owner, or
	 * by issuing a ?listmod command to check if the user is a moderator.
	 */
	@Override
	public synchronized void handleCommand(ChatType chat_type, String player, String command, String arguments) {
		if(player == null || chat_type != ChatType.PRIVATE)
			return; // Invalid parameters. Probably not called by the CommandManager.

		// Check if it's our owner...
		if(player.equalsIgnoreCase(BotActions.BOT_OWNER)) {
			this.completeShutdown(BotActions.BOT_OWNER);
			this.reset();

			return;
		}

		// Check if we're ready to perform a permission check...
		if(this.ready) {
			// We are. Store necessary data and initialize check...
			this.ready = false;
			this.in_check = true;
			this.parsing_lm = false;

			this.player = player;
			this.arena = this.arena_manager.getArenaName();

			// Register handler and issue "?listmod" command...
			this.event_dispatcher.registerHandler(InboundChatMessage.class, this);
			super.connection.sendPublicCommand("?listmod");

			// Create and schedule timer...
			this.timer.schedule(new TimerTask() { public void run() { handleTimer(); } }, 5000);
		} else {
			// We're not. Tell the player to try again later.
			super.connection.sendPrivateMessage(player, "A shutdown check is already in progress. Try again later.");
		}
	}

	/**
	 * Event handler. Handles inbound messages and searches for responses to the "?listmod"
	 * command to check if the player who issued the "!shutdown" command is a moderator.
	 */
	@Override
	public synchronized void handleEvent(InboundChatMessage event) {
		if(event == null)
			return; // Invalid event.

		if(!this.in_check)
			return; // No longer checking permissions.

		// Check the message type. ?listmod responses are always arena messages.
		if(event.getChatType() == ChatType.ARENA) {
			Matcher lm_matcher = LISTMOD_REGEX.matcher(event.getMessage());

			// Check the message is formatted like a ?listmod response.
			if(lm_matcher.matches()) {
				this.parsing_lm = true;

				// Check that the player and arena match...
				if(this.player.equalsIgnoreCase(lm_matcher.group(1).trim())) {
					if(this.arena.equalsIgnoreCase(lm_matcher.group(2).trim())) {
						// Matched. Shutdown authorized...
						this.completeShutdown(this.player);
					} else {
						// Correct player, but they're in another arena.
						// We perform this check since a player could be a moderator in one arena, but not another.
						System.out.printf("Shutdown request denied for: %s. Player is not in the same arena.\n", this.player);
						super.connection.sendPrivateMessage(this.player, "Shutdown request denied: You must be in the same arena to issue a shutdown command.");
					}

					// Player matched. Since we decided something, reset.
					this.reset();
				}

				return;
			}
		}

		// If we're parsing listmod responses and we no longer match the message type or format,
		// we know that we're done receiving the moderator list.
		if(this.parsing_lm) {
			System.out.printf("Shutdown request denied for: %s\n", this.player);
			super.connection.sendPrivateMessage(this.player, "Shutdown request denied: Unable to verify permissions.");

			this.reset();
		}
	}

	/**
	 * Timer handler. This is called eventually after every shutdown check. If the moderator
	 * list did not contain the player and no other chat messages have been received, this
	 * method notifies the player that they don't have permissions.
	 */
	public synchronized void handleTimer() {
		if(this.in_check) {
			// We weren't able to verify the player's permissions in time...
			System.out.printf("Shutdown request denied for: %s\n", this.player);
			super.connection.sendPrivateMessage(this.player, "Shutdown request denied: Unable to verify permissions.");

			// Reset state...
			this.reset();
		}

		this.ready = true;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}