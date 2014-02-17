package com.cericlabs.jcnlib.util.arena;

import java.util.*;
import java.util.regex.*;

import com.cericlabs.jcnlib.*;
import com.cericlabs.jcnlib.events.InboundCNEvent;
import com.cericlabs.jcnlib.util.preprocessor.MessagePreprocessor;



/**
 * The ArenaManager is an event handler that stores player data for players currently in the arena,
 * such as ship, frequency and user-defined data.
 * <p/>
 * This class supports the use of generics. When declaring the class, the types specified are used
 * to create PlayerData instances with the same types.
 *
 * @author Chris "Ceiu" Rog
 */
public class ArenaManager<K, V> implements
	InboundCNEvent.Handler,
	MessagePreprocessor {

	private static final Pattern SPLIT_REGEX = Pattern.compile(":");

	/**
	 * Interface used to obtain a view of players.
	 */
	public static interface PlayerFilter<K, V> {
		/**
		 * Checks if the player should be included in the view.
		 *
		 * @param player
		 *	PlayerData representing a player tracked by the ArenaManager. They may or may not be
		 *	active in the current arena, depending on the configuration of the ArenaManager.
		 *
		 * @return
		 *	True if the player should be included in the view, false otherwise.
		 */
		public boolean checkPlayer(PlayerData<K, V> player);
	}

	/**
	 * Simple player filter implementation which filters all inactive players.
	 * Implementation note: This cannot be declared static because it uses the types of the
	 * ArenaManager as part of the declaration. Technically this could be declared without the use of
	 * generics, but it'd be unsafe and cause compiler warnings and junk.
	 */
	private static final PlayerFilter ACTIVE_PLAYER_FILTER = new PlayerFilter() {
		public boolean checkPlayer(PlayerData player) {
			return (player != null) && player.isActive();
		}
	};

////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, PlayerData<K, V>> player_data;
	private int player_count;		// Number of active players in the arena.

	private String username;		// The connection's username.
	private String arena;			// The arena we're currently in.
	private String queued_remove;	// A player to remove on the next event.

	private boolean retain_pd;		// Whether or not we're retaining player data for players who
									// have left the arena.

// Constructors
////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new ArenaManager.
	 */
	public ArenaManager() {
		this(false);
	}

	/**
	 * Creates a new ArenaManager with the specified retention policy.
	 *
	 * @param retain_player_data
	 *	Whether or not player data should be retained when players leave the arena.
	 */
	public ArenaManager(boolean retain_player_data) {
		this.player_data = new HashMap<String, PlayerData<K, V>>();
		this.player_count = 0;

		this.username = null;
		this.arena = null;
		this.queued_remove = null;

		this.retain_pd = retain_player_data;
	}

// Configuration
////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Sets whether or not this ArenaManager will retain player data for players who are no longer
	 * in the arena.
	 *
	 * @param retain_player_data
	 *	Whether or not this ArenaManager should retain player data.
	 */
	public void retainPlayerData(boolean retain_player_data) {
		this.retain_pd = retain_player_data;
	}

// Accessors
////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the name of the arena the attached connection is currently in.
	 *
	 * @return
	 *	The name of the current arena.
	 */
	public synchronized String getArenaName() {
		return this.arena;
	}

	/**
	 * Returns the number of players currently in the arena.
	 *
	 * @return
	 *	The number of players currently in the arena.
	 */
	public synchronized int getPlayerCount() {
		return this.player_count;
	}

	/**
	 * Returns the current username the connection is using or has been assigned by the server. For
	 * this method to work properly, this ArenaManager had to be registered with its connection
	 * before the start of the current session. In the event this information is unavailable, this
	 * method will return null.
	 *
	 * @return
	 *	The name of the user the connection is using, or null if the information is unavailable.
	 */
	public synchronized String getUsername() {
		return this.username;
	}

	/**
	 * Returns the player data associated with the current user. Like getUsername, this method can
	 * only function if and only if this ArenaManager was created before the start of its
	 * connection's current session.
	 *
	 * @return
	 *	The player data associated with the current user, or null if the information is
	 *	unavailable.
	 */
	public synchronized PlayerData<K, V> getPlayer() {
		return this.getPlayer(this.username);
	}

	/**
	 * Returns the player data associated with the player using the specified player name.
	 *
	 * @param player_name
	 *	The name of the player whose player data should be retrieved.
	 *
	 * @throws IllegalArgumentException
	 *	if the player_name parameter is null.
	 *
	 * @return
	 *	The player matching the specified player name, or null of the player is not active in the
	 *	current arena.
	 */
	public synchronized PlayerData<K, V> getPlayer(String player_name) {
		if(player_name == null)
			throw new IllegalArgumentException("player name");

		return this.player_data.get(player_name.toLowerCase());
	}

	/**
	 * Returns the first player with a username that matches the specified regular expression.
	 *
	 * @param pattern
	 *	A formatted regular expression used to filter players. Cannot be null.
	 *
	 * @param argv
	 *	An optional list of arguments to be used to format the pattern.
	 *
	 * @throws IllegalArgumentException
	 *	if the pattern parameter is null.
	 *
	 * @return
	 *	The first player with a matching username, or null if no match could be made.
	 */
	public synchronized PlayerData<K, V> findPlayer(String pattern, Object... argv) {
		if(pattern == null)
			throw new IllegalArgumentException("pattern");

		if(argv.length > 0)
			pattern = String.format(pattern, argv);

		Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

		for(PlayerData<K, V> pdata : this.player_data.values())
			if(regex.matcher(pdata.getPlayerName()).matches())
				return pdata;

		return null;
	}

	/**
	 * Returns a list of active players who match the criteria specified by the given filter. If no
	 * matching players are found, this method returns an empty list. That is, this method will
	 * never return null.
	 * <p/>
	 * Note: The list returned is not backed by this object. Changes made to the list are not
	 * reflected by this ArenaManager and vice versa.
	 *
	 * @param filter
	 *	A PlayerFilter instance to use to filter the list of active players.
	 *
	 * @throws IllegalArgumentException
	 *	if filter is null.
	 *
	 * @return
	 *	A list of active players matching the criteria specified by the given player filter.
	 */
	public synchronized List<PlayerData<K, V>> getPlayers(PlayerFilter<K, V> filter) {
		if(filter == null)
			throw new IllegalArgumentException("filter");

		LinkedList<PlayerData<K, V>> plist = new LinkedList<PlayerData<K, V>>();
		for(PlayerData<K, V> pdata : this.player_data.values())
			if(filter.checkPlayer(pdata))
				plist.add(pdata);

		return plist;
	}

	/**
	 * Returns a list containing every player currently in the arena. The list is not linked to the
	 * data contained by this ArenaManager. If a player leaves the arena, that will not be
	 * reflected by any list returned by this method.
	 *
	 * @return
	 *	A list of players currently in the arena.
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<PlayerData<K, V>> getPlayers() {
		return this.getPlayers(ACTIVE_PLAYER_FILTER);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles inbound ChatNet events. This method simply redirects the request to the
	 * processMessage method.
	 *
	 * @param event
	 *	The event to process.
	 */
	@Override
	public void handleEvent(InboundCNEvent event) {
		if(event == null) return;

		this.processMessage(event.data);
	}

	/**
	 * Processes inbound ChatNet messages. This method simply redirects the message to the
	 * processMessage method.
	 *
	 * @param connection
	 *	The connection the message was received on.
	 *
	 * @param message
	 *	The message to process.
	 *
	 * @return
	 *	The message as it was received.
	 */
	@Override
	public String processInboundMessage(CNConnection connection, String message) {
		this.processMessage(message);
		return message;
	}

	/**
	 * Returns the outbound message unmodified. This method does nothing and only exists due to
	 * the specification of the interface.
	 *
	 * @param connection
	 *	The connection the message was received on.
	 *
	 * @param message
	 *	The message to process.
	 *
	 * @return
	 *	The message as it was received.
	 */
	@Override
	public String processOutboundMessage(CNConnection connection, String message) {
		return message;
	}

	/**
	 * Processes the specified message and updates the arena data as necessary.
	 * <p/>
	 * This method should only be called in situations where the arena manager is not registered
	 * to an event dispatcher, or otherwise would not receive the message.
	 *
	 * @param message
	 *	The message to process.
	 */
	public synchronized void processMessage(String message) {
		if(message == null || message.length() == 0)
			return; // No message to process

		// Split message...
		String[] chunklets = SPLIT_REGEX.split(message);
		PlayerData<K, V> pdata = null;

		// Perform our queued player removal if necessary...
		if(this.queued_remove != null) {
			this.player_data.remove(this.queued_remove);
			this.queued_remove = null;
		}

		// Event handlers...
		String target;

		switch(MessageType.translate(chunklets[0])) {
			case LOGINOK:
				this.username = chunklets[1];
				break;

			case INARENA:
				// Get the current arena
				this.arena = chunklets[1];

				// Set existing players as inactive...
				if(this.retain_pd) {
					for(PlayerData<K, V> pd : this.player_data.values())
						pd.setActive(false);
				} else {
					this.player_data.clear();
				}

				// Create PlayerData for the bot's username (or activate it if it's been retained).
				if(this.username != null) {
					target = this.username.toLowerCase();

					if((pdata = this.player_data.get(target)) == null) {
						this.player_data.put(target, (pdata = new PlayerData<K, V>(this.username, Integer.parseInt(chunklets[2]), Ship.SPECTATOR, true)));
					} else {
						pdata.setFrequency(Integer.parseInt(chunklets[2]));
						pdata.setShip(Ship.SPECTATOR);

						pdata.setActive(true);
					}
				}

				// Reset player count
				this.player_count = 1;
				break;

			case PLAYER:
			case ENTERING:
				// Determine target...
				target = chunklets[1].toLowerCase();

				// Create new player data, or update existing instance.
				if((pdata = this.retain_pd ? this.player_data.get(target) : null) == null) {
					this.player_data.put(target, (pdata = new PlayerData<K, V>(chunklets)));
				} else {
					pdata.setShip(Ship.translate(Integer.parseInt(chunklets[2])));
					pdata.setFrequency(Integer.parseInt(chunklets[3]));
					pdata.setActive(true);
				}

				// Increment player count...
				++this.player_count;
				break;

			case LEAVING:
				// Determine target...
				target = chunklets[1].toLowerCase();

				// Update player data...
				if((pdata = this.player_data.get(target)) != null) {
					// Set player state as inactive...
					pdata.setActive(false);

					// Schedual removal if we're not retaining the player data...
					if(!this.retain_pd)
						this.queued_remove = target;

					// Decrement player count...
					--this.player_count;
				}
				break;

			case SHIPFREQCHANGE:
				// Determine target...
				target = chunklets[1].toLowerCase();

				// Update player data...
				if((pdata = this.player_data.get(target)) != null) {
					pdata.setShip(Ship.translate(Integer.parseInt(chunklets[2])));
					pdata.setFrequency(Integer.parseInt(chunklets[3]));
				}
				break;

			default:
				// Do nothing.
		}

		// Return!
	}

////////////////////////////////////////////////////////////////////////////////////////////////////
}