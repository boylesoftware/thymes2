package org.bsworks.x2.services.persistence.impl.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourceHandler;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourcePropertyValueHandler;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceQuery;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Persistence query implementation that returns application resource records.
 *
 * @param <R> Query result type.
 *
 * @author Lev Himmelfarb
 */
class ResourcePersistenceQueryImpl<R>
	extends AbstractPersistenceStatement
	implements PersistenceQuery<R> {

	/**
	 * Application resources manager.
	 */
	private final Resources resources;

	/**
	 * Query result resource class.
	 */
	private final Class<R> rsrcClass;

	/**
	 * Query result resource value handler.
	 */
	private final ResourcePropertyValueHandler rsrcValueHandler;

	/**
	 * The query data consumer.
	 */
	private final Actor actor;

	/**
	 * Session cache.
	 */
	private ResourceReadSessionCache sessionCache;

	/**
	 * First row to fetch.
	 */
	private int firstRecord = 0;

	/**
	 * Maximum number of records to fetch, or negative for no limit.
	 */
	private int maxRecords = -1;


	/**
	 * Create new query.
	 *
	 * @param resources Application resources manager.
	 * @param con Database connection.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param queryText The query text.
	 * @param rsrcClass Query result resource class.
	 * @param actor The query data consumer.
	 * @param params Initial parameters. May be {@code null} for none.
	 */
	ResourcePersistenceQueryImpl(final Resources resources,
			final Connection con,
			final ParameterValuesFactoryImpl paramsFactory,
			final String queryText, final Class<R> rsrcClass, final Actor actor,
			final Map<String, JDBCParameterValue> params) {
		super(con, paramsFactory, params);

		this.resources = resources;
		this.rsrcClass = rsrcClass;
		this.actor = actor;

		final ResourceHandler<R> rsrcHandler =
			resources.getResourceHandler(rsrcClass);
		this.rsrcValueHandler = rsrcHandler.getResourceValueHandler();

		this.setStatementText(queryText);
	}


	/**
	 * Set new query text without changing anything else about the query setup.
	 *
	 * @param queryText New query text.
	 *
	 * @return This query object.
	 */
	ResourcePersistenceQueryImpl<R> setQueryText(final String queryText) {

		this.setStatementText(queryText);

		return this;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<R> setSessionCache(
			final ResourceReadSessionCache sessionCache) {

		this.sessionCache = sessionCache;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<R> setParameter(final String paramName,
			final PersistentValueType paramType, final Object paramValue) {

		this.setStatementParameter(paramName, paramType, paramValue);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<R> setResultRange(final int firstRecord,
			final int maxRecords) {

		if ((firstRecord < 0) || (maxRecords <= 0))
			throw new IllegalArgumentException("Invalid first record or maximum"
					+ " records number value.");

		this.firstRecord = firstRecord;
		this.maxRecords = maxRecords;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public List<R> getResultList(final Map<String, Object> refsFetchResult) {

		final List<R> res = new ArrayList<>(
				this.maxRecords > 10 ? this.maxRecords : 10);

		try (final PreparedStatement pstmt = this.getPreparedStatement()) {

			// set result set limit
			if (this.maxRecords > 0)
				pstmt.setMaxRows(this.maxRecords);

			// get the data
			try (final ResultSet rs = this.getResultSet(pstmt)) {
				if (!rs.next())
					return res;
				final ResultSetParser rsParser =
					new ResultSetParser(this.resources, rs, this.sessionCache,
							this.actor, refsFetchResult);
				do {
					res.add(this.rsrcClass.cast(
							this.rsrcValueHandler.readValue(
									ResourcePropertyAccess.LOAD, rsParser)));
				} while (rsParser.hasMore());
			}

		} catch (final SQLException | IOException |
				InvalidResourceDataException e) {
			throw new PersistenceException(e);
		}

		return res;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public R getFirstResult(final Map<String, Object> refsFetchResult) {

		try (final PreparedStatement pstmt = this.getPreparedStatement()) {

			// set result set limit
			if (this.maxRecords > 0)
				pstmt.setMaxRows(this.maxRecords);

			// get the data
			try (final ResultSet rs = this.getResultSet(pstmt)) {
				if (!rs.next())
					return null;
				final ResultSetParser rsParser =
					new ResultSetParser(this.resources, rs, this.sessionCache,
							this.actor, refsFetchResult);
				return this.rsrcClass.cast(
						this.rsrcValueHandler.readValue(
								ResourcePropertyAccess.LOAD, rsParser));
			}

		} catch (final SQLException | IOException |
				InvalidResourceDataException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Execute the query prepared statement and get the result set.
	 *
	 * @param pstmt The prepared statement.
	 *
	 * @return The result set.
	 *
	 * @throws SQLException If a database error happens.
	 */
	private ResultSet getResultSet(final PreparedStatement pstmt)
		throws SQLException {

		// execute the query and get the result set
		final ResultSet rs = pstmt.executeQuery();
		Utils.logWarnings(this.log, pstmt.getWarnings());

		// advance the result set
		if (this.firstRecord > 0) {
			rs.relative(this.firstRecord);
			Utils.logWarnings(this.log, rs.getWarnings());
		}

		// return the result set
		return rs;
	}
}
