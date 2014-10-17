package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Abstract database statement such as a query or an update.
 *
 * @author Lev Himmelfarb
 */
abstract class AbstractPersistenceStatement {

	/**
	 * The log.
	 */
	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Database connection.
	 */
	private final Connection con;

	/**
	 * Query parameter value handlers factory.
	 */
	private final ParameterValuesFactoryImpl paramsFactory;

	/**
	 * The statement text.
	 */
	private String stmtText;

	/**
	 * Parameters.
	 */
	private final Map<String, JDBCParameterValue> params;


	/**
	 * Create new statement.
	 *
	 * @param con Database connection.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param params Initial parameters. May be {@code null} for none.
	 */
	protected AbstractPersistenceStatement(final Connection con,
			final ParameterValuesFactoryImpl paramsFactory,
			final Map<String, JDBCParameterValue> params) {

		this.con = con;
		this.paramsFactory = paramsFactory;
		this.params = (params != null ? params :
			new HashMap<String, JDBCParameterValue>());
	}


	/**
	 * Set new statement text without changing anything else about the statement
	 * setup.
	 *
	 * @param stmtText New statement text.
	 */
	protected final void setStatementText(final String stmtText) {

		this.stmtText = stmtText;
	}

	/**
	 * Set statement parameter.
	 *
	 * @param paramName Parameter name.
	 * @param paramType Parameter value type.
	 * @param paramValue Parameter value. May be {@code null}.
	 */
	protected final void setStatementParameter(final String paramName,
			final PersistentValueType paramType, final Object paramValue) {

		this.params.put(paramName,
				this.paramsFactory.getParameterValue(paramType, paramValue));
	}

	/**
	 * Create prepared statement for the query.
	 *
	 * @return The query prepared statement.
	 *
	 * @throws SQLException If a database error happens.
	 */
	protected final PreparedStatement getPreparedStatement()
		throws SQLException {

		final boolean debug = this.log.isDebugEnabled();

		// build query SQL and arrange parameters
		final List<JDBCParameterValue> paramsList = new ArrayList<>();
		final String sql = Utils.convertNamedParams(this.stmtText, this.params,
				paramsList);

		// log it
		if (debug)
			this.log.debug("executing SQL query:\n" + sql
					+ "\nparams: " + paramsList);

		// prepare statement
		final PreparedStatement pstmt =
			this.con.prepareStatement(sql,
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);

		// set parameters
		int paramInd = 1;
		for (final JDBCParameterValue paramHandler : paramsList)
			paramInd = paramHandler.set(pstmt, paramInd);

		// return the prepared statement
		return pstmt;
	}
}
