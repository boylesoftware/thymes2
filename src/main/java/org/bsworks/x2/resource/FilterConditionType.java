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
	NOT_EMPTY;


	/**
	 * Get condition type that has inverse effect to this one.
	 *
	 * @return The inverse condition type.
	 */
	public FilterConditionType inverse() {

		switch (this) {
		case EQ:
			return NE;
		case NE:
			return EQ;
		case LT:
			return GE;
		case LE:
			return GT;
		case GT:
			return LE;
		case GE:
			return LT;
		case MATCH:
			return NOT_MATCH;
		case NOT_MATCH:
			return MATCH;
		case MATCH_CS:
			return NOT_MATCH_CS;
		case NOT_MATCH_CS:
			return MATCH_CS;
		case SUBSTRING:
			return NOT_SUBSTRING;
		case NOT_SUBSTRING:
			return SUBSTRING;
		case SUBSTRING_CS:
			return NOT_SUBSTRING_CS;
		case NOT_SUBSTRING_CS:
			return SUBSTRING_CS;
		case PREFIX:
			return NOT_PREFIX;
		case NOT_PREFIX:
			return PREFIX;
		case PREFIX_CS:
			return NOT_PREFIX_CS;
		case NOT_PREFIX_CS:
			return PREFIX_CS;
		case EMPTY:
			return NOT_EMPTY;
		case NOT_EMPTY:
			return EMPTY;
		}

		throw new AssertionError("Unknown filter condition type.");
	}
}
