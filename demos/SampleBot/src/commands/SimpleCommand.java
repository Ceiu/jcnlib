package samplebot.commands;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.util.commands.*;



public abstract class SimpleCommand implements CommandHandler {

	protected final CNConnection connection;

////////////////////////////////////////////////////////////////////////////////////////////////////

	public SimpleCommand(CNConnection connection) {
		if(connection == null)
			throw new IllegalArgumentException("connection");

		this.connection = connection;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}