package org.bsworks.x2.resource;


/**
 * Persistent record id property handler.
 *
 * @author Lev Himmelfarb
 */
public interface IdPropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get id handling mode.
	 *
	 * @return The id handling mode.
	 */
	IdHandling getHandling();
}
