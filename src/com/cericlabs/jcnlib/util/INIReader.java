package com.cericlabs.jcnlib.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;


/**
 * Reads (not writes) from an ini file. Used for very basic configuration.
 *
 * The entire file is loaded into memory when the reader is created, so it's probably
 * a bad idea to use process large ini files with this class.
 *
 * @author Chris "Ceiu" Rog
 */
public class INIReader {
	/** Section regex */
	private static final Pattern SEC_REGEX = Pattern.compile("\\s*\\[(.+)\\]\\s*");
	/** Key/Value regex */
	private static final Pattern KEY_REGEX = Pattern.compile("\\s*([^;].*?)\\s*=\\s*([^\\s]+)\\s*");


	private Map<String, Map<String, String>> sections;

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new INIReader from the specified file.
	 *
	 * @param file
	 *	The file to read.
	 *
	 * @throws IllegalArgumentException
	 *	If file is null.
	 *
	 * @throws FileNotFoundException
	 *	If the specified file cannot be found, is not a file or cannot be read for any reason.
	 *
	 * @throws IOException
	 *	If something goes horribly wrong while reading the file.
	 */
	public INIReader(File file) throws IOException {
		if(file == null)
			throw new IllegalArgumentException();

		String line;
		Matcher matcher;
		Map<String, String> section = null;
		this.sections = new HashMap<String, Map<String, String>>();

		BufferedReader reader = new BufferedReader(new FileReader(file));
		while((line = reader.readLine()) != null) {
			if((matcher = SEC_REGEX.matcher(line)).matches())
				this.sections.put(matcher.group(1).toLowerCase(), (section = new HashMap<String, String>()));
			else if(section != null && (matcher = KEY_REGEX.matcher(line)).matches())
				section.put(matcher.group(1).toLowerCase(), matcher.group(2));
		}
	}

	/**
	 * Creates an INIReader from the specified file. Identical to simply constructing the object
	 * directly, except this returns null if the file could not be read (rather than throwing an
	 * exception).
	 *
	 * @param file
	 *	The file to read.
	 *
	 * @throws IllegalArgumentException
	 *	If file is null, not a file or not readable.
	 *
	 * @return
	 *	An INIReader if the file was loaded successfully; null otherwise.
	 */
	public static INIReader open(File file) {
		try {
			return new INIReader(file);
		} catch(IOException e) {
			return null;
		}
	}

////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the value stored in the given section and key combination. If either the section or
	 * the key is not defined, this method returns null.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or null if either is not defined.
	 */
	public String get(String section, String key) {
		return this.get(section, key, null);
	}

	/**
	 * Returns the value stored in the given section and key combination. If either the section or
	 * the key is not defined, this method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public String get(String section, String key, String default_value) {
		if(section == null || key == null)
			throw new IllegalArgumentException();

		Map<String, String> s_map = this.sections.get(section.toLowerCase());
		String val = s_map != null ? s_map.get(key.toLowerCase()) : null;

		return val != null ? val : default_value;
	}


	/**
	 * Returns the value stored in the given section and key combination as a boolean value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns false.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or false if the value cannot be found or cast.
	 */
	public boolean getBoolean(String section, String key) {
		return this.getBoolean(section, key, false);
	}

	/**
	 * Returns the value stored in the given section and key combination as a boolean value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public boolean getBoolean(String section, String key, boolean default_value) {
		return Boolean.parseBoolean(this.get(section, key, String.valueOf(default_value)));
	}


	/**
	 * Returns the value stored in the given section and key combination as a byte value. If either
	 * the section or the key is not defined or the value cannot be properly cast, this method
	 * returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public byte getByte(String section, String key) {
		return this.getByte(section, key, (byte)0);
	}

	/**
	 * Returns the value stored in the given section and key combination as a byte value. If either
	 * the section or the key is not defined or the value cannot be properly cast, this method
	 * returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public byte getByte(String section, String key, byte default_value) {
		try {
			return Byte.parseByte(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}

	/**
	 * Returns the value stored in the given section and key combination as a short value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public short getShort(String section, String key) {
		return this.getShort(section, key, (short)0);
	}

	/**
	 * Returns the value stored in the given section and key combination as a short value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public short getShort(String section, String key, short default_value) {
		try {
			return Short.parseShort(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}

	/**
	 * Returns the value stored in the given section and key combination as a integer value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public int getInt(String section, String key) {
		return this.getInt(section, key, 0);
	}

	/**
	 * Returns the value stored in the given section and key combination as an integer value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public int getInt(String section, String key, int default_value) {
		try {
			return Integer.parseInt(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}

	/**
	 * Returns the value stored in the given section and key combination as a long value. If either
	 * the section or the key is not defined or the value cannot be properly cast, this method
	 * returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public long getLong(String section, String key) {
		return this.getLong(section, key, 0);
	}

	/**
	 * Returns the value stored in the given section and key combination as a long value. If either
	 * the section or the key is not defined or the value cannot be properly cast, this method
	 * returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public long getLong(String section, String key, long default_value) {
		try {
			return Long.parseLong(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}

	/**
	 * Returns the value stored in the given section and key combination as a floating point value.
	 * If either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public float getFloat(String section, String key) {
		return this.getFloat(section, key, 0.0F);
	}

	/**
	 * Returns the value stored in the given section and key combination as a floating point value.
	 * If either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public float getFloat(String section, String key, float default_value) {
		try {
			return Float.parseFloat(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}

	/**
	 * Returns the value stored in the given section and key combination as a double value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns 0.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key or 0 if the value cannot be found or cast.
	 */
	public double getDouble(String section, String key) {
		return this.getDouble(section, key, 0.0);
	}

	/**
	 * Returns the value stored in the given section and key combination as a double value. If
	 * either the section or the key is not defined or the value cannot be properly cast, this
	 * method returns the value specified as the default value.
	 *
	 * @param section
	 *	The section the value resides in. Cannot be null.
	 *
	 * @param key
	 *	The key for the value. Cannot be null.
	 *
	 * @param default_value
	 *	The value to return if either the section or the key is not defined.
	 *
	 * @throws IllegalArgumentException
	 *	if either the section or key arguments are null.
	 *
	 * @return
	 *	The value stored in the given section & key, or the value specified as the default value
	 *	if either the section or key is not defined.
	 */
	public double getDouble(String section, String key, double default_value) {
		try {
			return Double.parseDouble(this.get(section, key, null));
		} catch(NumberFormatException e) {
			return default_value;
		}
	}
}