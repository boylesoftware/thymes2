package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bsworks.x2.services.persistence.ParameterValue;


/**
 * JDBC query parameter value handler.
 *
 * @author Lev Himmelfarb
 */
interface JDBCParameterValue
	extends ParameterValue {

	/**
	 * Get number of query parameter placeholders required for the parameter.
	 *
	 * @return Number of placeholders.
	 */
	int getNumPlaceholders();

	/**
	 * Set parameter in the query.
	 *
	 * @param pstmt Query prepared statement.
	 * @param ind Beginning parameter index (1 based).
	 *
	 * @return Beginning parameter index for the next parameter.
	 *
	 * @throws SQLException If an error happens.
	 */
	int set(PreparedStatement pstmt, int ind)
		throws SQLException;

	/**
	 * Needs to be overridden for logging.
	 */
	@Override
	String toString();
}
