package org.bsworks.x2.services.persistence;


/**
 * Persistent resource records lock type.
 *
 * @author Lev Himmelfarb
 */
public enum LockType {

	/**
	 * Other transactions can read the records, but are not allowed to modify or
	 * delete them.
	 */
	SHARED,

	/**
	 * Other transactions are not allowed any access to the records, including
	 * reading them.
	 */
	EXCLUSIVE
}
