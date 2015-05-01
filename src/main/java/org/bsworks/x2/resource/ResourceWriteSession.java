package org.bsworks.x2.resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.bsworks.x2.Actor;


/**
 * Resource write session, which is the context of an action of writing the
 * resource instance into some external form.
 *
 * @author Lev Himmelfarb
 */
public interface ResourceWriteSession {

	/**
	 * Get actor that is intended to consume the resource data.
	 *
	 * @return The actor, or {@code null} if for public consumption.
	 */
	Actor getActor();

	/**
	 * Tell if the writer should drop null-valued properties and not attempt to
	 * write them to the session. This flag is merely a suggestion to the user
	 * of the session.
	 *
	 * @return {@code true} if the session suggests to the writer to skip
	 * null-valued properties.
	 */
	boolean isNullDropped();

	/**
	 * Convert date object to the corresponding string representation.
	 *
	 * @param date The date object.
	 *
	 * @return String representation of the specified date.
	 */
	String dateToString(Date date);

	/**
	 * Add property to the session's output. Must be followed with
	 * {@link #startObject(Object)}, {@link #startCollection()},
	 * {@link #startMap()}, {@link #writeNullValue()} or one of the
	 * {@code writeValue(xxx)} methods.
	 *
	 * @param propName Property name.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession addProperty(String propName)
		throws IOException;

	/**
	 * Start writing value of an object property.
	 *
	 * @param obj The object.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession startObject(Object obj)
		throws IOException;

	/**
	 * Start writing value of a polymorphic object property.
	 *
	 * @param obj The object.
	 * @param typePropHandler Handler of the polymorphic object value type
	 * property.
	 * @param valueType Value type.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession startObject(Object obj,
			TypePropertyHandler typePropHandler, String valueType)
		throws IOException;

	/**
	 * Finish writing value of an object property.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession endObject()
		throws IOException;

	/**
	 * Start writing value of a collection property.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession startCollection()
		throws IOException;

	/**
	 * Finish writing value of a collection property.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession endCollection()
		throws IOException;

	/**
	 * Start writing value of a map property.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession startMap()
		throws IOException;

	/**
	 * Finish writing value of a map property.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession endMap()
		throws IOException;

	/**
	 * Write next map entry key.
	 *
	 * @param key Map entry key value as a string. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 * @throws NullPointerException If the specified key is {@code null}.
	 */
	ResourceWriteSession writeKey(String key)
		throws IOException;

	/**
	 * Write {@code null} property value.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeNullValue()
		throws IOException;

	/**
	 * Write string property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(String val)
		throws IOException;

	/**
	 * Write byte property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(byte val)
		throws IOException;

	/**
	 * Write short property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(short val)
		throws IOException;

	/**
	 * Write integer property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(int val)
		throws IOException;

	/**
	 * Write long property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(long val)
		throws IOException;

	/**
	 * Write Boolean property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(boolean val)
		throws IOException;

	/**
	 * Write float property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(float val)
		throws IOException;

	/**
	 * Write double property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(double val)
		throws IOException;

	/**
	 * Write BigDecimal property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(BigDecimal val)
		throws IOException;

	/**
	 * Write enumeration property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(Enum<?> val)
		throws IOException;

	/**
	 * Write date property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(Date val)
		throws IOException;

	/**
	 * Write persistent resource reference property value.
	 *
	 * @param val Property value. May not be {@code null}.
	 *
	 * @return This session.
	 *
	 * @throws IOException If an I/O error happens writing data to the session's
	 * output.
	 * @throws IllegalStateException If expecting a different method call.
	 */
	ResourceWriteSession writeValue(Ref<?> val)
		throws IOException;
}
