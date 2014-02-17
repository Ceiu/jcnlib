package com.cericlabs.jcnlib;



/**
 * The StateListener interface defines a method for receiving notification that the state of a
 * CNConnection has changed.
 * <p/>
 * Classes implementing this interface must be registered with the CNConnection instance for which
 * they want to receive state change notifications.
 */
public interface StateListener {

	/**
	 * Called whenever the state of a CNConnection instance has changed. This will only be called
	 * by CNConnections this StateListener has been registered with.
	 *
	 * @param connection
	 *	The connection who's state has changed.
	 *
	 * @param state
	 *	The new state of the connection.
	 */
	public void handleStateChange(CNConnection connection, CNConnection.State state);

}