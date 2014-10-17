package org.bsworks.x2.util;


/**
 * Collection of utility methods for working with strings.
 *
 * @author Lev Himmelfarb
 */
public final class StringUtils {

	/**
	 * All methods are static.
	 */
	private StringUtils() {}


	/**
	 * Convert string to {@code null} if it is empty.
	 *
	 * @param str Input string.
	 *
	 * @return {@code null} if the specified string is {@code null} or empty,
	 * the string itself otherwise.
	 */
	public static String nullIfEmpty(final String str) {

		return ((str == null) || str.isEmpty() ? null : str);
	}

	/**
	 * Returns specified default value if the specified string is empty.
	 *
	 * @param str Input string.
	 * @param def Default value.
	 *
	 * @return The specified default value if the specified string is
	 * {@code null} or empty, the string itself otherwise.
	 */
	public static String defaultIfEmpty(final String str, final String def) {

		return ((str == null) || str.isEmpty() ? def : str);
	}
}
