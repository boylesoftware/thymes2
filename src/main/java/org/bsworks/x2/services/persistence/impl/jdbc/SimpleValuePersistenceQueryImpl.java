package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceQuery;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Persistence query implementation that returns simple type values.
 *
 * @param <Y> Query result type.
 *
 * @author Lev Himmelfarb
 */
class SimpleValuePersistenceQueryImpl<Y>
	extends AbstractPersistenceStatement
	implements PersistenceQuery<Y> {

	/**
	 * Query result resource class.
	 */
	private final ResultSetValueReader<Y> valueReader;

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
	 * @param con Database connection.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param queryText The query text.
	 * @param valueReader Query result value reader.
	 * @param params Initial parameters. May be {@code null} for none.
	 */
	SimpleValuePersistenceQueryImpl(final Connection con,
			final ParameterValuesFactoryImpl paramsFactory,
			final String queryText, final ResultSetValueReader<Y> valueReader,
			final Map<String, JDBCParameterValue> params) {
		super(con, paramsFactory, params);

		this.valueReader = valueReader;

		this.setStatementText(queryText);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<Y> setSessionCache(
			final ResourceReadSessionCache sessionCache) {

		// not used anyway

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<Y> setParameter(final String paramName,
			final PersistentValueType paramType, final Object paramValue) {

		this.setStatementParameter(paramName, paramType, paramValue);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceQuery<Y> setResultRange(final int firstRecord,
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
	public List<Y> getResultList(final Map<String, Object> refsFetchResult) {

		final List<Y> res = new ArrayList<>(
				this.maxRecords > 10 ? this.maxRecords : 10);

		try (final PreparedStatement pstmt = this.getPreparedStatement()) {

			// set result set limit
			if (this.maxRecords > 0)
				pstmt.setMaxRows(this.maxRecords);

			// get the data
			try (final ResultSet rs = this.getResultSet(pstmt)) {
				while (!rs.next())
					res.add(this.valueReader.readValue(rs, 1));
			}

		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}

		return res;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Y getFirstResult(final Map<String, Object> refsFetchResult) {

		try (final PreparedStatement pstmt = this.getPreparedStatement()) {

			// set result set limit
			pstmt.setMaxRows(1);

			// get the data
			try (final ResultSet rs = this.getResultSet(pstmt)) {
				if (!rs.next())
					return null;
				return this.valueReader.readValue(rs, 1);
			}

		} catch (final SQLException e) {
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
