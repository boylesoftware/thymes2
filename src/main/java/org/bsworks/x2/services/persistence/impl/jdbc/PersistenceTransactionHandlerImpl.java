package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceTransactionHandler;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Persistence transaction handler implementation.
 *
 * @author Lev Himmelfarb
 */
class PersistenceTransactionHandlerImpl
	implements PersistenceTransactionHandler {

	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Application resources manager.
	 */
	private final Resources resources;

	/**
	 * Actor performing the transaction, or {@code null} if unauthenticated.
	 */
	private final Actor actor;

	/**
	 * The database connection.
	 */
	private final Connection con;

	/**
	 * The transaction.
	 */
	private final JDBCPersistenceTransaction tx;

	/**
	 * Tells if the transaction has been committed via a
	 * {@link #commitTransaction()} call.
	 */
	private final AtomicBoolean isCommitted = new AtomicBoolean(false);


	/**
	 * Start new transaction and create handler for it.
	 *
	 * @param resources Application resources manager.
	 * @param ds The database data source.
	 * @param dialect SQL dialect.
	 * @param actor Actor performing the transaction, or {@code null} if
	 * unauthenticated.
	 * @param readOnly {@code true} for a read-only transaction.
	 * @param txIsoLevel Isolation level.
	 * @param paramsFactory Query parameter values factory.
	 *
	 * @throws SQLException If an error happens.
	 */
	PersistenceTransactionHandlerImpl(final Resources resources,
			final DataSource ds, final SQLDialect dialect, final Actor actor,
			final boolean readOnly, final int txIsoLevel,
			final ParameterValuesFactoryImpl paramsFactory)
		throws SQLException {

		this.resources = resources;
		this.actor = actor;

		boolean success = false;
		try {
			this.con = ds.getConnection();
			this.con.setReadOnly(!dialect.tempTablesRequireReadWrite()
					&& readOnly);
			this.con.setTransactionIsolation(txIsoLevel);
			this.con.setAutoCommit(false);
			if (this.log.isDebugEnabled())
				this.log.debug("started transaction on connection #"
						+ this.con.hashCode());
			success = true;
		} finally {
			if (!success)
				this.con.close();
		}

		this.tx = new JDBCPersistenceTransaction(this.resources, this.con,
				dialect, this.actor, paramsFactory);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void close() {

		try {
			if (!this.isCommitted.getAndSet(true)) {
				if (this.log.isDebugEnabled())
					this.log.debug("rolling back transaction on connection #"
							+ this.con.hashCode());
				try {
					this.con.rollback();
				} catch (final SQLException e) {
					throw new PersistenceException(
							"Error rolling back transaction.", e);
				}
			}
		} finally {
			if (this.log.isDebugEnabled())
				this.log.debug("closing connection #" + this.con.hashCode());
			try {
				this.con.close();
			} catch (final SQLException e) {
				throw new PersistenceException("Error closing connection.", e);
			}
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public JDBCPersistenceTransaction getTransaction() {

		return this.tx;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void commitTransaction() {

		if (this.isCommitted.getAndSet(true))
			throw new IllegalStateException("Already committed.");

		if (this.log.isDebugEnabled())
			this.log.debug("committing transaction on connection #"
					+ this.con.hashCode());
		try {
			this.con.commit();
		} catch (final SQLException e) {
			throw new PersistenceException("Error committing transaction.", e);
		}
	}
}
