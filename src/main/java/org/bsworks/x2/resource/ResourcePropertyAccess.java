package org.bsworks.x2.resource;


/**
 * Application resource access type. Applicable to resource properties and
 * persistent resources as a whole.
 *
 * @author Lev Himmelfarb
 */
public enum ResourcePropertyAccess {

	/**
	 * See property values or persistent resource records via the application
	 * API.
	 */
	SEE,

	/**
	 * Submit property values or persistent resource records via the application
	 * API.
	 */
	SUBMIT,

	/**
	 * Load persistent property values or persistent resource records from the
	 * persistent storage.
	 */
	LOAD,

	/**
	 * Save persistent property values in a new persistent resource record or
	 * create new records of the persistent resource.
	 */
	PERSIST,

	/**
	 * Modify persistent property value in an existing persistent resource
	 * record or modify persistent resource records in general.
	 */
	UPDATE,

	/**
	 * Delete property records (for properties that are stored in their own
	 * persistent collections) or delete persistent resource records.
	 */
	DELETE
}
