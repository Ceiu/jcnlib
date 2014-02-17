package com.cericlabs.jcnlib;

/**
 * Enum containing values representing the types of chatnet messages that are supported
 * by jcnlib.
 */
public enum MessageType {
	UNKNOWN,

	// Inbound events...
	ENTERING,
	INARENA,
	KILL,
	LEAVING,
	LOGINBAD,
	LOGINOK,
	MSG,
	PLAYER,
	SHIPFREQCHANGE,

	// Outbound events...
	CHANGEFREQ,
	GO,
	SEND,

	// Misc...
	NOOP;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Translates the type, as received from a chatnet message, to a MessageType enum.
	 *
	 * @param message_type
	 *	The type, as received, to translate.
	 *
	 * @throws IllegalArgumentException
	 *	If type is null.
	 *
	 * @return
	 *	An MessageType value representing the type passed to this function.
	 */
	public static MessageType translate(String message_type) {
		try {
			return MessageType.valueOf(MessageType.class, message_type);
		} catch(IllegalArgumentException e) {
			return MessageType.UNKNOWN;
		}
	}
}