package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 * The EnteredArena event is fired whenever the client enters a new arena. Applications should use
 * this event as a notification to clear any cached player lists.
 */
public class EnteredArena extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(EnteredArena event);
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

			if(!(event instanceof EnteredArena))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((EnteredArena)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String arena;	// The arena entered
	private final int freq;		// The initial frequency (spec freq, likely)

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new EnteredArena event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public EnteredArena(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 3);

		if(chunklets.length != 3)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("INARENA"))
			throw new IllegalArgumentException("message");

		this.arena = chunklets[1];
		this.freq = Integer.parseInt(chunklets[2]);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the arena the client has entered. This could be a named arena or the
	 * number of a public arena.
	 *
	 * @return
	 *	The name of the arena the client entered.
	 */
	public String getArenaName() {
		return this.arena;
	}

	/**
	 * Returns the frequency the client has been assigned to.
	 *
	 * @return
	 *	See above.
	 */
	public int getFrequency() {
		return this.freq;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}