package org.bsworks.x2.resource;


/**
 * Dependent persistent resource reference property handler.
 *
 * @author Lev Himmelfarb
 */
public interface DependentRefPropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get target dependent resource class.
	 *
	 * @return Dependent persistent resource class. Always a concrete class
	 * representing a persistent resource.
	 */
	Class<?> getReferredResourceClass();

	/**
	 * Get name of the property in the dependent resource class that points back
	 * to the resource that contains the property.
	 *
	 * @return Name of the property. The property is always a single-valued,
	 * non-transient {@link Ref} property with the target type being the
	 * resource that contains the handled property and not a wildcard.
	 */
	String getReverseRefPropertyName();
}
