package org.bsworks.x2.resource;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;


/**
 * Resource properties container, such as a resource or a nested object.
 *
 * @author Lev Himmelfarb
 */
public interface ResourcePropertiesContainer {

	/**
	 * Get persistent property handlers chain.
	 * 
	 * <p>An exception is made for the {@link TypePropertyHandler} of a
	 * persistent polymorphic nested object property - it does not have to be
	 * persistent.
	 *
	 * <p>If a polymorphic object property needs to be accessed, the concrete
	 * type name is used as a pseudo-property between the polymorphic object
	 * property name and the nested property name in the path. In the resulting
	 * chain, both polymorphic object property handler, and the concrete type
	 * property handler are included.
	 *
	 * @param propPath Property path relative to the container. Nested
	 * properties separated with dots.
	 *
	 * @return Unmodifiable chain of property handlers. The first element is
	 * handler for one of this container's property. The last element is handler
	 * for the property identified by the path.
	 *
	 * @throws IllegalArgumentException If the path is invalid or any of the
	 * properties in the chain is not persistent.
	 */
	Deque<? extends ResourcePropertyHandler> getPersistentPropertyChain(
			String propPath);

	/**
	 * Get handlers for all properties in the container.
	 *
	 * @return Unmodifiable map of property handlers by property names.
	 */
	Map<String, ? extends ResourcePropertyHandler> getProperties();

	/**
	 * Get handler for the container record id property.
	 *
	 * @return Record id property handler, or {@code null} if the container does
	 * not define a record id property.
	 */
	IdPropertyHandler getIdProperty();

	/**
	 * Get handlers for all simple properties in the container.
	 *
	 * @return Unmodifiable collection of property handlers.
	 */
	Collection<? extends SimplePropertyHandler> getSimpleProperties();

	/**
	 * Get handlers for all nested object properties in the container.
	 *
	 * @return Unmodifiable collection of property handlers.
	 */
	Collection<? extends ObjectPropertyHandler> getObjectProperties();

	/**
	 * Get handlers for all persistent resource reference properties in the
	 * container.
	 *
	 * @return Unmodifiable collection of property handlers.
	 */
	Collection<? extends RefPropertyHandler> getRefProperties();
}
