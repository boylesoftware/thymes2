package org.bsworks.x2.resource;


/**
 * Persistent resource reference property handler.
 *
 * @author Lev Himmelfarb
 */
public interface RefPropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get reference target resource class.
	 *
	 * @return The referred persistent resource class. For a transient property
	 * may return a superclass or {@code null} if the reference target is a
	 * wildcard.
	 */
	Class<?> getReferredResourceClass();
}
