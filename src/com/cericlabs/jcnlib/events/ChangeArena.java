package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 * The ChangeArena event is fired whenever a connection attempts to change arenas.
 */
public class ChangeArena extends OutboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(ChangeArena event);
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

			if(!(event instanceof ChangeArena))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((ChangeArena)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private String arena;	// The target arena.

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new ChangeArena event from the specified chatnet message or arena name.
	 *
	 * @param message
	 *	The chatnet message to use to build this event, or the arena
	 */
	public ChangeArena(CNConnection connection, String message) {
		super(connection);

		if(message.indexOf(':') != -1) {
			String[] chunklets = message.split(":", 2);

			if(!chunklets[0].equalsIgnoreCase("GO"))
				throw new IllegalArgumentException("message");

			this.setArena(chunklets[1]);
		} else {
			this.setArena(message);
		}
	}

	/**
	 * Creates a new ChangeArena event from the specified public arena number.
	 *
	 * @param arena
	 *	The public arena to change to.
	 */
	public ChangeArena(CNConnection connection, int arena) {
		super(connection);

		this.setArena(arena);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the arena the client is attempting to change to.
	 *
	 * @return
	 *	The name of the arena the client entered.
	 */
	public String getArena() {
		return this.arena;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public void setArena(String arena) {
		if(arena == null)
			throw new IllegalArgumentException("arena");

		this.arena = arena;
	}

	public void setArena(int arena) {
		if(arena < 0 || arena > 255)
			throw new IllegalArgumentException("arena");

		this.arena = String.valueOf(arena);
	}

	public String toMessage() {
		return "GO:" + this.arena;
	}

}