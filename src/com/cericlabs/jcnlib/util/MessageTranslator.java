package com.cericlabs.jcnlib.util;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * The MessageTranslator translates chatnet messages into event objects.
 *
 * @author Chris "Ceiu" Rog
 */
public class MessageTranslator {

	private static final class EventGenerator {
		private final Class<? extends CNEvent> event_class;
		private final Constructor<? extends CNEvent> constructor;

		public EventGenerator(Class<? extends CNEvent> event_class, Constructor<? extends CNEvent> constructor) {
			if(event_class == null)
				throw new IllegalArgumentException("event_class");

			if(constructor == null)
				throw new IllegalArgumentException("constructor");

			this.event_class = event_class;
			this.constructor = constructor;
		}

		public CNEvent generateEvent(CNConnection connection, String message) {
			if(connection == null)
				throw new IllegalArgumentException("connection");

			if(message == null)
				throw new IllegalArgumentException("message");

			try {
				return this.constructor.newInstance(connection, message);
			} catch(InstantiationException e) {
				e.getCause().printStackTrace();
			} catch(Exception e) {
				e.printStackTrace(); // This shouldn't ever happen.
			}

			return null;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, EventGenerator> generators;


////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new MessageTranslator with the default translations.
	 */
	public MessageTranslator() {
		this.generators = new HashMap<String, EventGenerator>();

		// Inbound events
		this.registerTranslation("LOGINBAD", LoginResponse.class);
		this.registerTranslation("LOGINOK", LoginResponse.class);
		this.registerTranslation("INARENA", EnteredArena.class);
		this.registerTranslation("PLAYER", PlayerEntered.class);
		this.registerTranslation("ENTERING", PlayerEntered.class);
		this.registerTranslation("LEAVING", PlayerLeft.class);
		this.registerTranslation("SHIPFREQCHANGE", PlayerUpdate.class);
		this.registerTranslation("KILL", PlayerDeath.class);
		this.registerTranslation("MSG", InboundChatMessage.class);
		//this.registerTranslation("NOOP", null); // Probably unnecessary...

		// Outbound events
		this.registerTranslation("GO", ChangeArena.class);
		this.registerTranslation("CHANGEFREQ", ChangeFrequency.class);
		this.registerTranslation("SEND", OutboundChatMessage.class);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Registers a chatnet command to be translated by the specified class. The class must be a
	 * subclass of the <tt>CNEvent</tt> class and must have a constructor which accepts a
	 * CNConnection and a String, in that order.
	 * <p/>
	 * Whenever a <tt>MessageTranslator</tt> receives a message that starts with the specified
	 * command, a new instance of the specified class will be created and returned.
	 *
	 * @param command
	 *	The chatnet command to translate (ex: SEND, PLAYER, MSG, etc).
	 *
	 * @param event_class
	 *	The class which will translate the message.
	 *
	 * @return
	 *	the previously registered class for the specified command, or <tt>null</tt> if a translator
	 *	had not yet been registered.
	 */
	public boolean registerTranslation(String command, Class<? extends CNEvent> event_class) {
		if(command == null)
			throw new IllegalArgumentException("command");

		if(event_class == null)
			throw new IllegalArgumentException("event_class");

		try {
			Constructor<? extends CNEvent> constructor = event_class.getConstructor(CNConnection.class, String.class);
			if(!Modifier.isPublic(constructor.getModifiers()))
				return false; // No access to the method.

			this.generators.put(command.toUpperCase(), new EventGenerator(event_class, constructor));
			return true;
		} catch(NoSuchMethodException e) {
			// Constructor doesn't exist...
		} catch(SecurityException e) {
			// Shouldn't happen, but if it does, we can't use the constructor anyway...
			if(JCNLib.SHOW_ERRORS) e.printStackTrace();
		}

		return false;
	}

	/**
	 * Removes a chatnet command translation class. The value returned is the class that's
	 * currently registered as the translator for the specified command.
	 *
	 * @param command
	 *	The chatnet command to remove translation for.
	 *
	 * @throws IllegalArgumentException
	 *	if any input parameter is null.
	 *
	 * @return
	 *	True if the class was removed successfully; false otherwise.
	 */
	public boolean removeTranslation(String command) {
		if(command == null)
			throw new IllegalArgumentException();

		return (this.generators.remove(command.toUpperCase()) != null);
	}

	/**
	 * Gets the event class currently associated with the specified command.
	 *
	 * @param command
	 *	The chatnet command for which to retrieve the event class.
	 *
	 * @return
	 *	The currently registered event class, or null if no class is registered for the specified
	 *	command.
	 */
	public Class<? extends CNEvent> getEventClass(String command) {
		if(command == null)
			throw new IllegalArgumentException("command");

		EventGenerator generator = this.generators.get(command.toUpperCase());
		return generator != null ? generator.event_class : null;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an event class based on the message. Returns null if the operation fails or the
	 * message does not have an event class associated with it.
	 *
	 * @param connection
	 *	The connection to associate with the message.
	 *
	 * @param message
	 *	The message to translate.
	 *
	 * @return
	 *	A new instance of a class representing the given message, or null if no translation exists.
	 */
	public CNEvent translate(CNConnection connection, String message) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		if(message == null)
			throw new IllegalArgumentException("message");

		int index = message.indexOf(':');
		EventGenerator generator = this.generators.get(index != -1 ? message.substring(0, index).toUpperCase() : message);

		return generator != null ? generator.generateEvent(connection, message) : null;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}