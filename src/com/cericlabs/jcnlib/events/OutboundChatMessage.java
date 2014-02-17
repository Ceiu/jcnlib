package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 * The OutboundChatMessage event is fired when a chat message is sent.
 */
public class OutboundChatMessage extends OutboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(OutboundChatMessage event);
	}

	public static class Dispatcher implements EventDispatcher.Dispatcher {
		private List<Handler> handlers;

		public Dispatcher() {
			this.handlers = new CopyOnWriteArrayList<Handler>();
		}

		public boolean registerHandler(CNEventHandler handler) {
			if(handler == null)
				throw new IllegalArgumentException("handler");

			if(handler instanceof Handler)
				if(!this.handlers.contains(handler))
					return this.handlers.add((Handler)handler);

			return false;
		}

		public boolean removeHandler(CNEventHandler handler) {
			if(handler == null)
				throw new IllegalArgumentException("handler");

			return this.handlers.remove(handler);
		}

		public void dispatchEvent(CNEvent event) {
			if(event == null)
				throw new IllegalArgumentException("event");

			if(!(event instanceof OutboundChatMessage))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((OutboundChatMessage)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Pattern SPLIT_REGEX_A = Pattern.compile(":");
	private static final Pattern SPLIT_REGEX_B = Pattern.compile(";");

////////////////////////////////////////////////////////////////////////////////////////////////////

	private ChatType ctype;

	private String player;
	private String message;

	private int channel;
	private int frequency;
	private String squad;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new InboundChatMessage event from the specified chatnet message.
	 *
	 * @param connection
	 *	The connection this message was received on.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified message is formatted incorrectly.
	 *
	 * @throws NumberFormatException
	 *	if the specified message is formatted incorrectly.
	 */
	public OutboundChatMessage(CNConnection connection, String message) {
		super(connection);

		if(message == null)
			throw new IllegalArgumentException("message");

		String[] chunklets = SPLIT_REGEX_A.split(message, 3);

		if(chunklets.length != 3)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("SEND"))
			throw new IllegalArgumentException("message");

		// Initialize common variables...
		this.player = null;
		this.message = null;

		this.channel = -1;
		this.frequency = -1;
		this.squad = null;

		// Parse message...
		switch((this.ctype = ChatType.translate(chunklets[1]))) {
			case CHAT:				// SEND:CHAT:channel;message
				chunklets = SPLIT_REGEX_B.split(chunklets[2], 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.channel = Integer.parseInt(chunklets[0]);
				if(this.channel < 1 ) throw new NumberFormatException();

				this.message = chunklets[1];
				break;

			case COMMAND:			// SEND:CMD:msg
			case PUBLIC:			// SEND:PUB:msg
			case PUBLIC_MACRO:		// SEND:PUBM:msg
			case MODERATOR:			// SEND:MOD:msg
				this.message = chunklets[2];
				break;

			case PRIVATE:			// SEND:PRIV:name:msg
			case PRIVATE_COMMAND:	// SEND:PRIVCMD:name:msg
				chunklets = SPLIT_REGEX_A.split(chunklets[2], 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.player = chunklets[0];
				this.message = chunklets[1];
				break;

			case FREQUENCY:			// SEND:FREQ:freq:msg
				chunklets = SPLIT_REGEX_A.split(chunklets[2], 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.frequency = Integer.parseInt(chunklets[0]);
				if(this.frequency < 0 || this.frequency > 9999) throw new NumberFormatException();

				this.message = chunklets[1];
				break;

			case SQUAD:				// SEND:SQUAD:squad:msg
				chunklets = SPLIT_REGEX_A.split(chunklets[2], 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.squad = chunklets[0];
				this.message = chunklets[1];
				break;

			default:
				throw new IllegalArgumentException("message");
		}
	}

	public OutboundChatMessage(CNConnection connection, ChatType chat_type, String message) {
		super(connection);

		if(chat_type == null)
			throw new IllegalArgumentException("chat_type");

		if(message == null)
			throw new IllegalArgumentException("message");

		// Initialize common variables...
		this.player = null;
		this.message = null;

		this.channel = -1;
		this.frequency = -1;
		this.squad = null;

		// Parse message...
		switch((this.ctype = chat_type)) {
			case COMMAND:			// SEND:CMD:msg
			case PUBLIC:			// SEND:PUB:msg
			case PUBLIC_MACRO:		// SEND:PUBM:msg
			case MODERATOR:			// SEND:MOD:msg
				this.message = message;
				break;

			default:
				throw new IllegalArgumentException("chat_type");
		}
	}

	public OutboundChatMessage(CNConnection connection, ChatType chat_type, String target, String message) {
		super(connection);

		if(chat_type == null)
			throw new IllegalArgumentException("chat_type");

		if(target == null)
			throw new IllegalArgumentException("target");

		if(message == null)
			throw new IllegalArgumentException("message");

		// Initialize common variables...
		this.player = null;
		this.message = null;

		this.channel = -1;
		this.frequency = -1;
		this.squad = null;

		// Parse message...
		switch((this.ctype = chat_type)) {
			case PRIVATE:			// SEND:PRIV:name:msg
			case PRIVATE_COMMAND:	// SEND:PRIVCMD:name:msg
				this.player = target;
				this.message = message;
				break;

			case SQUAD:				// SEND:SQUAD:squad:msg
				this.squad = target;
				this.message = message;
				break;

			default:
				throw new IllegalArgumentException("chat_type");
		}
	}

	public OutboundChatMessage(CNConnection connection, ChatType chat_type, int target, String message) {
		super(connection);

		if(chat_type == null)
			throw new IllegalArgumentException("chat_type");

		if(message == null)
			throw new IllegalArgumentException("message");

		// Initialize common variables...
		this.player = null;
		this.message = null;

		this.channel = -1;
		this.frequency = -1;
		this.squad = null;

		// Parse message...
		switch((this.ctype = chat_type)) {
			case CHAT:				// SEND:CHAT:channel;message
				if(target < 1)
					throw new IllegalArgumentException("target");

				this.channel = target;
				this.message = message;
				break;

			case FREQUENCY:			// SEND:FREQ:freq:msg
			case PRIVATE_COMMAND:	// SEND:PRIVCMD:name:msg
				if(target < 0 || target > 9999)
					throw new IllegalArgumentException("target");

				this.frequency = target;
				this.message = message;
				break;

			default:
				throw new IllegalArgumentException("chat_type");
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the ChatType representing the chat medium this message will be sent on.
	 *
	 * @return
	 *	The ChatType representing this message.
	 */
	public ChatType getChatType() {
		return this.ctype;
	}

	/**
	 * Returns the name of the player that will receive this message. This only applies to PRIVATE
	 * and PRIVATE_COMMAND message types and will return null for all others.
	 *
	 * @return
	 *	The player that will receive this message.
	 */
	public String getPlayer() {
		return this.player;
	}

	/**
	 * Returns the message that will be sent.
	 *
	 * @return
	 *	The message to be sent.
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns the chat channel that will receive this message. This only applies to the CHAT
	 * message type and will return -1 for all other types.
	 *
	 * @return
	 *	The chat channel to receive this message.
	 */
	public int getChannel() {
		return this.channel;
	}

	/**
	 * Returns the frequency that will receive this message. This only applies to FREQUENCY message
	 * types and will return -1 for all others.
	 *
	 * @return
	 *	The frequency to receive this message.
	 */
	public int getFrequency() {
		return this.frequency;
	}

	/**
	 * Returns the squad that will receive this message. This only applies to SQUAD message types
	 * and will return null for all other types.
	 *
	 * @return
	 *	The squad that is receiving this message, or null if this is not a squad message.
	 */
	public String getSquad() {
		return this.squad;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sets the ChatType representing the chat medium this message will be sent on.
	 *
	 * @throws IllegalArgumentException
	 *	if chat_type is null or an unsupported value.
	 */
	public void setChatType(ChatType chat_type) {
		if(chat_type == null || chat_type == ChatType.UNKNOWN)
			throw new IllegalArgumentException("chat_type");

		this.ctype = chat_type;
	}

	/**
	 * Sets the name of the player that will receive this message. This only applies to PRIVATE and
	 * PRIVATE_COMMAND message types and is ignored by all others.
	 *
	 * @param player
	 *	The player to receive this message.
	 *
	 * @throws IllegalArgumentException
	 *	if player is null or zero-length.
	 */
	public void setPlayer(String player) {
		if(player == null || player.length() < 1)
			throw new IllegalArgumentException("player");

		this.player = player;
	}

	/**
	 * Sets the message that will be sent.
	 *
	 * @param message
	 *	The message to be sent. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if message is null.
	 */
	public void setMessage(String message) {
		if(message == null)
			throw new IllegalArgumentException("message");

		this.message = message;
	}

	/**
	 * Sets the chat channel that will receive this message. This only applies to the CHAT
	 * message type and is ignored by all other types.
	 *
	 * @param channel
	 *	The chat channel to receive this message. Must be positive, though most servers ignore
	 *	channel numbers higher than 9.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified channel is less than 1.
	 */
	public void setChannel(int channel) {
		if(channel < 1)
			throw new IllegalArgumentException("channel");

		this.channel = channel;
	}

	/**
	 * Sets the frequency that will receive this message. This only applies to FREQUENCY message
	 * types and is ignored by all other types.
	 *
	 * @param frequency
	 *	The frequency to receive this message. Must be between 0 and 9999, inclusively.
	 *
	 * @throws IllegalArgumentException
	 *	if frequency is lower than 0 or higher than 9999.
	 */
	public void setFrequency(int frequency) {
		if(frequency < 0 || frequency > 9999)
			throw new IllegalArgumentException("frequency");

		this.frequency = frequency;
	}

	/**
	 * Sets the squad that will receive this message. This only applies to SQUAD message types, and
	 * is ignored by all other types.
	 *
	 * @param squad
	 *	The squad that should this message.
	 *
	 * @throws IllegalArgumentException
	 *	if squad is null or zero-length.
	 */
	public void setSquad(String squad) {
		if(squad == null || squad.length() < 1)
			throw new IllegalArgumentException("squad");

		this.squad = squad;
	}


////////////////////////////////////////////////////////////////////////////////////////////////////

	public String toMessage() {
		StringBuilder builder = new StringBuilder("SEND:");

		switch(this.ctype) {
			case CHAT:				// SEND:CHAT:msg
				builder.append("CHAT:").append(this.channel).append(';').append(this.message);
				break;

			case COMMAND:			// SEND:CMD:msg
				builder.append("CMD:").append(this.message);
				break;

			case PUBLIC:			// SEND:PUB:msg
				builder.append("PUB:").append(this.message);
				break;

			case PUBLIC_MACRO:		// SEND:PUBM:msg
				builder.append("PUBM:").append(this.message);
				break;

			case MODERATOR:			// SEND:MOD:msg
				builder.append("MOD:").append(this.message);
				break;

			case PRIVATE:			// SEND:PRIV:name:msg
				builder.append("PRIV:").append(this.player).append(':').append(this.message);
				break;

			case PRIVATE_COMMAND:	// SEND:PRIVCMD:name:msg
				builder.append("PRIVCMD:").append(this.player).append(':').append(this.message);
				break;

			case FREQUENCY:			// SEND:FREQ:freq:msg
				builder.append("FREQ:").append(this.frequency).append(':').append(this.message);
				break;

			case SQUAD:				// SEND:SQUAD:squad:msg
				builder.append("SQUAD:").append(this.squad).append(':').append(this.message);
				break;

			default:
				throw new IllegalStateException("chat_type");
		}

		return builder.toString();
	}

}