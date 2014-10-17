package org.bsworks.x2.services.persistence;


/**
 * Type of a value stored in the persistent storage.
 *
 * @author Lev Himmelfarb
 */
public enum PersistentValueType {

	/**
	 * {@link String}.
	 */
	STRING,

	/**
	 * {@link Number}.
	 */
	NUMERIC,

	/**
	 * {@link Boolean}.
	 */
	BOOLEAN,

	/**
	 * {@link java.util.Date}.
	 */
	DATE,

	/**
	 * Structured object.
	 *
	 * <p>Note, that not all persistent storage implementations support this
	 * type.
	 */
	OBJECT
}
