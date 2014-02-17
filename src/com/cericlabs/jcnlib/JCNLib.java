package com.cericlabs.jcnlib;

import java.util.*;

import com.cericlabs.jcnlib.events.*;


/**
 * Contains global library information and configuration.
 */
public final class JCNLib {

////////////////////////////////////////////////////////////////////////////////////////////////////
// Version Information
////////////////////////////////////////////////////////////////////////////////////////////////////

	public final class Version {
		/** jcnlib version: major version number */
		public static final int MAJOR = @version.major@;

		/** jcnlib version: minor version number */
		public static final int MINOR = @version.minor@;

		/** jcnlib version: release version number */
		public static final int RELEASE = @version.release@;

		/** jcnlib version: build number */
		public static final int BUILD = @version.build@;

		/** jcnlib version: build date */
		public static final String DATE = "@version.date@";
	}


////////////////////////////////////////////////////////////////////////////////////////////////////
// Global Runtime Configuration
////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Whether or not we're displaying internal errors (debugging purposes only) */
	public static boolean SHOW_ERRORS = false;

	/**
	 * How long to wait (in milliseconds) for data before assuming the connection timed out.
	 * Setting this to zero (or negative) will allow the connections to wait indefinitely
	 * for data.
	 */
	public static int CONNECTION_TIMEOUT = 300000;

////////////////////////////////////////////////////////////////////////////////////////////////////

}