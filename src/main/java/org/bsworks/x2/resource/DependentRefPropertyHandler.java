package org.bsworks.x2.resource;


/**
 * Dependent persistent resource reference property handler.
 *
 * @author Lev Himmelfarb
 */
public interface DependentRefPropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get reference target resource class.
	 *
	 * @return Referred persistent resource class. Always a concrete class
	 * representing a persistent resource.
	 */
	Class<?> getReferredResourceClass();

	/**
	 * Get name of the property in the referred dependent resource class that
	 * points back to this resource.
	 *
	 * @return Name of the property. The property is always a single-valued,
	 * non-transient {@link Ref} property with the target type being this
	 * resource and not a wildcard.
	 */
	String getReverseRefPropertyName();
}
