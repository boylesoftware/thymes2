package org.bsworks.x2.services.persistence.impl.jdbc;


/**
 * Result set status.
 *
 * @author Lev Himmelfarb
 */
enum ResultSetStatus {

	/**
	 * The current result set row is the row used for the current context.
	 */
	ON_LAST_ROW,

	/**
	 * The current result set row is the row next after the one used to for
	 * the current context.
	 */
	ON_NEXT_ROW,

	/**
	 * The result set end reached.
	 */
	AFTER_LAST
}
