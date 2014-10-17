package org.bsworks.x2.resource;


/**
 * Application resource handler.
 *
 * @param <R> Handled resource type.
 *
 * @author Lev Himmelfarb
 */
public interface ResourceHandler<R>
	extends ResourcePropertiesContainer {

	/**
	 * Get handled resource class.
	 *
	 * @return The resource class.
	 */
	Class<R> getResourceClass();

	/**
	 * Get value handler for a resource instance as a whole.
	 *
	 * @return The value handler.
	 */
	ResourcePropertyValueHandler getResourceValueHandler();
}
