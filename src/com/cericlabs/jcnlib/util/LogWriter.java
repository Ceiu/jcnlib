package com.cericlabs.jcnlib.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;


/**
 * The LogWriter class handles events and writes Continuum-compatible logs from their contents. This
 * class can be used as an event handler, or standalone by simply calling each event handler as
 * necessary.
 *
 * @author Chris "Ceiu" Rog
 */
public class LogWriter
	implements

		InboundChatMessage.Handler,
		LoginResponse.Handler,
		OutboundChatMessage.Handler,
		PlayerDeath.Handler,
		PlayerEntered.Handler,
		PlayerLeft.Handler {

////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Map<ChatType, Character> CHAT_PREFIXES;

	static {
		LogWriter.CHAT_PREFIXES = new HashMap<ChatType, Character>(15, 1);

		LogWriter.CHAT_PREFIXES.put(ChatType.UNKNOWN, 'X');
		LogWriter.CHAT_PREFIXES.put(ChatType.ARENA, ' ');
		LogWriter.CHAT_PREFIXES.put(ChatType.CHAT, 'C');
		LogWriter.CHAT_PREFIXES.put(ChatType.COMMAND, ' ');
		LogWriter.CHAT_PREFIXES.put(ChatType.FREQUENCY, 'F');
		LogWriter.CHAT_PREFIXES.put(ChatType.MODERATOR, '*');
		LogWriter.CHAT_PREFIXES.put(ChatType.PUBLIC, ' ');
		LogWriter.CHAT_PREFIXES.put(ChatType.PUBLIC_MACRO, ' ');
		LogWriter.CHAT_PREFIXES.put(ChatType.PRIVATE, 'P');
		LogWriter.CHAT_PREFIXES.put(ChatType.PRIVATE_COMMAND, 'P');
		LogWriter.CHAT_PREFIXES.put(ChatType.SQUAD, 'P');
		LogWriter.CHAT_PREFIXES.put(ChatType.SYSOP, '*');
	}


////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Writer out;

	private String username;

	private final int namelen;
	private final int linelen;

	private final int stdmsg_len;
	private final Pattern stdmsg_regex;
	private final String stdmsg_format_1;
	private final String stdmsg_format_2;

	private final int spcmsg_len;
	private final Pattern spcmsg_regex;
	private final String spcmsg_format_1;
	private final String spcmsg_format_2;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new LogWriter instance that writes to the specified Writer, using a name length of
	 * 10 characters and no wrapping.
	 *
	 * @param out
	 *	The writer to write log data to.
	 */
	public LogWriter(Writer out) {
		this(out, null, 10, 0);
	}

	/**
	 * Creates a new LogWriter instance that writes to the specified Writer, with the specified
	 * name length and line length. If the specified line length is zero, no wrapping is performed.
	 *
	 * @param out
	 *	The writer to write log data to.
	 *
	 * @param namelen
	 *	The length of names in standard messages.
	 *
	 * @param linelen
	 *	The length a line can be before wrapping occurs.
	 */
	public LogWriter(Writer out, int namelen, int linelen) {
		this(out, null, namelen, linelen);
	}

	/**
	 * Creates a new LogWriter instance that writes to the specified Writer, with the specified
	 * name length, line length and default username. The default username is used for displaying
	 * outbound messages properly.
	 *
	 * @param out
	 *	The writer to write log data to.
	 *
	 * @param default_username
	 *	The username to display for outbound messages. If this is null, the name "-UNKNOWN-" will
	 *	be used.
	 *
	 * @param namelen
	 *	The length of names in standard messages.
	 *
	 * @param linelen
	 *	The length a line can be before wrapping occurs.
	 */
	public LogWriter(Writer out, String default_username, int namelen, int linelen) {
		if(out == null)
			throw new IllegalArgumentException("writer");

		if(namelen < 1)
			throw new IllegalArgumentException("name length");

		if(linelen < 0)
			throw new IllegalArgumentException("line length");

		else if(linelen > 0 && linelen - (namelen + 4) < 1)
			throw new IllegalArgumentException("line length, name length");

		// Initialize variables...
		this.out = out;

		this.username = default_username != null ? default_username : "-UNKNOWN-";

		this.namelen = namelen;
		this.linelen = linelen;

		// Create patterns and formats...
		this.stdmsg_format_1 = String.format("%%1$c %%2$%1$d.%1$ds> %%3$s\n", this.namelen);
		this.spcmsg_format_1 = "%1$c %2$s\n";

		if(this.linelen > 0) {
			this.stdmsg_len = (this.linelen - (this.namelen + 4));
			this.stdmsg_regex = Pattern.compile(String.format("(.{1,%d})(?:\\n|\\s|\\z)", this.stdmsg_len));
			this.stdmsg_format_2 = String.format("%%2$%ds%%3$s\n", (this.namelen + 4));

			this.spcmsg_len = (this.linelen - 2);
			this.spcmsg_regex = Pattern.compile(String.format("(.{1,%d})(?:\\n|\\s|\\z)", this.spcmsg_len));
			this.spcmsg_format_2 = "  %2$s\n";
		} else {
			this.stdmsg_len = 0;
			this.stdmsg_regex = null;
			this.stdmsg_format_2 = null;

			this.spcmsg_len = 0;
			this.spcmsg_regex = null;
			this.spcmsg_format_2 = null;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Generic event handler. Attempts to cast the event to a known, handled event for processing.
	 */
	public void handleEvent(CNEvent event) {
		if(event == null)
			return;

		if(event instanceof InboundChatMessage)
			this.handleEvent((InboundChatMessage)event);

		else if(event instanceof OutboundChatMessage)
			this.handleEvent((OutboundChatMessage)event);

		else if(event instanceof PlayerDeath)
			this.handleEvent((PlayerDeath)event);

		else if(event instanceof PlayerEntered)
			this.handleEvent((PlayerEntered)event);

		else if(event instanceof PlayerLeft)
			this.handleEvent((PlayerLeft)event);
	}

	/**
	 * Stores the username the bot will be using.
	 */
	public void handleEvent(LoginResponse event) {
		if(event == null)
			return;

		if(event.accepted())
			this.username = event.getUsername();
	}

	/**
	 * Handles and formats player deaths.
	 */
	public void handleEvent(PlayerDeath event) {
		if(event == null)
			return;

		try {
			out.write(String.format("  %s(%d) killed by: %s\n", event.getKilled(), event.getBounty(), event.getKiller()));
			out.flush();
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}
	}

	/**
	 * Handles and formats player entering notifications.
	 */
	public void handleEvent(PlayerEntered event) {
		if(event == null)
			return;

		try {
			out.write(String.format("  %s entered arena.\n", event.getPlayerName()));
			out.flush();
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}
	}

	/**
	 * Handles and formats player leaving notifications.
	 */
	public void handleEvent(PlayerLeft event) {
		if(event == null)
			return;

		try {
			out.write(String.format("  %s left arena.\n", event.getPlayerName()));
			out.flush();
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}
	}

	/**
	 * Handles and formats inbound chat messages.
	 */
	public void handleEvent(InboundChatMessage event) {
		if(event == null)
			return;

		ChatType chat_type = event.getChatType();
		switch(chat_type) {
			case ARENA:
			case SYSOP:
				this.formatSpecialMessage(chat_type, event.getMessage());
				break;

			case CHAT:
				this.formatSpecialMessage(chat_type, String.format("%d:%s> %s", event.getChannel(), event.getPlayer(), event.getMessage()));
				return;

			case SQUAD:
				this.formatSpecialMessage(chat_type, String.format("(#%s)(%s)> %s", event.getSquad(), event.getPlayer(), event.getMessage()));
				return;

			default:
				this.formatStandardMessage(chat_type, event.getPlayer(), event.getMessage());
				return;
		}
	}

	/**
	 * Handles and formats outbound chat messages.
	 */
	public void handleEvent(OutboundChatMessage event) {
		if(event == null)
			return;

		ChatType chat_type = event.getChatType();
		switch(chat_type) {
			case ARENA:
			case SYSOP:
				this.formatSpecialMessage(chat_type, event.getMessage());
				break;

			case CHAT:
				this.formatSpecialMessage(chat_type, String.format("%d:%s> %s", event.getChannel(), this.username, event.getMessage()));
				return;

			case SQUAD:
				this.formatSpecialMessage(chat_type, String.format("(#%s)(%s)> %s", event.getSquad(), this.username, event.getMessage()));
				return;

			default:
				this.formatStandardMessage(chat_type, this.username, event.getMessage());
				return;
		}
	}

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Formats and writes system messages (Arena, sysop, chat, squad, unknown).
	 */
	private void formatSpecialMessage(ChatType chat_type, String message) {
		if(chat_type == null)
			throw new IllegalArgumentException("chat_type");

		if(message == null)
			throw new IllegalArgumentException("message");

		try {
			if(this.linelen > 0) {
				String format = this.spcmsg_format_1;
				char type = CHAT_PREFIXES.get(chat_type);
				int index = 0;

				for(Matcher m = this.spcmsg_regex.matcher(message); m.find(index); ) {
					if(m.start() != index) {
						out.write(String.format(format, type, message.substring(index, index + this.spcmsg_len)));

						message = message.substring(index + this.spcmsg_len);
						m = this.spcmsg_regex.matcher(message);
						index = 0;
					} else {
						out.write(String.format(format, type, m.group(1)));
						index = m.end();
					}

					format = this.spcmsg_format_2;
				}
			} else {
				out.write(String.format(this.spcmsg_format_1, CHAT_PREFIXES.get(chat_type), message));
			}

			out.flush();
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}
	}

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Formats and writes standard messages (Public, private, frequency, etc.).
	 */
	private void formatStandardMessage(ChatType chat_type, String player, String message) {
		if(chat_type == null)
			throw new IllegalArgumentException("chat_type");

		if(player == null)
			throw new IllegalArgumentException("player");

		if(message == null)
			throw new IllegalArgumentException("message");

		try {
			if(this.linelen > 0) {
				String format = this.stdmsg_format_1;
				char type = CHAT_PREFIXES.get(chat_type);
				int index = 0;

				for(Matcher m = this.stdmsg_regex.matcher(message); m.find(index); ) {
					if(m.start() != index) {
						out.write(String.format(format, type, player, message.substring(index, index + this.stdmsg_len)));

						message = message.substring(index + this.stdmsg_len);
						m = this.stdmsg_regex.matcher(message);
						index = 0;
					} else {
						out.write(String.format(format, type, player, m.group(1)));
						index = m.end();
					}

					player = ""; // Subsequent lines do not have a player field.
					format = this.stdmsg_format_2;
				}
			} else {
				out.write(String.format(this.stdmsg_format_1, CHAT_PREFIXES.get(chat_type), player, message));
			}

			out.flush();
		} catch(IOException e) {
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}