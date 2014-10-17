package org.bsworks.x2.resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.bsworks.x2.Actor;


/**
 * Resource read session, which is the context of an action of read the resource
 * instance from some external form.
 *
 * @author Lev Himmelfarb
 */
public interface ResourceReadSession {

	/**
	 * Get actor that is providing the resource data.
	 *
	 * @return The actor, or {@code null} if unauthenticated.
	 */
	Actor getActor();

	/**
	 * Convert string representation to the corresponding date object.
	 *
	 * @param dateStr String representation of a date.
	 *
	 * @return Corresponding date object.
	 *
	 * @throws InvalidResourceDataException If the specified date string
	 * representation is invalid.
	 */
	Date stringToDate(String dateStr)
		throws InvalidResourceDataException;

	/**
	 * Get session cache.
	 *
	 * @return Session cache instance, or {@code null} if the session does not
	 * use caching.
	 */
	ResourceReadSessionCache getSessionCache();

	/**
	 * Get next resource property name from the session's input.
	 *
	 * @return The property name, or {@code null} if end of the object.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	String nextProperty()
		throws InvalidResourceDataException, IOException;

	/**
	 * Swallow current property value.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	void swallowValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Enter reading an object property value.
	 *
	 * @return {@code true} if ready to read the object properties,
	 * {@code false} if the object property value is {@code null}.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	boolean enterObject()
		throws InvalidResourceDataException, IOException;

	/**
	 * Enter reading a collection property value.
	 *
	 * @return {@code true} if ready to read the collection values,
	 * {@code false} if the collection property value is {@code null}.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	boolean enterCollection()
		throws InvalidResourceDataException, IOException;

	/**
	 * Enter reading a map property value.
	 *
	 * @return {@code true} if ready to read the map entries,
	 * {@code false} if the map property value is {@code null}.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	boolean enterMap()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read polymorphic object property value type.
	 *
	 * @param typePropHandler Handler of the polymorphic object value type
	 * property.
	 *
	 * @return The value type.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	String readObjectType(TypePropertyHandler typePropHandler)
		throws InvalidResourceDataException, IOException;

	/**
	 * Read next map entry key.
	 *
	 * @return The key value as a string, or {@code null} if no more entries in
	 * the map.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	String readKey()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read string property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	String readStringValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read byte property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Byte readByteValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read short property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Short readShortValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read integer property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Integer readIntegerValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read long property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Long readLongValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read Boolean property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Boolean readBooleanValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read float property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Float readFloatValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read double property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Double readDoubleValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read BigDecimal property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	BigDecimal readBigDecimalValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read enumeration property value.
	 *
	 * @param enumClass Enumeration class.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	<E extends Enum<E>> E readEnumValue(Class<E> enumClass)
		throws InvalidResourceDataException, IOException;

	/**
	 * Read date property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Date readDateValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Read persistent resource reference property value.
	 *
	 * @return The property value, or {@code null} if the property value was
	 * {@code null} or end of current collection value reached.
	 *
	 * @throws InvalidResourceDataException If the session's input contains
	 * unexpected or invalid data.
	 * @throws IOException If an I/O error happens reading data from the
	 * session's input.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	Ref<?> readRefValue()
		throws InvalidResourceDataException, IOException;

	/**
	 * Tell if the last {@code null} value meant end of the current collection.
	 *
	 * @return {@code true} if was end of the current collection.
	 */
	boolean wasCollectionEnd();
}
