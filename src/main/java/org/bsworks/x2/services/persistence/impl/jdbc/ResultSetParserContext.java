package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bsworks.x2.services.persistence.PersistenceException;


/**
 * Result set parser context.
 *
 * @author Lev Himmelfarb
 */
abstract class ResultSetParserContext {

	/**
	 * Reference to the parser.
	 */
	protected final ResultSetParser parser;

	/**
	 * Result set descriptor.
	 */
	protected final ResultSetDescriptor rsd;

	/**
	 * Result set from the result set descriptor.
	 */
	protected final ResultSet rs;

	/**
	 * Parent context, or {@code null} for the top-level context.
	 */
	protected final ResultSetParserContext parentCtx;

	/**
	 * Index of the column containing this context's object key.
	 */
	protected final int keyColInd;


	/**
	 * Create new context.
	 *
	 * @param parser Reference to the parser.
	 * @param rsd Result set descriptor.
	 * @param parentCtx Parent context, or {@code null} for the top-level
	 * context.
	 * @param keyColInd Index of the column containing this context's object
	 * key.
	 */
	protected ResultSetParserContext(final ResultSetParser parser,
			final ResultSetDescriptor rsd,
			final ResultSetParserContext parentCtx, final int keyColInd) {

		this.parser = parser;
		this.rsd = rsd;
		this.rs = rsd.getResultSet();
		this.parentCtx = parentCtx;
		this.keyColInd = keyColInd;
	}


	/**
	 * Get parent context.
	 *
	 * @return The parent context, or {@code null} if top-level resource object
	 * context.
	 */
	ResultSetParserContext getParent() {

		return this.parentCtx;
	}

	/**
	 * Reset the context as new.
	 */
	abstract void reset();

	/**
	 * Get context object key value.
	 *
	 * @return The key value.
	 */
	Object getKey() {

		try {
			return this.rsd.getResultSet().getObject(this.keyColInd);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Get next property name. Supported only for
	 * {@link ObjectResultSetParserContext}.
	 *
	 * @return Property name, or {@code null} if no more properties.
	 *
	 * @throws IllegalStateException If getting next property is not expected.
	 */
	abstract String getNextPropertyName();

	/**
	 * Get index of the result set column, from which the next
	 * {@link #getValue(ResultSetValueReader)} call will read the value.
	 *
	 * @return Column index, or {@code 0} if call to
	 * {@link #getValue(ResultSetValueReader)} is not expected.
	 */
	abstract int getValueColumnIndex();

	/**
	 * Get current value. For an {@link ObjectResultSetParserContext}, gets the
	 * value of the property returned by the last {@link #getNextPropertyName()}
	 * call. For a {@link CollectionResultSetParserContext}, gets the next
	 * collection element value.
	 *
	 * @param reader Value reader.
	 *
	 * @return Property value.
	 *
	 * @throws IllegalStateException If getting current value is not expected.
	 */
	abstract <Y> Y getValue(ResultSetValueReader<Y> reader);

	/**
	 * Get child context for parsing nested object.
	 *
	 * @return The context, or {@code null} if nested object value is
	 * {@code null}.
	 *
	 * @throws IllegalStateException If entering nested object is not expected.
	 */
	abstract ObjectResultSetParserContext enterObject();

	/**
	 * Get polymorphic object type.  Supported only for
	 * {@link ObjectResultSetParserContext}.
	 *
	 * @return Object type.
	 *
	 * @throws IllegalStateException If getting object type is not expected.
	 */
	abstract String getObjectType();

	/**
	 * Get child context for parsing nested collection.
	 *
	 * @return The context, or {@code null} if the nested collection is empty.
	 *
	 * @throws IllegalStateException If entering nested collection is not
	 * expected.
	 */
	abstract CollectionResultSetParserContext enterCollection();

	/**
	 * Get next map element key. Supported only for
	 * {@link CollectionResultSetParserContext}.
	 *
	 * @return Map key value, or {@code null} if no more elements.
	 *
	 * @throws IllegalStateException If getting next map key is not expected.
	 */
	abstract Object getMapKey();

	/**
	 * Notify the context that its child context has just finished its action.
	 *
	 * @param rsStatus Result set status.
	 * @param numRows Number of rows consumed by the child context.
	 */
	abstract void childContextExhausted(ResultSetStatus rsStatus, int numRows);

	/**
	 * Create exception to throw on unexpected call.
	 *
	 * @return The exception to throw.
	 */
	protected final IllegalStateException createIllegalStateException() {

		return new IllegalStateException(
				"Unexpected resource read session call.");
	}
}
