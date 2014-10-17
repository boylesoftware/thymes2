package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bsworks.x2.services.persistence.PersistenceException;


/**
 * Collection result set parser context.
 *
 * @author Lev Himmelfarb
 */
class CollectionResultSetParserContext
	extends ResultSetParserContext {

	/**
	 * Value column index.
	 */
	private final int valColInd;

	/**
	 * Contexts for the parent keys.
	 */
	private final ResultSetParserContext[] parentKeyCtxs;

	/**
	 * The parent key values.
	 */
	private final Object[] parentKeyValues;

	/**
	 * Tells if the next get value or enter object call is going to be for a
	 * subsequent (not the first) element of the collection.
	 */
	private boolean subsequentRead;

	/**
	 * Result set status left after reading collection element.
	 */
	private ResultSetStatus rsStatus;

	/**
	 * Number of rows consumed by the context.
	 */
	private int numRows;

	/**
	 * Child context, if any.
	 */
	private ObjectResultSetParserContext childContext;


	/**
	 * Create new context.
	 *
	 * @param parser Reference to the parser.
	 * @param rsd Result set descriptor.
	 * @param parentCtx Parent context.
	 * @param keyColInd Index of the column containing the collection's element
	 * object key. The value is assumed to be in the next column.
	 */
	CollectionResultSetParserContext(final ResultSetParser parser,
			final ResultSetDescriptor rsd,
			final ResultSetParserContext parentCtx, final int keyColInd) {
		super(parser, rsd, parentCtx, keyColInd);

		this.valColInd = keyColInd + 1;

		final List<ResultSetParserContext> parentKeyCtxs = new ArrayList<>();
		for (ResultSetParserContext ctx = this.parentCtx; ctx != null;
				ctx = ctx.parentCtx)
			parentKeyCtxs.add(ctx);
		this.parentKeyCtxs = parentKeyCtxs.toArray(
				new ResultSetParserContext[parentKeyCtxs.size()]);
		this.parentKeyValues = new Object[this.parentKeyCtxs.length];

		this.reset();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	void reset() {

		for (int i = 0; i < this.parentKeyCtxs.length; i++)
			this.parentKeyValues[i] = this.parentKeyCtxs[i].getKey();

		this.subsequentRead = false;
		this.rsStatus = ResultSetStatus.ON_LAST_ROW;
		this.numRows = 0;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	String getNextPropertyName() {

		// collections don't have properties
		throw this.createIllegalStateException();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	int getValueColumnIndex() {

		return this.valColInd;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	<Y> Y getValue(final ResultSetValueReader<Y> reader) {

		// check if collection end reached
		if (this.isCollectionEnd(true))
			return null;

		// read and return the value
		try {
			return reader.readValue(this.rs, this.valColInd);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	ObjectResultSetParserContext enterObject() {

		// check if collection end reached
		if (this.isCollectionEnd(true))
			return null;

		// get child context
		if (this.childContext == null)
			this.childContext = new ObjectResultSetParserContext(this.parser,
					this.rsd, this, this.keyColInd, this.valColInd);
		else
			this.childContext.reset();

		// return child context
		return this.childContext;
	}

	/**
	 * Check if collection end is reached.
	 *
	 * @param setSubsequentRead {@code true} to set subsequent read flag.
	 *
	 * @return {@code true} if collection end reached.
	 */
	private boolean isCollectionEnd(final boolean setSubsequentRead) {

		try {

			// advance and check if collection end
			if (this.subsequentRead) {

				// advance and check if next row belongs to the same collection
				ResultSetStatus collectionEndStatus = null;
				switch (this.rsStatus) {
				case ON_LAST_ROW:
					if (this.rs.next()) {
						if (!setSubsequentRead)
							this.rsStatus = ResultSetStatus.ON_NEXT_ROW;
						if (!this.isSameParent())
							collectionEndStatus = ResultSetStatus.ON_NEXT_ROW;
					} else {
						collectionEndStatus = ResultSetStatus.AFTER_LAST;
					}
					this.numRows++;
					break;
				case ON_NEXT_ROW:
					if (!this.isSameParent())
						collectionEndStatus = ResultSetStatus.ON_NEXT_ROW;
					break;
				case AFTER_LAST:
					collectionEndStatus = ResultSetStatus.AFTER_LAST;
					break;
				default: // cannot happen
					throw new RuntimeException("Unknown result set status.");
				}

				// reached end of the collection?
				if (collectionEndStatus != null) {
					this.parser.contextExhausted(collectionEndStatus,
							this.numRows, true);
					return true;
				}
			}

			// next read will be subsequent read
			if (setSubsequentRead)
				this.subsequentRead = true;

			// ready to read the row
			return false;

		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Check if the current row belongs to the same parent object of the
	 * collection.
	 *
	 * @return {@code true} if still the same.
	 */
	private boolean isSameParent() {

		for (int i = 0; i < this.parentKeyCtxs.length; i++) {
			if (!this.parentKeyValues[i].equals(this.parentKeyCtxs[i].getKey()))
				return false;
		}

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	String getObjectType() {

		// collections don't have object types
		throw this.createIllegalStateException();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	CollectionResultSetParserContext enterCollection() {

		// collections of collections are not supported
		throw this.createIllegalStateException();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	Object getMapKey() {

		// check if collection end reached
		if (this.isCollectionEnd(false))
			return null;

		// return the key
		return this.getKey();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	void childContextExhausted(final ResultSetStatus rsStatus, int numRows) {

		this.rsStatus = rsStatus;
		this.numRows += numRows;
	}
}
