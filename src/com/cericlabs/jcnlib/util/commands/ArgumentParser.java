package com.cericlabs.jcnlib.util.commands;

import java.util.*;
import java.util.regex.*;


/**
 * The ArgumentParser provides simple argument parsing. While intended to be used with the
 * CommandManager, the ArgumentParser can be used with any string fitting the format:
 * <pre>-opt -opt "arg" arg</pre>
 * In the above example, options are prefixed with a single dash and may have values
 * associated with them (depending on how they're defined). A final argument may also be present if
 * the parser is configured to use one.
 *
 * @author Chris "Ceiu" Rog
 */
public class ArgumentParser {
	private static final Pattern OPTION_REGEX = Pattern.compile("\\s*(?:[\\-/]([A-Z0-9]+)(?:\\s+|\\z))?(?:\"(.+?)(?!\\\\)\"|([^\\-/]\\S*))?\\s*", Pattern.CASE_INSENSITIVE);

////////////////////////////////////////////////////////////////////////////////////////////////////

	private class CommandOption {
		public final String option;
		public final String arg_pattern;
		public final boolean has_argument;
		public final boolean required;

		public CommandOption(String option, boolean required, boolean has_argument, String arg_pattern) {
			if(option == null)
				throw new IllegalArgumentException("option");

			this.option = option;
			this.arg_pattern = arg_pattern;
			this.has_argument = has_argument;
			this.required = required;
		}
	}

	private final Map<String, CommandOption> options;
	private final Map<String, String> values;

	private boolean require_argument;	// True when we require an argument to exist (Ie: !command -opt optarg arg).
	private boolean allow_extra;		// True when we will silently discard unexpected options.

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an ArgumentParser that does not require an argument and does not allow extraneous
	 * options.
	 */
	public ArgumentParser() {
		this.options = new HashMap<String, CommandOption>();
		this.values = new HashMap<String, String>();

		this.require_argument = false;
		this.allow_extra = false;
	}

