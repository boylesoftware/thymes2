package org.bsworks.x2.services.persistence.impl.jdbc;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourcePropertyValueHandler;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.TypePropertyHandler;
import org.bsworks.x2.services.persistence.PersistenceException;


/**
 * Parser for a single query result set that produces resource record objects.
 *
 * @author Lev Himmelfarb
 */
class ResultSetParser
	implements ResourceReadSession {

	/**
	 * Resources manager.
	 */
	private final Resources resources;

	/**
	 * The result set descriptor.
	 */
	private final ResultSetDescriptor rsd;

	/**
	 * References fetch result, or {@code null}.
	 */
	private final Map<String, Object> refsFetchResult;

	/**
	 * Date format for parsing dates.
	 */
	private DateFormat df = null;

	/**
	 * Top-level resource object parsing context.
	 */
	private final ObjectResultSetParserContext rootCtx;

	/**
	 * Current parsing context.
	 */
	private ResultSetParserContext ctx;

	/**
	 * Current result set status.
	 */
	private ResultSetStatus rsStatus;

	/**
	 * Number of rows consumed by the parser.
	 */
	private int numRows;

	/**
	 * Tells if the last context exhausted because it was a collection context
	 * and the end of the collection was reached.
	 */
	private boolean wasCollectionEnd;

	/**
	 * Number of rows taken by fetched references.
	 */
	private final Map<String, Integer> fetchedRefsRows;


	/**
	 * Create new parser.
	 *
	 * @param resources Application resources manager.
	 * @param rs The result set. The result set must be positioned on the first
	 * row to start parsing (not before it!).
	 * @param sessionCache Session cache, or {@code null} if not used.
	 * @param actor The parsed data consumer.
	 * @param refsFetchResult References fetch result, or {@code null}.
	 *
	 * @throws SQLException If a database error happens.
	 */
	ResultSetParser(final Resources resources, final ResultSet rs,
			final ResourceReadSessionCache sessionCache,
			final Actor actor, final Map<String, Object> refsFetchResult)
		throws SQLException {
		this(resources,
				new ResultSetDescriptor(resources, rs, sessionCache, actor,
						refsFetchResult),
				null, 1, 1, refsFetchResult);
	}

	/**
	 * Create new parser. This constructor allows creating sub-result set
	 * parsers.
	 *
	 * @param resources Application resources manager.
	 * @param rsd Result set descriptor.
	 * @param parentCtx Parent context for a sub-parser, or {@code null} for the
	 * top-level parser.
	 * @param keyColInd Index of the column containing the top-level resource
	 * key.
	 * @param firstPropertyColInd Index of the first column containing the
	 * top-level resource property.
	 * @param refsFetchResult References fetch result, or {@code null}.
	 */
	ResultSetParser(final Resources resources, final ResultSetDescriptor rsd,
			final ResultSetParserContext parentCtx, final int keyColInd,
			final int firstPropertyColInd,
			final Map<String, Object> refsFetchResult) {

		this.resources = resources;
		this.rsd = rsd;
		this.refsFetchResult = refsFetchResult;
		this.fetchedRefsRows = new HashMap<>();

		this.rootCtx = new ObjectResultSetParserContext(this, rsd, parentCtx,
				keyColInd, firstPropertyColInd);

		this.reset();
	}


	/**
	 * Reset the parser as new.
	 */
	void reset() {

		this.ctx = null;
		this.rsStatus = ResultSetStatus.ON_NEXT_ROW;
		this.numRows = 0;
		this.wasCollectionEnd = false;
	}

	/**
	 * Tell if the parser can be used to parse next top-level resource object.
	 *
	 * @return {@code true} if can be continued to be used, {@code false} if
	 * reached the end of the result set.
	 */
	boolean hasMore() {

		return (this.rsStatus == ResultSetStatus.ON_NEXT_ROW);
	}

	/**
	 * Call-back called by the current context when it becomes exhausted.
	 *
	 * @param rsStatus Result set status left by the context.
	 * @param numRows Number of rows consumed by the context.
	 * @param isCollection {@code true} if the current context is a collection
	 * context.
	 */
	void contextExhausted(final ResultSetStatus rsStatus, final int numRows,
			final boolean isCollection) {

		if (isCollection)
			this.numRows += numRows;

		final boolean atRoot = (this.ctx == this.rootCtx);

		this.ctx = this.ctx.getParent();

		if (!atRoot) {
			this.ctx.childContextExhausted(rsStatus, numRows);
		} else {
			switch (rsStatus) {
			case ON_LAST_ROW:
				if (this.rootCtx.isSubParserContext()) {
					this.rsStatus = rsStatus;
					break;
				}
				try {
					if (this.rsd.getResultSet().next())
						this.rsStatus = ResultSetStatus.ON_NEXT_ROW;
					else
						this.rsStatus = ResultSetStatus.AFTER_LAST;
					this.numRows++;
				} catch (final SQLException e) {
					throw new PersistenceException(e);
				}
				break;
			case ON_NEXT_ROW:
				this.rsStatus = ResultSetStatus.ON_NEXT_ROW;
				break;
			case AFTER_LAST:
				this.rsStatus = ResultSetStatus.AFTER_LAST;
				break;
			default: // cannot happen
				throw new RuntimeException("Unknown result set status.");
			}
		}

		this.wasCollectionEnd = isCollection;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor getActor() {

		return this.rsd.getActor();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Date stringToDate(final String dateStr)
		throws InvalidResourceDataException {

		if (this.df == null) {
			this.df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			this.df.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

		try {
			return this.df.parse(dateStr);
		} catch (final ParseException e) {
			throw new InvalidResourceDataException(
					"Invalid date value " + dateStr + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceReadSessionCache getSessionCache() {

		return this.rsd.getSessionCache();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String nextProperty() {

		if (this.ctx == null)
			return null;

		return this.ctx.getNextPropertyName();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void swallowValue() {

		throw new PersistenceException("Column in the result set is not a"
				+ " resource property or loading it is disallowed.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterObject() {

		if (this.ctx == null) {
			this.ctx = this.rootCtx;
			this.ctx.reset();
			return true;
		}

		final ObjectResultSetParserContext childCtx = this.ctx.enterObject();
		if (childCtx == null)
			return false;

		this.ctx = childCtx;

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterCollection() {

		final CollectionResultSetParserContext childCtx =
			this.ctx.enterCollection();
		if (childCtx == null)
			return false;

		this.ctx = childCtx;

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean enterMap() {

		return this.enterCollection();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readObjectType(final TypePropertyHandler typePropHandler) {

		return this.ctx.getObjectType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readKey() {

		final Object key = this.ctx.getMapKey();

		return (key == null ? null : key.toString());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readStringValue() {

		return this.ctx.getValue(ResultSetValueReader.STRING_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Byte readByteValue() {

		return this.ctx.getValue(ResultSetValueReader.BYTE_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Short readShortValue() {

		return this.ctx.getValue(ResultSetValueReader.SHORT_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Integer readIntegerValue() {

		return this.ctx.getValue(ResultSetValueReader.INTEGER_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Long readLongValue() {

		return this.ctx.getValue(ResultSetValueReader.LONG_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Boolean readBooleanValue() {

		return this.ctx.getValue(ResultSetValueReader.BOOLEAN_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Float readFloatValue() {

		return this.ctx.getValue(ResultSetValueReader.FLOAT_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Double readDoubleValue() {

		return this.ctx.getValue(ResultSetValueReader.DOUBLE_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public BigDecimal readBigDecimalValue() {

		return this.ctx.getValue(ResultSetValueReader.BIG_DECIMAL_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <E extends Enum<E>> E readEnumValue(Class<E> enumClass) {

		final String value =
			this.ctx.getValue(ResultSetValueReader.STRING_VALUE_READER);

		return (value == null ? null : Enum.valueOf(enumClass, value));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Date readDateValue() {

		return this.ctx.getValue(ResultSetValueReader.DATE_VALUE_READER);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Ref<?> readRefValue() {

		final ResultSetParser subParser = this.rsd.getFetchedReferenceRSParser(
				this.ctx.getValueColumnIndex(), this.ctx);

		final String value =
			this.ctx.getValue(ResultSetValueReader.STRING_VALUE_READER);

		if (value == null)
			return null;

		final Ref<?> ref = this.resources.parseRef(value);

		// check if fetched reference
		if (subParser != null) {

			ResultSetStatus endStatus = ResultSetStatus.ON_LAST_ROW;
			int numRowsConsumed = 0;
			boolean skipped = false;
			if (this.refsFetchResult.containsKey(value)) {
				final Integer numRowsToSkip = this.fetchedRefsRows.get(value);
				if (numRowsToSkip != null) {
					skipped = true;
					try {
						int i;
						for (i = numRowsToSkip.intValue(); i > 0; i--) {
							numRowsConsumed++;
							if (this.rsd.getResultSet().next()) {
								endStatus = ResultSetStatus.ON_NEXT_ROW;
							} else {
								endStatus = ResultSetStatus.AFTER_LAST;
								break;
							}
						}
					} catch (final SQLException e) {
						throw new PersistenceException(e);
					}
				}
			}

			if (!skipped) {
				final PersistentResourceHandler<?> prsrcHandler =
					this.resources.getPersistentResourceHandler(
							ref.getResourceClass());
				final ResourcePropertyValueHandler vh =
					prsrcHandler.getResourceValueHandler();
				final Object refValue;
				try {
					refValue = vh.readValue(ResourcePropertyAccess.LOAD,
							subParser);
				} catch (final IOException | InvalidResourceDataException e) {
					throw new PersistenceException(e);
				}
				this.fetchedRefsRows.put(value,
						Integer.valueOf(subParser.numRows));
				this.refsFetchResult.put(value, refValue);
				endStatus = subParser.rsStatus;
				numRowsConsumed = subParser.numRows;
			}

			this.ctx.childContextExhausted(endStatus, numRowsConsumed);
		}

		return ref;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean wasCollectionEnd() {

		if (this.wasCollectionEnd) {
			this.wasCollectionEnd = false;
			return true;
		}

		return false;
	}
}
