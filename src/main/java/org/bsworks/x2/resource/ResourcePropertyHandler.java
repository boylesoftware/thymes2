package org.bsworks.x2.resource;

import org.bsworks.x2.Actor;


/**
 * Resource property handler.
 *
 * @author Lev Himmelfarb
 */
public interface ResourcePropertyHandler {

	/**
	 * Get property name.
	 *
	 * @return Property name.
	 */
	String getName();

	/**
	 * Tell if the specified actor has specified access to the property.
	 *
	 * @param access The access type.
	 * @param actor The actor, or {@code null} if unauthenticated.
	 *
	 * @return {@code true} if access is allowed.
	 */
	boolean isAllowed(ResourcePropertyAccess access, Actor actor);

	/**
	 * Get property value handler.
	 *
	 * @return Property value handler.
	 */
	ResourcePropertyValueHandler getValueHandler();

	/**
	 * If the property is a map, get value handler for the map keys.
	 *
	 * @return Map key value handler, or {@code null} if not a map property.
	 */
	ResourcePropertyValueHandler getKeyValueHandler();

	/**
	 * Tell if the property value can be read using {@link #getValue(Object)}
	 * method.
	 *
	 * @return {@code true} if can be read.
	 */
	boolean isGettable();

	/**
	 * Get property value.
	 *
	 * @param obj The property container object, which is the resource instance
	 * of one of its nested objects. Never {@code null}.
	 *
	 * @return The property value.
	 *
	 * @throws UnsupportedOperationException If property value cannot be read.
	 * Use {@link #isGettable()} to check.
	 */
	Object getValue(Object obj);

	/**
	 * Tell if the property value can be written using
	 * {@link #setValue(Object, Object)} method.
	 *
	 * @return {@code true} if can be written.
	 */
	boolean isSettable();

	/**
	 * Set property value.
	 *
	 * @param obj The property container object, which is the resource instance
	 * of one of its nested objects. Never {@code null}.
	 * @param val The new value.
	 *
	 * @throws UnsupportedOperationException If property value cannot be
	 * written. Use {@link #isSettable()} to check.
	 */
	void setValue(Object obj, Object val);

	/**
	 * Tell if the property is single-valued, or a collection or a map.
	 *
	 * @return {@code true} if single-valued.
	 */
	boolean isSingleValued();

	/**
	 * Get property persistence descriptor.
	 *
	 * @return The persistence descriptor, or {@code null} if the property is
	 * not persistent.
	 */
	ResourcePropertyPersistence getPersistence();

	/**
	 * Tell if during an existing persistent record update operation, the
	 * property value needs to be set to {@code null} if it is {@code null} in
	 * the incoming data. If this method returns {@code false} and in the
	 * incoming new record data the property is {@code null}, the property value
	 * of the existing record is left unchanged.
	 *
	 * @return {@code true} to set to {@code null}, {@code false} if ignore and
	 * leave unchanged.
	 */
	boolean updateIfNull();
}
