package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 * The InboundChatMessage event is fired when a chat message is received.
 */
public class InboundChatMessage extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(InboundChatMessage event);
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

			if(!(event instanceof InboundChatMessage))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((InboundChatMessage)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final ChatType ctype;

	private final String player;
	private final String message;

	private final int channel;
	private final String squad;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new InboundChatMessage event from the specified chatnet message.
	 *
	 * @param connection
	 *	The connection this message was received on.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public InboundChatMessage(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 3);

		if(chunklets.length != 3)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("MSG"))
			throw new IllegalArgumentException("message");

		switch((this.ctype = ChatType.translate(chunklets[1]))) {
			case ARENA:				// MSG:ARENA:msg
			case COMMAND:			// MSG:CMD:msg
			case SYSOP:				// MSG:SYSOP:msg
				this.message = chunklets[2];

				this.player = null;
				this.channel = -1;
				this.squad = null;
				break;


			case PUBLIC:			// MSG:PUB:name:msg
			case PUBLIC_MACRO:		// MSG:PUBM:name:msg
			case PRIVATE:			// MSG:PRIV:name:msg
			case FREQUENCY:			// MSG:FREQ:name:msg
			case MODERATOR:			// MSG:MOD:name:msg
				chunklets = chunklets[2].split(":", 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.player = chunklets[0];
				this.message = chunklets[1];

				this.channel = -1;
				this.squad = null;
				break;

			case CHAT:				//MSG:CHAT:channelnum:msg
				chunklets = chunklets[2].split(":", 2);
				if(chunklets.length != 2) throw new IllegalArgumentException("message");

				this.channel = Integer.parseInt(chunklets[0]);
				this.message = chunklets[1];

				this.player = null;
				this.squad = null;
				break;

			case SQUAD:				// MSG:SQUAD:squad:sender:msg
				chunklets = chunklets[2].split(":", 3);
				if(chunklets.length != 3) throw new IllegalArgumentException("message");

				this.squad = chunklets[0];
				this.player = chunklets[1];
				this.message = chunklets[2];

				this.channel = -1;
				break;

			default:
				throw new IllegalArgumentException("message");
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the ChatType representing the chat medium this message was received on.
	 *
	 * @return
	 *	The ChatType representing this message.
	 */
	public ChatType getChatType() {
		return this.ctype;
	}

	/**
	 * Returns the player that sent this message. If the message does not contain a well-defined
	 * player field, this will return null. For reference, the Arena, Command, Sysop and Chat types
	 * will never have a player field.
	 *
	 * @return
	 *	The player that sent this message, or null if the player is not well-defined.
	 */
	public String getPlayer() {
		return this.player;
	}

	/**
	 * Returns the message sent.
	 *
	 * @return
	 *	See above.
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns the channel this message was received on. This only applies to Chat message types
	 * and will return -1 for all other types.
	 *
	 * @return
	 *	The chat channel this message was received on, or -1 if this is not a Chat message.
	 */
	public int getChannel() {
		return this.channel;
	}

	/**
	 * Returns the squad that is receiving this message. This only applies to Squad message types
	 * and will return null for all other types.
	 *
	 * @return
	 *	The squad that is receiving this message, or null if this is not a Squad message.
	 */
	public String getSquad() {
		return this.squad;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}