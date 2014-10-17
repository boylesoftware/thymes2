package org.bsworks.x2.services.persistence;

import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Extension of {@link PersistenceTransaction} for RDBMS persistence.
 *
 * @author Lev Himmelfarb
 */
public interface RDBMSPersistenceTransaction
	extends PersistenceTransaction {

	/**
	 * Get SQL dialect used by the database.
	 *
	 * @return The SQL dialect.
	 */
	SQLDialect getSQLDialect();
}
