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
	EQ(false),

	/**
	 * Not equal.
	 */
	NE(false),

	/**
	 * Less than.
	 */
	LT(false),

	/**
	 * Less than or equal.
	 */
	LE(false),

	/**
	 * Greater than.
	 */
	GT(false),

	/**
	 * Greater than or equal.
	 */
	GE(false),

	/**
	 * Contains substring matching a regular expression. Case-insensitive.
	 */
	MATCH(true),

	/**
	 * Does not contain substring matching a regular expression.
	 * Case-insensitive.
	 */
	NOT_MATCH(true),

	/**
	 * Contains substring matching a regular expression. Case-sensitive.
	 */
	MATCH_CS(true),

	/**
	 * Does not contain substring matching a regular expression. Case-sensitive.
	 */
	NOT_MATCH_CS(true),

	/**
	 * Contains substring. Case-insensitive.
	 */
	SUBSTRING(true),

	/**
	 * Does not contain substring. Case-insensitive.
	 */
	NOT_SUBSTRING(true),

	/**
	 * Contains substring. Case-sensitive.
	 */
	SUBSTRING_CS(true),

	/**
	 * Does not contain substring. Case-sensitive.
	 */
	NOT_SUBSTRING_CS(true),

	/**
	 * Starts with a prefix. Case-insensitive.
	 */
	PREFIX(true),

	/**
	 * Does not start with a prefix. Case-insensitive.
	 */
	NOT_PREFIX(true),

	/**
	 * Starts with a prefix. Case-sensitive.
	 */
	PREFIX_CS(true),

	/**
	 * Does not starts with a prefix. Case-sensitive.
	 */
	NOT_PREFIX_CS(true),

	/**
	 * Value does not exist.
	 */
	EMPTY(false),

	/**
	 * Value exists.
	 */
	NOT_EMPTY(false);


	/**
	 * Tells if the condition is string-specific.
	 */
	private final boolean _requiresString;


	/**
	 * Create new value.
	 *
	 * @param requiresString {@code true} if the condition is string-specific.
	 */
	private FilterConditionType(final boolean requiresString) {

		this._requiresString = requiresString;
	}


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

	/**
	 * Tell if the condition is string-specific (that is requires a string
	 * operand(s)).
	 *
	 * @return {@code true} if string-specific.
	 */
	public boolean requiresString() {

		return this._requiresString;
	}
}
