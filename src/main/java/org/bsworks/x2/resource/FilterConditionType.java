package org.bsworks.x2.resource;


/**
 * Filter condition type.
 *
 * @author Lev Himmelfarb
 */
public enum FilterConditionType {

	/**
	 * Equal.
	 */
	EQ,

	/**
	 * Not equal.
	 */
	NE,

	/**
	 * Less than.
	 */
	LT,

	/**
	 * Less than or equal.
	 */
	LE,

	/**
	 * Greater than.
	 */
	GT,

	/**
	 * Greater than or equal.
	 */
	GE,

	/**
	 * Contains substring matching a regular expression. Case-insensitive.
	 */
	MATCH,

	/**
	 * Does not contain substring matching a regular expression.
	 * Case-insensitive.
	 */
	NOT_MATCH,

	/**
	 * Contains substring matching a regular expression. Case-sensitive.
	 */
	MATCH_CS,

	/**
	 * Does not contain substring matching a regular expression. Case-sensitive.
	 */
	NOT_MATCH_CS,

	/**
	 * Contains substring. Case-insensitive.
	 */
	SUBSTRING,

	/**
	 * Does not contain substring. Case-insensitive.
	 */
	NOT_SUBSTRING,

	/**
	 * Contains substring. Case-sensitive.
	 */
	SUBSTRING_CS,

	/**
	 * Does not contain substring. Case-sensitive.
	 */
	NOT_SUBSTRING_CS,

	/**
	 * Starts with a prefix. Case-insensitive.
	 */
	PREFIX,

	/**
	 * Does not start with a prefix. Case-insensitive.
	 */
	NOT_PREFIX,

	/**
	 * Starts with a prefix. Case-sensitive.
	 */
	PREFIX_CS,

	/**
	 * Does not starts with a prefix. Case-sensitive.
	 */
	NOT_PREFIX_CS,

	/**
	 * Value does not exist.
	 */
	EMPTY,

	/**
	 * Value exists.
	 */
	NOT_EMPTY
}
