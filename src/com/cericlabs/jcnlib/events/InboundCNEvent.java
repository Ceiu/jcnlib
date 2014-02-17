package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 *
 *
 *
 */
public abstract class InboundCNEvent extends CNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(InboundCNEvent event);
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

			if(!(event instanceof InboundCNEvent))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((InboundCNEvent)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public final String data;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new inbound event associated with the specified connection.
	 *
	 * @param connection
	 *	The connection this event is being sent from. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified connection is null.
	 */
	protected InboundCNEvent(CNConnection connection, String message) {
		super(connection);

		if(message == null || message.length() < 1)
			throw new IllegalArgumentException("message");

		this.data = message;
	}

	/**
	 * Creates a new inbound event based on the specified event.
	 *
	 * @param event
	 *	The event to base this event on.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified event is null.
	 */
	protected InboundCNEvent(CNEvent event) {
		super(event);

		this.data = event.toMessage();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	public final String toMessage() {
		return this.data;
	}
}