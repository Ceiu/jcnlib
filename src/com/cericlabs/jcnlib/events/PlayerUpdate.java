package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;



/**
 * The PlayerUpdate event is fired whenever a player changes their ship, frequency or both.
 */
public class PlayerUpdate extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(PlayerUpdate event);
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

			if(!(event instanceof PlayerUpdate))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((PlayerUpdate)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String player_name;
	private final Ship ship;
	private final int freq;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new PlayerUpdate event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public PlayerUpdate(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 4);

		if(chunklets.length != 4)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("SHIPFREQCHANGE"))
			throw new IllegalArgumentException("message");

		this.player_name = chunklets[1];
		this.ship = Ship.translate(Integer.parseInt(chunklets[2]));
		this.freq = Integer.parseInt(chunklets[3]);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the player that has changed ships or frequencies.
	 *
	 * @return
	 *	See above.
	 */
	public String getPlayerName() {
		return this.player_name;
	}

	/**
	 * Returns the ship the player is now using.
	 *
	 * @return
	 *	See above.
	 */
	public Ship getShip() {
		return this.ship;
	}

	/**
	 * Returns the frequency the player is now on.
	 *
	 * @return
	 *	See above.
	 */
	public int getFrequency() {
		return this.freq;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}