package com.cericlabs.jcnlib.util;

import java.util.*;
import java.util.concurrent.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;
import com.cericlabs.jcnlib.util.preprocessor.*;


/**
 * The EventDispatcher translates messages into events (through use of a MessageTranslator) and
 * dispatches events to event handlers.
 *
 * The order in which handlers are given events is undefined. The order in which event handler
 *
 * @author Chris "Ceiu" Rog
 */
public class EventDispatcher implements MessagePreprocessor {

	/**
	 * The Dispatcher interface defines methods for handling registration and removal of event
	 * handlers, as well as a method for dispatching an event.
	 */
	public static interface Dispatcher {
		/**
		 * Called when a handler is being registered with this dispatcher. The dispatcher should
		 * check whether or not the dispatcher is of the correct type (using instanceof, etc.),
		 * before completing the registration.
		 * <p/>
		 * This method should return true if and only if the given handler is of the correct type
		 * and the registration was completed. In all other cases, it should return false.
		 *
		 * @param handler
		 *	The handler to register with this dispatcher.
		 *
		 * @return
		 *	True if the handler was registered; false otherwise.
		 */
		public boolean registerHandler(CNEventHandler handler);

		/**
		 * Called when a handler is being removed from this dispatcher. The dispatcher should
		 * return true if and only if the handler is registered and a call to this method results
		 * in its removal.
		 *
		 * @param handler
		 *	The handler to remove from this dispatcher.
		 *
		 * @return
		 *	True if the handler was removed; false otherwise.
		 */
		public boolean removeHandler(CNEventHandler handler);

		/**
		 * Called when this dispatcher should dispatch the specified event. The dispatcher should
		 * verify the event is of the correct type (using instanceof, etc.) before dispatching it
		 * to registered handlers.
		 *
		 * @param event
		 *	The event to dispatch.
		 */
		public void dispatchEvent(CNEvent event);
	}

	////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Represents a generic inbound event.
	 */
	private static class UnknownInboundEvent extends InboundCNEvent {

