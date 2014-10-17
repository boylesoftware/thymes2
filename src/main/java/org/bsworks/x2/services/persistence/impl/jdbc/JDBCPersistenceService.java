package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.bsworks.x2.Actor;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * JDBC persistence service implementation.
 *
 * @author Lev Himmelfarb
 */
class JDBCPersistenceService
	implements PersistenceService {

	/**
	 * Runtime context.
	 */
	private final RuntimeContext runtimeCtx;

	/**
	 * The data source.
	 */
	private final DataSource ds;

	/**
	 * SQL dialect.
	 */
	private final SQLDialect dialect;

	/**
	 * Transaction isolation level.
	 */
	private final int txIsoLevel;

	/**
	 * Query parameter value handlers factory.
	 */
	private final ParameterValuesFactoryImpl paramsFactory;


	/**
	 * Create new service.
	 *
	 * @param runtimeCtx Runtime context.
	 * @param ds The data source.
	 * @param dialect SQL dialect.
	 * @param txIsoLevel Transaction isolation level.
	 */
	JDBCPersistenceService(final RuntimeContext runtimeCtx, final DataSource ds,
			final SQLDialect dialect, final int txIsoLevel) {

		this.runtimeCtx = runtimeCtx;
		this.ds = ds;
		this.dialect = dialect;
		this.txIsoLevel = txIsoLevel;
		this.paramsFactory = new ParameterValuesFactoryImpl();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceTransactionHandlerImpl createPersistenceTransaction(
			final Actor actor, final boolean readOnly) {

		try {
			return new PersistenceTransactionHandlerImpl(
					this.runtimeCtx.getResources(), this.ds, this.dialect,
					actor, readOnly, this.txIsoLevel, this.paramsFactory);
		} catch (final SQLException e) {
			throw new PersistenceException("Error starting new transaction.",
					e);
		}
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean supportsEmbeddedCollections() {

		return false;
	}
}
