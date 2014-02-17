package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;


/**
 * The OutboundCNEvent class is the superclass for all events representing outbound messages (such
 * as: ChangeArena, ChangeFrequency, OutboundChatMessage, etc.)
 * <p/>
 * Subclasses of this class must implement the toMessage method, which builds the chatnet message
 * to be sent.
 */
public abstract class OutboundCNEvent extends CNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(OutboundCNEvent event);
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

			if(!(event instanceof OutboundCNEvent))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((OutboundCNEvent)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Whether or not this message will be sent after the handlers process it. */
	private boolean suppressed;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new outbound event associated with the specified connection.
	 *
	 * @param connection
	 *	The connection this event is being sent from. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified connection is null.
	 */
	protected OutboundCNEvent(CNConnection connection) {
		super(connection);

		this.suppressed = false;
	}

	/**
	 * Creates a new outbound event based on the specified event.
	 *
	 * @param event
	 *	The event to base this event on.
	 *
	 * @throws IllegalArgumentException
	 *	if the specified event is null.
	 */
	protected OutboundCNEvent(CNEvent event) {
		super(event);

		this.suppressed = false;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Marks this event as "suppressed." Supressed messages are still processed by event handlers,
	 * but will not be sent once the handlers are finished running.
	 */
	public final void supress() {
		this.suppressed = true;
	}

	/**
	 * Checks if this event has been suppressed. If this method returns true, the event will not
	 * be sent as a message after event handlers have processed it.
	 *
	 * @return
	 *	True if this message is supressed; false otherwise.
	 */
	public final boolean isSuppressed() {
		return this.suppressed;
	}

}