	/**
	 * Creates an ArgumentParser with the specified configuration for the required argument and
	 * extraneous options.
	 *
	 * @param require_argument
	 *	Whether or not the parser should require an argument for the command.
	 *
	 * @param allow_extra
	 *	Whether or not the parser should allow extraneous options.
	 */
	public ArgumentParser(boolean require_argument, boolean allow_extra) {
		this.options = new HashMap<String, CommandOption>();
		this.values = new HashMap<String, String>();

		this.require_argument = require_argument;
		this.allow_extra = allow_extra;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds an optional option that does not have an argument.
	 *
	 * @param option
	 *	The name of the option to add.
	 *
	 * @return
	 *	This argument parser.
	 */
	public ArgumentParser addOption(String option) {
		return this.addOption(option, false, false);
	}

	/**
	 * Adds an option that does not have an argument, but may be required.
	 *
	 * @param option
	 *	The name of the option to add.
	 *
	 * @param required
	 *	Whether or not the option is required.
	 *
	 * @return
	 *	This argument parser.
	 */
	public ArgumentParser addOption(String option, boolean required) {
		return this.addOption(option, required, false);
	}

	/**
	 * Adds an option that may have an argument and may be required.
	 *
	 * @param option
	 *	The name of the option to add.
	 *
	 * @param required
	 *	Whether or not the option is required.
	 *
	 * @param has_argument
	 *	Whether or not the option requires an argument with it.
	 *
	 * @return
	 *	This argument parser.
	 */
	public ArgumentParser addOption(String option, boolean required, boolean has_argument) {
		if(option == null)
			throw new IllegalArgumentException("option");

		if(this.options.containsKey(option))
			throw new IllegalStateException("Option already registered.");

		this.options.put(option, new CommandOption(option, required, has_argument, null));

		return this;
	}

	/**
	 * Adds an option that may be optional, but requires an argument with the specified pattern.
	 * The pattern must be a valid regular expression which defines the format for the option's
	 * argument.
	 *
	 * @param option
	 *	The name of the option to add.
	 *
	 * @param arg_pattern
	 *	A regular expression for the option's argument.
	 *
	 * @return
	 *	This argument parser.
	 */
	public ArgumentParser addOption(String option, boolean required, String arg_pattern) {
		if(option == null)
			throw new IllegalArgumentException("option");

		if(this.options.containsKey(option))
			throw new IllegalStateException("Option already registered.");

		if(arg_pattern != null)
			Pattern.compile(arg_pattern); // Throws an exception if the pattern is bad.

		this.options.put(option, new CommandOption(option, required, true, arg_pattern));

		return this;
	}

	/**
	 * Configures this ArgumentParser to require an argument on the command.
	 *
	 * @return
	 *	This argument parser.
	 */
	public ArgumentParser requireArgument() {
		this.require_argument = true;

		return this;
	}

	/**
	 * Sets the argument requirement for this ArgumentParser.
	 *
	 * @return
	 *	This ArgumentParser.
	 */
	public ArgumentParser requireArgument(boolean require_argument) {
		this.require_argument = require_argument;

		return this;
	}

	/**
	 * Configures this ArgumentParser to allow extaneous options.
	 *
	 * @return
	 *	This ArgumentParser.
	 */
	public ArgumentParser allowExtraneousOptions() {
		this.allow_extra = true;

		return this;
	}

	/**
	 * Sets the allowance of extraneous options for this ArgumentParser.
	 *
	 * @return
	 *	This ArgumentParser.
	 */
	public ArgumentParser allowExtraneousOptions(boolean allow_extra) {
		this.allow_extra = allow_extra;

		return this;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses the specified string as a series of arguments. If the given string matches the format
	 * defined by this argument parser, this method will return true. If the string is null, does
	 * not contain all of the required options (or the options are missing required arguments), or
	 * contains extraneous options when not allowed, this method returns false.
	 *
	 * @param arguments
	 *	A string representing the arguments to parse.
	 *
	 * @return
	 *	True if the arguments were parsed from the string successfully; false otherwise.
	 */
	public boolean parse(String arguments) {
		if(arguments == null)
			return false; // If we're not expecting arguments, why are they using a parser?

		// Clear previous values...
		this.values.clear();

		// Get our matcher...
		Matcher opt_matcher = OPTION_REGEX.matcher(arguments);
		int offset = 0;
		int len = arguments.length();

		// Parse!
		while(offset < len && opt_matcher.find(offset)) {
			String option = opt_matcher.group(1);
			String argument;
			int start, end;

			if(opt_matcher.group(2) == null) {
				argument = opt_matcher.group(3);
				start = opt_matcher.start(3);
				end = opt_matcher.end(3);
			} else {
				argument = opt_matcher.group(2);
				start = opt_matcher.start(2);
				end = opt_matcher.end(2);
			}

			if(option != null) {
				CommandOption copt = this.options.get(option);

				if(copt != null) {
					if(copt.has_argument) {
						if(argument == null) {
							offset = opt_matcher.end(1) + 1;
						} else {
							if(copt.arg_pattern == null || argument.matches(copt.arg_pattern))
								this.values.put(option, argument);

							offset = end + 1;
						}
					} else {
						this.values.put(option, null);
						offset = opt_matcher.end(1) + 1;
					}
				} else {
					// Unexpected option.
					if(!this.allow_extra) return false;
					offset = opt_matcher.end(1) + 1;
				}
			} else {
				if(argument != null)
					this.values.put(null, arguments.substring(start));

				break;
			}
		}

		// Verify that we got everything we require...
		for(CommandOption copt : this.options.values())
			if(copt.required && !this.values.containsKey(copt.option))
				return false;

		if(this.require_argument && this.values.get(null) == null)
			return false;

		// Everything checks out...
		return true;
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if the given option was set during the last parse attempt.
	 *
	 * @param option
	 *
	 * @throws IllegalArgumentException
	 *	if option is null.
	 *
	 * @return
	 *	True if the option is set; false otherwise.
	 */
	public boolean isSet(String option) {
		if(option == null)
			throw new IllegalArgumentException("option");

		return this.values.containsKey(option);
	}

	/**
	 * Returns the argument for given option read during the last parse attempt. If the option does
	 * not have an argument, or was not present in the last parse attempt, this method returns
	 * null.
	 *
	 * @param option
	 *
	 * @throws IllegalArgumentException
	 *	if option is null.
	 *
	 * @return
	 *	The argument associated with the specified option.
	 */
	public String getArgument(String option) {
		if(option == null)
			throw new IllegalArgumentException("option");

		return this.values.get(option);
	}

	/**
	 * Returns the command argument for the last parse attempt. If no argument was detected, this
	 * method returns null.
	 *
	 * @return
	 *	The argument associated with the base command.
	 */
	public String getArgument() {
		return this.values.get(null);
	}

}