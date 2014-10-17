package org.bsworks.x2.util.sql.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.bsworks.x2.InitializationException;


/**
 * Selector of SQL dialect for a given RDBMS product.
 *
 * @author Lev Himmelfarb
 */
public final class SQLDialectSelector {

	/**
	 * All methods are static.
	 */
	private SQLDialectSelector() {}


	/**
	 * Select dialect.
	 *
	 * @param db Database meta-data.
	 *
	 * @return The dialect to use.
	 *
	 * @throws SQLException If an error happens communicating with the database.
	 * @throws InitializationException If dialect cannot be selected.
	 */
	public static SQLDialect selectDialect(final DatabaseMetaData db)
		throws SQLException, InitializationException {

		final String dbProductName = db.getDatabaseProductName();
		switch (dbProductName) {
		case "PostgreSQL":
			return new PostgreSQLDialect();
		case "MySQL":
			return new MySQLDialect();
		default:
			throw new InitializationException(
					"Unsupported database product " + dbProductName);
		}
	}
}
