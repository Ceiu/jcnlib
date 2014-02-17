package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;

/**
 * The ChangeFrequency event is fired whenever a connection sends a request to change frequencies.
 */
public class ChangeFrequency extends OutboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(ChangeFrequency event);
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

			if(!(event instanceof ChangeFrequency))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((ChangeFrequency)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** The frequency to change to. */
	private int frequency;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new ChangeArena event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public ChangeFrequency(CNConnection connection, String message) {
		super(connection);

		String[] chunklets = message.split(":", 2);

		if(chunklets.length != 2)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].equalsIgnoreCase("CHANGEFREQ"))
			throw new IllegalArgumentException("message");

		this.setFrequency(Integer.parseInt(chunklets[1]));
	}

	/**
	 * Creates a new ChangeFrequency event from the specified frequency.
	 *
	 * @param connection
	 *	The connection this event will occur on.
	 *
	 * @param frequency
	 *	The freuency to change to.
	 */
	public ChangeFrequency(CNConnection connection, int frequency) {
		super(connection);

		this.setFrequency(frequency);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the arena the client is requesting to change to.
	 *
	 * @return
	 *	The name of the arena the client entered.
	 */
	public int getFrequency() {
		return this.frequency;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sets the frequency the connection is requesting to change to.
	 *
	 * @param frequency
	 *	The frequency to change to. Must be between 0 and 9999, inclusively.
	 */
	public void setFrequency(int frequency) {
		if(frequency < 0 || frequency > 9999)
			throw new IllegalArgumentException("frequency");

		this.frequency = frequency;
	}

	public String toMessage() {
		return "CHANGEFREQ:" + String.valueOf(this.frequency);
	}

}