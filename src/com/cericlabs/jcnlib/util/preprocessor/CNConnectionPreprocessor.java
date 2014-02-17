package com.cericlabs.jcnlib.util.preprocessor;

import java.util.*;

import java.util.concurrent.CopyOnWriteArrayList;

import com.cericlabs.jcnlib.CNConnection;




/**
 * The CNConnectionPreprocessor decorator adds message preprocessing to a connection.
 *
 * @author Chris "Ceiu" Rog
 */
public class CNConnectionPreprocessor extends CNConnection {

	private final List<MessagePreprocessor> preprocessors;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new CNConnectionPreprocessor that preprocesses messages sent or received on the
	 * specified base connection.
	 *
	 * @param connection
	 *	The base connection to add preprocessing to.
	 */
	public CNConnectionPreprocessor(CNConnection connection) {
		super(connection);

		this.preprocessors = new CopyOnWriteArrayList<MessagePreprocessor>();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Registers the specified message preprocessor with this connection preprocessor.
	 *
	 * @param preprocessor
	 *	The MessagePreprocessor to register with this connection preprocessor.
	 *
	 * @return
	 *	True if the preprocessor was registered successfully; false otherwise.
	 */
	public boolean registerPreprocessor(MessagePreprocessor preprocessor) {
		if(preprocessor == null)
			throw new IllegalArgumentException("preprocessor");

		return !this.preprocessors.contains(preprocessor) && this.preprocessors.add(preprocessor);
	}

	/**
	 * Removes the specified message preprocessor from this connection preprocessor.
	 *
	 * @param preprocessor
	 *	The MessagePreprocessor to remove from this connection preprocessor.
	 *
	 * @return
	 *	True if the preprocessor was removed successfully; false otherwise.
	 */
	public boolean removePreprocessor(MessagePreprocessor preprocessor) {
		if(preprocessor == null)
			throw new IllegalArgumentException("preprocessor");

		return this.preprocessors.remove(preprocessor);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sends a message using this connection after allowing registered preprocessors to process the
	 * message. The final message sent may not be the same as the message passed to this method.
	 * This method will return true if any message was sent after the preprocessors finished.
	 *
	 * @param message
	 *	The ChatNet message to send.
	 *
	 * @return
	 *	True if any message was sent; false otherwise.
	 */
	@Override
	public boolean sendMessage(String message) {
		if(message == null)
			return false;

		for(MessagePreprocessor preprocessor : this.preprocessors)
			if((message = preprocessor.processOutboundMessage(this, message)) == null)
				return false;

		return super.sendMessage(message);
	}

	/**
	 * Retreives the next message from the server, after allowing registered preprocessors to
	 * process the message. The final message returned by this method may not be the message as
	 * received from the server.
	 *
	 * @return
	 *	A ChatNet message or null if an error occured.
	 */
	@Override
	public String receiveMessage() {
		String message = super.receiveMessage();

		if(message == null)
			return null;

		for(MessagePreprocessor preprocessor : this.preprocessors)
			if((message = preprocessor.processInboundMessage(this, message)) == null)
				return null;

		return message;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}