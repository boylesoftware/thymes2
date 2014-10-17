package org.bsworks.x2.util;


/**
 * Collection of utility methods for working with enumerations.
 *
 * @author Lev Himmelfarb
 */
public final class EnumUtils {

	/**
	 * All methods are static.
	 */
	private EnumUtils() {}


	/**
	 * Get enumeration value give its string representation.
	 *
	 * @param enumClass Enumeration class (must extend {@link Enum}).
	 * @param valStr String value.
	 *
	 * @return Enumeration value.
	 *
	 * @throws IllegalArgumentException If specified enumeration does not
	 * contain the specified value.
	 * @throws ClassCastException If the specified class is not an enumeration.
	 */
	@SuppressWarnings("unchecked")
	public static Object valueOf(final Class<?> enumClass,
			final String valStr) {

		return Enum.valueOf(enumClass.asSubclass(Enum.class), valStr);
	}
}
