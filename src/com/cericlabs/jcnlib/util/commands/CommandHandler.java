package com.cericlabs.jcnlib.util.commands;

import com.cericlabs.jcnlib.ChatType;


/**
 * The CommandHandler interface defines a method for receiving command usage notifications.
 */
public interface CommandHandler {
	/**
	 * The handleCommand method is called whenever the command this handler is registered to is
	 * used.
	 *
	 * @param chat_type
	 *	The ChatType of the message the command was received on. Compare this to the values in the
	 *	enum ChatType.
	 *
	 * @param player
	 *	The player using the command. This parameter will always be the name of the remote player
	 *	issuing the command.
	 *
	 * @param command
	 *	The actual command used. Useful if (and only if) the handler is registered to multiple
	 *	commands.
	 *
	 * @param arguments
	 *	Any additional arguments received with the command. If the command is used without any
	 *	arguments (!help vs !help arg), this parameter will be null.
	 */
	public void handleCommand(ChatType chat_type, String player, String command, String arguments);
}