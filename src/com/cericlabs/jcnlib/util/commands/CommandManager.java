package com.cericlabs.jcnlib.util.commands;

import java.util.*;
import java.util.regex.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.InboundCNEvent;



/**
 * The CommandManager class is a utility class which provides basic command parsing to programs
 * using jcnlib.
 *
 * @author Chris "Ceiu" Rog
 */
public class CommandManager implements InboundCNEvent.Handler {

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** The default regular expression used to parse commands and arguments from chat messages. */
	public static final Pattern DEF_CMD_REGEX = Pattern.compile("^!(\\w+?)(?:\\s+(.+))?$");

	/** Pattern used to perform the split operation on raw messages. */
	private static final Pattern EXPLODE_REGEX = Pattern.compile(":");

////////////////////////////////////////////////////////////////////////////////////////////////////

	private Pattern cmd_regex;
	private final Map<String, List<CommandHandler>> commands;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new CommandManager instance using the default command regex (DEF_CMD_REGEX). The
	 * new CommandManager will need to be attached to a CNConnection instance before it will
	 * receive events.
	 */
	public CommandManager() {
		this(DEF_CMD_REGEX);
	}

	/**
	 * Creates a new CommandManager using the given command regex.
	 * <p/>
	 * When specifying a regular expression, the expression must have at least two capture groups
	 * (for the command and arguments, respectively), though any groups in excess of two are
	 * ignored.
	 *
	 * @param cmd_regex
	 *	The regular expression to use for parsing commands and arguments. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if cmd_regex is null.
	 */
	public CommandManager(Pattern cmd_regex) {
		if(cmd_regex == null)
			throw new IllegalArgumentException("cmd_regex");

		this.cmd_regex = cmd_regex;
		this.commands = new HashMap<String, List<CommandHandler>>();
	}

// Configuration
////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sets the regular expression to use to identify and parse commands. The pattern must have at
	 * least two capture groups for the parsing step to function (though, groups in excess of two
	 * are ignored).
	 *
	 * @param cmd_regex
	 *	The regular expression to use for identifying and parsing commands.
	 *
	 * @throws IllegalArgumentException
	 *	If regex is null.
	 */
	public void setCommandRegex(Pattern cmd_regex) {
		if(cmd_regex == null)
			throw new IllegalArgumentException();

		this.cmd_regex = cmd_regex;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Registers the specified CommandHandler as a handler for the specified command.
	 *
	 * @param command
	 *	The command to register.
	 *
	 * @param handler
	 *	The CommandHandler which will be executed when the specified command is received.
	 *
	 * @throws IllegalArgumentException
	 *	If either command or handler are null.
	 *
	 * @return
	 *	True if the command was registered; false otherwise.
	 */
	public synchronized boolean registerCommand(String command, CommandHandler handler) {
		if(command == null || handler == null)
			throw new IllegalArgumentException();

		List<CommandHandler> clist = this.commands.get((command = command.toLowerCase()));
		if(clist == null) this.commands.put(command, (clist = new LinkedList<CommandHandler>()));

		return (!clist.contains(handler) && clist.add(handler));
	}

	/**
	 * Removes the specified handler from the list of handlers for the specified command.
	 *
	 * @param command
	 *	The command to remove the handler from.
	 *
	 * @param handler
	 *	The CommandHandler to remove.
	 *
	 * @throws IllegalArgumentException
	 *	If either command or handler are null.
	 *
	 * @return
	 *	True if the command was found and removed successfully; false otherwise.
	 */
	public synchronized boolean removeCommand(String command, CommandHandler handler) {
		if(command == null || handler == null)
			throw new IllegalArgumentException();

		List<CommandHandler> clist = this.commands.get((command = command.toLowerCase()));

		if(clist != null && clist.remove(handler)) {
			if(clist.size() == 0) this.commands.remove(command);
			return true;
		}

		return false;
	}

	/**
	 * Returns a list of commands which currently have at least one handler registered for them.
	 * <p/>
	 * The list returned is not backed by this CommandManager instance. That is, changes made to
	 * the list are not reflected in the CommandManager and vice versa.
	 *
	 * @return A list of commands registered to this CommandManager.
	 */
	public synchronized List<String> getCommands() {
		return new LinkedList<String>(this.commands.keySet());
	}

	/**
	 * Returns a list of handlers registered for the specified command. If no such handlers are
	 * registered, this method returns null.
	 * <p/>
	 * The list returned is not backed by this CommandManager instance. That is, changes made to
	 * the list are not reflected in this CommandManager and vice versa.
	 *
	 * @param command
	 *	The command handlers must be registered to to be returned by this method.
	 *
	 * @throws IllegalArgumentException
	 *	If the given command is null.
	 *
	 * @return
	 *	A list containing the handlers registered to the specified command or null if no such
	 *	handlers exist.
	 */
	public synchronized List<CommandHandler> getHandlers(String command) {
		if(command == null)
			throw new IllegalArgumentException();

		List<CommandHandler> hlist = this.commands.get(command.toLowerCase());
		return (hlist != null ? new LinkedList<CommandHandler>(hlist) : null);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles inbound chatnet events. This method simply redirects the request to the
	 * processMessage method.
	 *
	 * @param event
	 *	The event to process.
	 */
	public void handleEvent(InboundCNEvent event) {
		if(event == null) return;

		this.processMessage(event.data);
	}

	/**
	 * Processes the specified message as a command and dispatches it to handlers.
	 * <p/>
	 * This method should only be called in situations where the command manager is not registered
	 * to an event dispatcher, or otherwise would not receive the message.
	 *
	 * @param message
	 *	The message to process.
	 */
	public void processMessage(String message) {
		if(message == null || message.length() == 0)
			return;

		// Split the message...
		String[] chunklets = EXPLODE_REGEX.split(message, 3);
		if(!chunklets[0].equalsIgnoreCase("MSG"))
			return; // Not a chat message...

		// Process message based on type...
		ChatType chat_type = ChatType.translate(chunklets[1]);

		switch(chat_type) {
			case PUBLIC:			// MSG:PUB:name:msg
			case PUBLIC_MACRO:		// MSG:PUBM:name:msg
			case PRIVATE:			// MSG:PRIV:name:msg
			case FREQUENCY:			// MSG:FREQ:name:msg
			case MODERATOR:			// MSG:MOD:name:msg
				chunklets = EXPLODE_REGEX.split(chunklets[2], 2);

				this.processCommand(chat_type, chunklets[0], chunklets[1]);
				break;

			case SQUAD:				// MSG:SQUAD:squad:sender:msg
				chunklets = EXPLODE_REGEX.split(chunklets[2], 3);

				this.processCommand(chat_type, chunklets[1], chunklets[2]);
				break;
		}

		// Return!
	}

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Processes the message as a command and dispatches it to handlers.
	 */
	private synchronized void processCommand(ChatType chat_type, String target, String message) {
		if(message == null || message.length() == 0)
			return;

		Matcher matcher = this.cmd_regex.matcher(message);
		if(!matcher.matches() || matcher.groupCount() < 2)
			return; // Didn't match or the regex is no good.

		String command = matcher.group(1).toLowerCase();
		String arguments = matcher.group(2);

		// Iterate through lists and see if anything wants to handle it...
		List<CommandHandler> hlist = this.commands.get(command);
		if(hlist != null)
			for(CommandHandler handler : hlist)
				handler.handleCommand(chat_type, target, command, arguments);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}