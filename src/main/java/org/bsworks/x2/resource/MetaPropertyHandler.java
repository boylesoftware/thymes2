package org.bsworks.x2.resource;


/**
 * Persistent resource record meta-property handler.
 *
 * @author Lev Himmelfarb
 */
public interface MetaPropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get meta-property type.
	 *
	 * @return The meta-property type.
	 */
	MetaPropertyType getType();
}
