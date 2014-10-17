package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bsworks.x2.services.persistence.PersistenceException;


/**
 * Object result set parser context.
 *
 * @author Lev Himmelfarb
 */
class ObjectResultSetParserContext
	extends ResultSetParserContext {

	/**
	 * Index of the first column containing this context's object property.
	 */
	private final int firstPropertyColInd;

	/**
	 * Index of the column for the next {@link #getNextPropertyName()} call, or
	 * {@code 0} if the next {@link #getNextPropertyName()} call must return
	 * {@code null}. Negative if {@link #getNextPropertyName()} call is not
	 * expected.
	 */
	private int nextPropertyColInd;

	/**
	 * Index of the column containing value for the next
	 * {@link #getValue(ResultSetValueReader)} call, or {@code 0} if
	 * {@link #getValue(ResultSetValueReader)} call is not expected.
	 */
	private int curValueColInd;

	/**
	 * Child contexts.
	 */
	private final List<ResultSetParserContext> childContexts =
		new ArrayList<>();

	/**
	 * Index of the next child context.
	 */
	private int nextChildContextInd;

	/**
	 * End result set status.
	 */
	private ResultSetStatus endResultSetStatus;

	/**
	 * Number of rows consumed by the context.
	 */
	private int numRows;


	/**
	 * Create new context.
	 *
	 * @param parser Reference to the parser.
	 * @param rsd Result set descriptor.
	 * @param parentCtx Parent context, or {@code null} for the top-level
	 * context.
	 * @param keyColInd Index of the column containing the object key.
	 * @param firstPropertyColInd Index of the first column containing the
	 * context's object property.
	 */
	ObjectResultSetParserContext(final ResultSetParser parser,
			final ResultSetDescriptor rsd,
			final ResultSetParserContext parentCtx, final int keyColInd,
			final int firstPropertyColInd) {
		super(parser, rsd, parentCtx, keyColInd);

		this.firstPropertyColInd = firstPropertyColInd;

		this.reset();
	}


	/**
	 * Tell if the context is not a top-level resource object context.
	 *
	 * @return {@code true} if sub-parser context.
	 */
	boolean isSubParserContext() {

		return (this.firstPropertyColInd > 1);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	void reset() {

		this.nextPropertyColInd = this.firstPropertyColInd;
		this.curValueColInd = 0;
		this.nextChildContextInd = 0;
		this.endResultSetStatus = ResultSetStatus.ON_LAST_ROW;
		this.numRows = 0;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	String getNextPropertyName() {

		final String propName;
		try {
			propName = this.rsd.getPropertyName(this.nextPropertyColInd);
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw this.createIllegalStateException();
		}

		this.curValueColInd = this.nextPropertyColInd;
		this.nextPropertyColInd = -1;

		if (propName == null)
			this.parser.contextExhausted(this.endResultSetStatus, this.numRows,
					false);

		return propName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	int getValueColumnIndex() {

		return this.curValueColInd;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	<Y> Y getValue(final ResultSetValueReader<Y> reader) {

		if (this.curValueColInd == 0)
			throw this.createIllegalStateException();

		final Y val;
		try {
			val = reader.readValue(this.rs, this.curValueColInd);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}

		this.nextPropertyColInd =
			this.rsd.getNextColumnIndex(this.curValueColInd);
		this.curValueColInd = 0;

		return val;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	ObjectResultSetParserContext enterObject() {

		if (this.curValueColInd == 0)
			throw this.createIllegalStateException();

		final ObjectResultSetParserContext childCtx;
		if (this.nextChildContextInd >= this.childContexts.size()) {
			childCtx = new ObjectResultSetParserContext(this.parser, this.rsd,
					this, this.curValueColInd, this.curValueColInd + 1);
			this.childContexts.add(childCtx);
			this.nextChildContextInd++;
		} else {
			childCtx =
				(ObjectResultSetParserContext) this.childContexts.get(
						this.nextChildContextInd++);
			childCtx.reset();
		}

		this.nextPropertyColInd =
			this.rsd.getNextColumnIndex(this.curValueColInd);
		this.curValueColInd = 0;

		if (childCtx.getKey() == null)
			return null;

		return childCtx;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	String getObjectType() {

		try {

			// find the object value type
			int typeColInd;
			String type = null;
			for (typeColInd = this.nextPropertyColInd; typeColInd != 0;
					typeColInd = this.rsd.getNextColumnIndex(typeColInd)) {
				type = this.rs.getString(typeColInd);
				if (type != null)
					break;
			}
			if (type == null)
				throw new PersistenceException("No polymorphic object type.");

			// set up for reading object properties
			this.nextPropertyColInd = typeColInd + 1;

			// return the type
			return type;

		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	CollectionResultSetParserContext enterCollection() {

		if (this.curValueColInd == 0)
			throw this.createIllegalStateException();

		final CollectionResultSetParserContext childCtx;
		if (this.nextChildContextInd >= this.childContexts.size()) {
			childCtx = new CollectionResultSetParserContext(this.parser,
					this.rsd, this, this.curValueColInd);
			this.childContexts.add(childCtx);
			this.nextChildContextInd++;
		} else {
			childCtx =
				(CollectionResultSetParserContext) this.childContexts.get(
						this.nextChildContextInd++);
			childCtx.reset();
		}

		this.nextPropertyColInd =
			this.rsd.getNextColumnIndex(this.curValueColInd);
		this.curValueColInd = 0;

		if (childCtx.getKey() == null)
			return null;

		return childCtx;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	Object getMapKey() {

		// single objects don't have map keys
		throw this.createIllegalStateException();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	void childContextExhausted(final ResultSetStatus rsStatus,
			final int numRows) {

		if (rsStatus != ResultSetStatus.ON_LAST_ROW) {
			if (this.nextPropertyColInd != 0)
				throw new PersistenceException("Collection properties must"
						+ " always be the last in the result set.");
			this.endResultSetStatus = rsStatus;
			this.numRows += numRows;
		}
	}
}
