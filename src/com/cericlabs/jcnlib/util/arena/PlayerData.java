package com.cericlabs.jcnlib.util.arena;

import java.util.*;

import com.cericlabs.jcnlib.Ship;


/**
 * The PlayerData class is used to store player-specific data for a given player while they are
 * in the current arena.
 * <p/>
 * Each instance of this class represents a single session for the given player. If the player
 * leaves the arena (or the current arena changes), the instance is discarded. When a player
 * re-enters the arena, a new instance is created for them, regardless of the existance of any
 * previous instances.
 * <p/>
 * As this class is an extension of the HashMap class, any operation you can perform on a map can
 * be used on instances of this class.
 *
 * @author Chris "Ceiu" Rog
 */
public class PlayerData<K, V> extends HashMap<K, V> {

	/** Serial Version ID -- Used for serialization */
	private static final long serialVersionUID = 4060996793801961440L;

	private final String player_name;

	private int freq;
	private Ship ship;

	private boolean active;


// Constructors
////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Creates a new PlayerData instance with the given initial values. */
	protected PlayerData(String player_name, int freq, Ship ship, boolean active) {
		super(5);

		if(player_name == null || player_name.length() < 1)
			throw new IllegalArgumentException("player_name");

		this.player_name = player_name;

		this.setFrequency(freq);
		this.setShip(ship);

		this.active = active;
	}

	/** Creates a new PlayerData instance from the given pre-split ChatNet message. */
	protected PlayerData(String[] chunklets) {
		super(5);

		if(chunklets == null || chunklets.length < 4)
			throw new IllegalArgumentException("chunklets");

		if(chunklets[1] == null || chunklets[1].length() < 1)
			throw new IllegalArgumentException("chunklets");

		this.player_name = chunklets[1];
		this.ship = Ship.translate(Integer.parseInt(chunklets[2]));
		this.freq = Integer.parseInt(chunklets[3]);

		this.active = true;
	}

// Accessors
////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the player this PlayerData instance represents. This method will never
	 * return null.
	 *
	 * @return
	 *	The name of the player.
	 */
	public String getPlayerName() {
		return this.player_name;
	}

	/**
	 * Returns the frequency the player is currently on or, if the player is no longer in the arena,
	 * the frequency they were last seen on.
	 * <p/>
	 * This method will always return a valid frequency number (0 - 9999, inclusive).
	 *
	 * @return
	 *	The frequency the player is/was on.
	 */
	public int getFrequency() {
		return this.freq;
	}

	/**
	 * Returns the ship this player is currently using or, if the player is no longer in the arena,
	 * the ship they were last seen using.
	 * <p/>
	 * This method will always return a valid ship number (0 - 9, inclusive). 0 represents a Warbird,
	 * 9 represents spectator mode.
	 *
	 *
	 * @return
	 *	The ship the player is/was in.
	 */
	public Ship getShip() {
		return this.ship;
	}

	/**
	 * Checks whether or not this player is in the current arena.
	 *
	 * @return
	 *	True if the player is in the current arena; false otherwise.
	 */
	public boolean isActive() {
		return this.active;
	}

// Mutators
////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Sets the frequency of the player. Frequency value must be an integer between 0 and 9999, inclusively. */
	protected void setFrequency(int freq) {
		if(freq < 0 || freq > 9999)
			throw new IllegalArgumentException("frequency");

		this.freq = freq;
	}

	/** Sets the ship the player is using. */
	protected void setShip(Ship ship) {
		if(ship == null)
			throw new IllegalArgumentException("Ship");

		this.ship = ship;
	}

	/** Sets the state of this player. */
	protected void setActive(boolean active) {
		this.active = active;
	}
}