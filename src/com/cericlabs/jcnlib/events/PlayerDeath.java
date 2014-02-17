package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;



/**
 * The PlayerDeath event is fired whenever a player is killed.
 */
public class PlayerDeath extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(PlayerDeath event);
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

			if(!(event instanceof PlayerDeath))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((PlayerDeath)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String killer;
	private final String killed;
	private final int bounty;
	private final int flags;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new PlayerDeath event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public PlayerDeath(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 5);

		if(chunklets.length != 5)
			throw new IllegalArgumentException("messages");

		if(!chunklets[0].equalsIgnoreCase("KILL"))
			throw new IllegalArgumentException("messages");

		this.killer = chunklets[1];
		this.killed = chunklets[2];
		this.bounty = Integer.parseInt(chunklets[3]);
		this.flags = Integer.parseInt(chunklets[4]);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the killer.
	 *
	 * @return
	 *	See above.
	 */
	public String getKiller() {
		return this.killer;
	}

	/**
	 * Returns the name of the player that was killed.
	 *
	 * @return
	 *	See above.
	 */
	public String getKilled() {
		return this.killed;
	}

	/**
	 * Returns the bounty of the killed player at the time of death. Certain game modes may
	 * generate this value based on the actual bounty at the time of death.
	 *
	 * @return
	 *	The bounty of the killed player at the time of death.
	 */
	public int getBounty() {
		return this.bounty;
	}

	/**
	 * Returns the number of flags the killed player was carrying at the time of death.
	 *
	 * @return
	 *	See above.
	 */
	public int getFlagsCarried() {
		return this.flags;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}