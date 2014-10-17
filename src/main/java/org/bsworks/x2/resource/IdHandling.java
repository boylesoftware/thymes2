package org.bsworks.x2.resource;


/**
 * Persistent resource record id handling.
 *
 * @author Lev Himmelfarb
 */
public enum IdHandling {

	/**
	 * The id is assigned by the application.
	 */
	ASSIGNED,

	/**
	 * The id is generated automatically in the persistent storage.
	 */
	AUTO_GENERATED
}
