package samplebot;


public class exe {

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The main method contains the main loop which controls the flow of the entire program.
	 */
	public static void main(String[] argv) throws Exception {
		// Debug logging. This should only be enabled if various library methods are returning
		// unexpected values. Enabling debug output may reveal some other issues.
		//JCNLib.SHOW_ERRORS = true;

		// We should do any other system-level configuration here (ie. figure out what directory
		// we're running from if that matters), load configuration files, etc.).

		// Create our bot actions
		// If we need to give it any configuration files, BotActions should be modified to accept
		// such objects as part of its constructor.
		BotActions bot_actions = new BotActions();

		// Run!
		bot_actions.run();
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

}