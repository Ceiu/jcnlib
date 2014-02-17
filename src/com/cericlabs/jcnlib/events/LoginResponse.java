package com.cericlabs.jcnlib.events;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.*;

/**
 * The LoginResponse event is fired whenever the server responds to a login request sent by the
 * client. Applications should use this event to determine whether or not the login was successful.
 */
public class LoginResponse extends InboundCNEvent {

	public static interface Handler extends CNEventHandler {
		public void handleEvent(LoginResponse event);
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

			if(!(event instanceof LoginResponse))
				return;

			for(Handler handler : this.handlers)
				handler.handleEvent((LoginResponse)event);
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final boolean accepted;	// If the login request was successful
	private final String aux_data;	// The failure message or username.

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new LoginResponse event from the specified chatnet message.
	 *
	 * @param message
	 *	The chatnet message to use to build this event.
	 */
	public LoginResponse(CNConnection connection, String message) {
		super(connection, message);

		String[] chunklets = message.split(":", 2);

		if(chunklets.length != 2)
			throw new IllegalArgumentException("message");

		if(!chunklets[0].toUpperCase().startsWith("LOGIN"))
			throw new IllegalArgumentException("message");

		this.accepted = chunklets[0].equals("LOGINOK");
		this.aux_data = chunklets[1];
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks whether or not the login request was accepted.
	 *
	 * @return
	 *	True if the login request was accepted; false otherwise.
	 */
	public boolean accepted() {
		return this.accepted;
	}

	/**
	 * Returns the username the server has assigned the client. In most cases this will be the
	 * username sent during the login request, but could be changed by the server for a number of
	 * reasons (ie: biller being offline causes the server to prepend a carrot to usernames).
	 *
	 * @return
	 *	The username assigned to the client or null if the login request was denied.
	 */
	public String getUsername() {
		return this.accepted ? this.aux_data : null;
	}

	/**
	 * Returns the server response for a denied login request.
	 *
	 * @return
	 *	The server response for a bad login, or null if the login request was accepted.
	 */
	public String getResponse() {
		return this.accepted ? null : this.aux_data;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}