package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;



/**
 * The CNEvent class represents any ChatNet message, both inbound and outbound.
 * <p/>
 * This class should not be directly subclassed. Instead, subclass either InboundCNMessage or
 * OutboundCNMessage, depending on the message the new class will represent.
 *
 * @author Chris "Ceiu" Rog
 */
public abstract class CNEvent {

	/**
	 * Defines a method for receiving all CNEvents.
	 */
	public static interface Handler extends CNEventHandler {
		public void handleEvent(CNEvent event);
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

			if(!(event instanceof CNEvent))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((CNEvent)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** The connection this message is associated with. */
	public final CNConnection connection;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new CNEvent object that represents the specified chatnet message.
	 *
	 * @param message
	 *	The Chatnet message this event represents.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified message is null.
	 */
	CNEvent(CNConnection connection) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		this.connection = connection;
	}

	/**
	 * Creates a new CNEvent using the specified event as the basis for this instance. Useful for
	 * creating sub-events or existing events.
	 *
	 * @param event
	 *	The event this event should be based on.
	 *
	 * @throws IllegalArgumentException
	 *	If the specified event is null.
	 */
	CNEvent(CNEvent event) {
		if(event == null)
			throw new IllegalArgumentException("event");

		this.connection = event.connection;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts this event to a chatnet message. In the case of InboundCNEvents, this is identical
	 * to referencing the data field directly, as inbound messages are immutable. However, for
	 * OutboundCNEvents, the message is generated each time this method is called.
	 */
	public abstract String toMessage();

////////////////////////////////////////////////////////////////////////////////////////////////////

}