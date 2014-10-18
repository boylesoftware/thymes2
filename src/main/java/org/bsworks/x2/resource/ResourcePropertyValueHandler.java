package org.bsworks.x2.resource;

import java.io.IOException;

import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Handler for a resource property value.
 *
 * @author Lev Himmelfarb
 */
public interface ResourcePropertyValueHandler {

	/**
	 * Tell if the property values can be represented as strings, in which case
	 * {@link #toString(Object)} and {@link #valueOf(String)} methods can be
	 * used.
	 *
	 * @return {@code true} if has string representation.
	 */
	boolean hasStringRepresentation();

	/**
	 * Create string representation of a property value. The returned value can
	 * be fed to {@link #valueOf(String)} to get the original value.
	 *
	 * <p>Note, that collection and map property value handlers assume that the
	 * value is for a single element.
	 *
	 * @param val The value. May be {@code null}.
	 *
	 * @return The value string representation, or {@code null} if the input
	 * value is {@code null}.
	 *
	 * @throws UnsupportedOperationException If the property values cannot be
	 * represented as strings. Use {@link #hasStringRepresentation()} to check.
	 */
	String toString(Object val);

	/**
	 * Parse string representation of a property value.
	 *
	 * <p>Note, that collection and map property value handlers assume that the
	 * string is for a single element value.
	 *
	 * @param str Property value string representation. May be {@code null}.
	 *
	 * @return The value, or {@code null} if the string is {@code null}.
	 *
	 * @throws UnsupportedOperationException If the property values cannot be
	 * represented as strings. Use {@link #hasStringRepresentation()} to check.
	 * @throws InvalidResourceDataException If the specified string
	 * representation is invalid.
	 */
	Object valueOf(String str)
		throws InvalidResourceDataException;

	/**
	 * Get value class. Collection and map property value handlers return value
	 * class of the elements.
	 *
	 * @return The value class.
	 */
	Class<?> getValueClass();

	/**
	 * Get persistent value type corresponding to the property value type.
	 *
	 * <p>Note, that collection and map property value handlers return value
	 * type of the elements. Reference properties return value type of the
	 * target resource id property.
	 *
	 * @return The value type.
	 */
	PersistentValueType getPersistentValueType();

	/**
	 * Tell if the value is a reference.
	 *
	 * @return {@code true} if reference.
	 */
	boolean isRef();

	/**
	 * For a reference value handler, get reference target persistent resource
	 * class.
	 *
	 * @return Reference target persistent resource class, persistent resource
	 * superclass if a bound wildcard, or {@code null} if unbound wildcard.
	 *
	 * @throws UnsupportedOperationException If not a reference value handler.
	 */
	Class<?> getRefTargetClass();

	/**
	 * Write the specified property value using the specified resource write
	 * session.
	 *
	 * @param access Type of access that results in writing the value.
	 * @param val The value to write. Never {@code null}.
	 * @param out The resource write session.
	 *
	 * @throws IOException If an I/O error happens writing the value to the
	 * resource write session's output.
	 */
	void writeValue(ResourcePropertyAccess access, Object val,
			ResourceWriteSession out)
		throws IOException;

	/**
	 * Read property value using the specified resource read session.
	 *
	 * @param access Type of access that results in reading the value.
	 * @param in The resource read session.
	 *
	 * @return The property value. May be {@code null}.
	 *
	 * @throws InvalidResourceDataException If the resource read session's input
	 * contains invalid data.
	 * @throws IOException If an I/O error happens reading the value from the
	 * resource read session's input.
	 */
	Object readValue(ResourcePropertyAccess access, ResourceReadSession in)
		throws InvalidResourceDataException, IOException;
}
