package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;



/**
 * The PlayerLeft event is fired whenever a player leaves the arena. Applications using this event
 * should note that by the time this event is fired, the player is no longer in the current arena.
 */
public class PlayerLeft extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(PlayerLeft event);
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

			if(!(event instanceof PlayerLeft))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((PlayerLeft)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String player_name;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new PlayerLeft event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public PlayerLeft(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 2);

		if(chunklets.length != 2)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("LEAVING"))
			throw new IllegalArgumentException("message");


		this.player_name = chunklets[1];
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the player who left.
	 *
	 * @return
	 *	See above.
	 */
	public String getPlayerName() {
		return this.player_name;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}