		public UnknownInboundEvent(CNConnection connection, String message) {
			super(connection, message);
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * INTERNAL USE ONLY.<br/>
	 * Represents a generic outbound event.
	 */
	private static class UnknownOutboundEvent extends OutboundCNEvent {

		private final String message;

		//////////////////////////////////////////////////

		public UnknownOutboundEvent(CNConnection connection, String message) {
			super(connection);

			if(message == null || message.length() == 0)
				throw new IllegalArgumentException("");

			this.message = message;
		}

		//////////////////////////////////////////////////

		public String toMessage() {
			return this.message;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final MessageTranslator translator;
	private final Map<Class<? extends CNEvent>, Dispatcher> dispatchers;


	/**
	 * Creates a new EventDispatcher using the default message translations.
	 */
	public EventDispatcher() {
		this(new MessageTranslator());
	}

	/**
	 * Creates a new EventDispatcher using the specified MessageTranslator.
	 *
	 * @param translator
	 *	The MessageTranslator to use for translating messages to events. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if translator is null.
	 */
	public EventDispatcher(MessageTranslator translator) {
		if(translator == null)
			throw new IllegalArgumentException("translator");

		// Create stuff...
		this.translator = translator;
		this.dispatchers = new ConcurrentHashMap<Class<? extends CNEvent>, Dispatcher>();

		// Register dispatchers...
		// Generic events
		this.registerDispatcher(CNEvent.class, new CNEvent.Dispatcher());

		// Inbound events
		this.registerDispatcher(InboundCNEvent.class, new InboundCNEvent.Dispatcher());
		this.registerDispatcher(EnteredArena.class, new EnteredArena.Dispatcher());
		this.registerDispatcher(InboundChatMessage.class, new InboundChatMessage.Dispatcher());
		this.registerDispatcher(LoginResponse.class, new LoginResponse.Dispatcher());
		this.registerDispatcher(PlayerDeath.class, new PlayerDeath.Dispatcher());
		this.registerDispatcher(PlayerEntered.class, new PlayerEntered.Dispatcher());
		this.registerDispatcher(PlayerLeft.class, new PlayerLeft.Dispatcher());
		this.registerDispatcher(PlayerUpdate.class, new PlayerUpdate.Dispatcher());

		// Outbound events
		this.registerDispatcher(OutboundCNEvent.class, new OutboundCNEvent.Dispatcher());
		this.registerDispatcher(ChangeArena.class, new ChangeArena.Dispatcher());
		this.registerDispatcher(ChangeFrequency.class, new ChangeFrequency.Dispatcher());
		this.registerDispatcher(OutboundChatMessage.class, new OutboundChatMessage.Dispatcher());
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the MessageTranslator responsible for translating messages to events for this
	 * dispatcher.
	 *
	 */
	public MessageTranslator getTranslator() {
		return this.translator;
	}

	/**
	 * Registers a dispatcher for the specified event class.
	 *
	 * @param event_class
	 *	The event class that will be managed by the specified dispatcher.
	 *
	 * @param dispatcher
	 *	The dispatcher to register.
	 *
	 * @return
	 *	The previously registered dispatcher, or null if no dispatcher was replaced.
	 */
	public Dispatcher registerDispatcher(Class<? extends CNEvent> event_class, Dispatcher dispatcher) {
		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		if(dispatcher == null)
			throw new IllegalArgumentException("dispatcher");

		return this.dispatchers.put(event_class, dispatcher);
	}

	/**
	 * Retrieves the dispatcher registered for the specified event class.
	 *
	 * @param event_class
	 *	The event class for which to retrieve the dispatcher.
	 *
	 * @return
	 *	The dispatcher registered to the specified event class or null if no dispatcher is
	 *	registered.
	 */
	public Dispatcher getDispatcher(Class<? extends CNEvent> event_class) {
		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		return this.dispatchers.get(event_class);
	}

	/**
	 * Removes and returns the dispatcher associated with the specified event class.
	 *
	 * @param event_class
	 *	The event class for which to remove the dispatcher.
	 *
	 * @return
	 *	The dispatcher registered to the specified event class or null if no dispatcher is
	 *	registered for the class.
	 */
	public Dispatcher removeDispatcher(Class<? extends CNEvent> event_class) {
		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		return this.dispatchers.remove(event_class);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Registers an event handler for the specified class.
	 *
	 * @param event_class
	 *	The event class the handler will be registered for.
	 *
	 * @param handler
	 *	The event handler to register.
	 *
	 * @return
	 *	True if the handler was registered; false otherwise.
	 */
	public boolean registerHandler(Class<? extends CNEvent> event_class, CNEventHandler handler) {
		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		if(handler == null)
			throw new IllegalArgumentException("handler");

		Dispatcher dispatcher;

		if((dispatcher = this.dispatchers.get(event_class)) != null)
			if(dispatcher.registerHandler(handler))
				return true;

		return false;
	}

	/**
	 * Registers the specified handler with as many dispatchers as possible. This method attempts
	 * to register the handler with every registered dispatcher, then returns the number of
	 * dispatchers that reported successful registration.
	 *
	 * @param handler
	 *	The handler to register.
	 *
	 * @return
	 *	The number of dispatchers the handler was registered with.
	 */
	public int registerHandlerEx(CNEventHandler handler) {
		if(handler == null)
			throw new IllegalArgumentException("handler");

		int count = 0;
		for(Dispatcher dispatcher : this.dispatchers.values())
			if(dispatcher.registerHandler(handler))
				++count;

		return count;
	}

	/**
	 * Removes the handler from the dispatcher for the specified event.
	 *
	 * @param event_class
	 *	The event class the handler will no longer process.
	 *
	 * @param handler
	 *	The handler to remove.
	 *
	 * @return
	 *	True if the handler was removed from the specified event class' dispatcher; false
	 *	otherwise.
	 */
	public boolean removeHandler(Class<? extends CNEvent> event_class, CNEventHandler handler) {
		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		if(handler == null)
			throw new IllegalArgumentException("handler");

		Dispatcher dispatcher;

		for(Class<?> eclass = event_class; eclass != null; eclass = eclass.getSuperclass())
			if((dispatcher = this.dispatchers.get(eclass)) != null)
				if(dispatcher.removeHandler(handler))
					return true;

		return false;
	}

	/**
	 * Removes the specified handler from every dispatcher it's registered with.
	 *
	 * @param handler
	 *	The handler to remove.
	 *
	 * @return
	 *	The number of dispatchers the handler was removed from.
	 */
	public int removeHandlerEx(CNEventHandler handler) {
		if(handler == null)
			throw new IllegalArgumentException("handler");

		int count = 0;
		for(Dispatcher dispatcher : this.dispatchers.values())
			if(dispatcher.removeHandler(handler))
				++count;

		return count;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Translates the specified message to an event, dispatches the event to all registered event
	 * handlers, then sends the message.
	 *
	 * @param message
	 *	The message to translate and send. Cannot be null.
	 *
	 * @return
	 *	True if the message was sent; false otherwise.
	 */
	@Override
	public String processOutboundMessage(CNConnection connection, String message) {
		if(message == null || message.length() == 0)
			return message;

		// Translate message...
		CNEvent event = this.translator.translate(connection, message);
		if(event == null) event = new UnknownOutboundEvent(connection, message);

		// Dispatch event...
		Dispatcher dispatcher;
		for(Class<?> event_class = event.getClass(); event_class != null; event_class = event_class.getSuperclass())
			if((dispatcher = this.dispatchers.get(event_class)) != null)
				dispatcher.dispatchEvent(event);

		// If it's an outbound message, make sure it's not suppressed.
		if((event instanceof OutboundCNEvent) && ((OutboundCNEvent)event).isSuppressed())
			return null;

		// Return the message...
		return message;
	}

	/**
	 * Receives a message, translates the message to an event, then dispatches the event to all
	 * registered event handlers.
	 *
	 * @return
	 *	The message received from the server.
	 */
	@Override
	public String processInboundMessage(CNConnection connection, String message) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		// Receive message...
		if(message == null)
			return null; // Something bad happened. :(

		// Translate message...
		CNEvent event = this.translator.translate(connection, message);
		if(event == null) event = new UnknownInboundEvent(connection, message);

		// Dispatch event...
		Dispatcher dispatcher;
		for(Class<?> event_class = event.getClass(); event_class != null; event_class = event_class.getSuperclass())
			if((dispatcher = this.dispatchers.get(event_class)) != null)
				dispatcher.dispatchEvent(event);

		// Return message...
		return message;
	}

	/**
	 * Processes the specified message as an event. This method should only be called when the
	 * EventDispatcher is not registered as a preprocessor.
	 * <p/>
	 * NOTE: Unlike the preprocessing step, if the message cannot be translated to a known event,
	 * this method will not dispatch an event.
	 *
	 * @param connection
	 *	The connection the message was received on.
	 *
	 * @param message
	 *	The ChatNet message to process.
	 */
	public void processMessage(CNConnection connection, String message) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		if(message == null)
			return; // Nothing to process.

		// Translate message...
		CNEvent event = this.translator.translate(connection, message);
		if(event == null)
			return; // Nothing to dispatch.

		// Dispatch event...
		Dispatcher dispatcher;
		for(Class<?> event_class = event.getClass(); event_class != null; event_class = event_class.getSuperclass())
			if((dispatcher = this.dispatchers.get(event_class)) != null)
				dispatcher.dispatchEvent(event);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}