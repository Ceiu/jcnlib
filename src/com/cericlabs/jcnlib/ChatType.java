package com.cericlabs.jcnlib;

import java.util.*;

/**
 * Enum containing values representing the types of chat messages that can
 * be received.
 */
public enum ChatType {
	UNKNOWN,

	ARENA,
	CHAT,
	COMMAND,
	FREQUENCY,
	MODERATOR,
	PUBLIC,
	PUBLIC_MACRO,
	PRIVATE,
	PRIVATE_COMMAND,
	SQUAD,
	SYSOP;

////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Map<String, ChatType> TRANSLATOR;

	static {
		TRANSLATOR = new HashMap<String, ChatType>(15, 1);

		TRANSLATOR.put("ARENA", ARENA);
		TRANSLATOR.put("CHAT", CHAT);
		TRANSLATOR.put("CMD", COMMAND);
		TRANSLATOR.put("FREQ", FREQUENCY);
		TRANSLATOR.put("MOD", MODERATOR);
		TRANSLATOR.put("PUB", PUBLIC);
		TRANSLATOR.put("PUBM", PUBLIC_MACRO);
		TRANSLATOR.put("PRIV", PRIVATE);
		TRANSLATOR.put("PRIVCMD", PRIVATE_COMMAND);
		TRANSLATOR.put("SQUAD", SQUAD);
		TRANSLATOR.put("SYSOP", SYSOP);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Translates the type, as received from a chatnet message, to a ChatType enum.
	 *
	 * @param type
	 *	The type, as received, to translate.
	 *
	 * @throws IllegalArgumentException
	 *	If type is null.
	 *
	 * @return
	 *	A ChatType value representing the type passed to this function.
	 */
	public static ChatType translate(String type) {
		if(type == null)
			throw new IllegalArgumentException();

		ChatType ctype = TRANSLATOR.get(type);
		return ctype != null ? ctype : UNKNOWN;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}
