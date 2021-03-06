package org.bsworks.x2.services.persistence.impl.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.LockType;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceQuery;
import org.bsworks.x2.services.persistence.PersistenceUpdate;
import org.bsworks.x2.services.persistence.PersistentResourceFetch;
import org.bsworks.x2.services.persistence.RDBMSPersistenceTransaction;
import org.bsworks.x2.util.JDBCConnectionWrapper;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Persistence transaction implementation used by JDBC persistence service
 * implementation. Transaction provided to the application by the framework can
 * be cast to this class if the application knows that it is using the JDBC
 * persistence service. The JDBC transaction provides access to the underlying
 * database connection via its {@link #getConnection()} method.
 *
 * @author Lev Himmelfarb
 */
public class JDBCPersistenceTransaction
	implements RDBMSPersistenceTransaction {

	/**
	 * Simple value readers.
	 */
	private static final Map<Class<?>, ResultSetValueReader<?>> SIMPLE_READERS;
	static {
		SIMPLE_READERS = new HashMap<>(10);
		SIMPLE_READERS.put(String.class,
				ResultSetValueReader.STRING_VALUE_READER);
		SIMPLE_READERS.put(Integer.class,
				ResultSetValueReader.INTEGER_VALUE_READER);
		SIMPLE_READERS.put(BigDecimal.class,
				ResultSetValueReader.BIG_DECIMAL_VALUE_READER);
		SIMPLE_READERS.put(Boolean.class,
				ResultSetValueReader.BOOLEAN_VALUE_READER);
		SIMPLE_READERS.put(Double.class,
				ResultSetValueReader.DOUBLE_VALUE_READER);
		SIMPLE_READERS.put(Long.class,
				ResultSetValueReader.LONG_VALUE_READER);
		SIMPLE_READERS.put(Date.class,
				ResultSetValueReader.DATE_VALUE_READER);
		SIMPLE_READERS.put(Byte.class,
				ResultSetValueReader.BYTE_VALUE_READER);
		SIMPLE_READERS.put(Short.class,
				ResultSetValueReader.SHORT_VALUE_READER);
		SIMPLE_READERS.put(Float.class,
				ResultSetValueReader.FLOAT_VALUE_READER);
	}


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Application resources manager.
	 */
	private final Resources resources;

	/**
	 * The database connection.
	 */
	private final Connection con;

	/**
	 * Connection, returned by the {@link #getConnection()} method.
	 */
	private Connection exposedCon = null;

	/**
	 * SQL dialect.
	 */
	private final SQLDialect dialect;

	/**
	 * Actor performing the transaction, or {@code null} if unauthenticated.
	 */
	private final Actor actor;

	/**
	 * Query parameter value handlers factory.
	 */
	private final ParameterValuesFactoryImpl paramsFactory;

	/**
	 * Locked tables, or {@code null} if nothing is locked.
	 */
	private String[] lockedTables;

	/**
	 * Temporary tables created in this transaction.
	 */
	private Set<String> tempTables = new HashSet<>();


	/**
	 * Create new transaction.
	 *
	 * @param resources Application resources manager.
	 * @param con The database connection.
	 * @param dialect SQL dialect.
	 * @param actor Actor performing the transaction, or {@code null} if
	 * unauthenticated.
	 * @param paramsFactory Query parameter value handlers factory.
	 */
	JDBCPersistenceTransaction(final Resources resources, final Connection con,
			final SQLDialect dialect, final Actor actor,
			final ParameterValuesFactoryImpl paramsFactory) {

		this.resources = resources;
		this.con = con;
		this.dialect = dialect;
		this.actor = actor;
		this.paramsFactory = paramsFactory;
	}


	/**
	 * Get database connection used by the transaction. All transaction handling
	 * methods on the returned connection object are disabled and throw
	 * {@link UnsupportedOperationException}.
	 *
	 * @return The underlying database connection.
	 */
	public Connection getConnection() {

		if (this.exposedCon == null)
			this.exposedCon = new JDBCConnectionWrapper(this.con) {
				@Override
				public void setAutoCommit(final boolean autoCommit) {
					throw new UnsupportedOperationException();
				}
				@Override
				public void commit() {
					throw new UnsupportedOperationException();
				}
				@Override
				public void rollback() {
					throw new UnsupportedOperationException();
				}
				@Override
				public void close() {
					throw new UnsupportedOperationException();
				}
		};

		return this.exposedCon;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void commitAndStartNew() {

		if (this.log.isDebugEnabled())
			this.log.debug("committing and starting new transaction on"
					+ " connection #" + this.con.hashCode());
		try {
			this.con.commit();
		} catch (final SQLException e) {
			throw new PersistenceException("Error committing transaction.", e);
		} finally {
			this.tempTables.clear();
			this.releaseLocks();
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@SuppressWarnings("unchecked") // we know the type of the simple value
	@Override
	public <X> PersistenceQuery<X> createQuery(final String queryText,
			final Class<X> resultClass) {

		ResultSetValueReader<?> simpleValueReader =
			SIMPLE_READERS.get(resultClass);
		if (simpleValueReader != null)
			return (PersistenceQuery<X>) new SimpleValuePersistenceQueryImpl<>(
					this.resources, this, queryText,
					simpleValueReader, null);

		return new ResourcePersistenceQueryImpl<>(this.resources, this,
				queryText, resultClass, null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceUpdate createUpdate(final String stmtText) {

		return new PersistenceUpdateImpl(this.resources, this, stmtText, null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> PersistentResourceFetch<R> createPersistentResourceFetch(
			final Class<R> prsrcClass) {

		return new PersistentResourceFetchImpl<>(this.resources, this,
				prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> void persist(final Class<R> prsrcClass, final R recTmpl) {

		// check that the transaction is authenticated
		if (this.actor == null)
			throw new UnsupportedOperationException("Persisting records is not"
					+ " allowed to unauthenticated transactions.");

		// get persistent resource handler
		final PersistentResourceHandler<R> prsrcHandler =
			this.resources.getPersistentResourceHandler(prsrcClass);

		// set meta-properties
		final Date now = new Date();
		for (final MetaPropertyType metaType : MetaPropertyType.values()) {
			final MetaPropertyHandler metaPropHandler =
				prsrcHandler.getMetaProperty(metaType);
			if (metaPropHandler == null)
				continue;
			switch (metaType) {
			case VERSION:
				try {
					metaPropHandler.setValue(recTmpl,
							metaPropHandler.getValueHandler().valueOf("1"));
				} catch (final InvalidResourceDataException e) { // can't happen
					throw new RuntimeException("Error getting initial version.",
							e);
				}
				break;
			case CREATION_ACTOR:
				metaPropHandler.setValue(recTmpl, this.actor.getActorName());
				break;
			case CREATION_TIMESTAMP:
				metaPropHandler.setValue(recTmpl, now);
				break;
			case MODIFICATION_ACTOR:
				metaPropHandler.setValue(recTmpl, this.actor.getActorName());
				break;
			case MODIFICATION_TIMESTAMP:
				metaPropHandler.setValue(recTmpl, now);
				break;
			default:
			}
		}

		// create execution plan
		final InsertBuilder stmts = new InsertBuilder(this.resources,
				this.paramsFactory, prsrcHandler, recTmpl, this.actor);

		// execute it
		try {
			stmts.execute(this.con);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> boolean update(final Class<R> prsrcClass, final R rec,
			final R recTmpl, final Set<String> updatedProps) {

		// check that the transaction is authenticated
		if (this.actor == null)
			throw new UnsupportedOperationException("Updating records is not"
					+ " allowed to unauthenticated transactions.");

		// get persistent resource handler
		final PersistentResourceHandler<R> prsrcHandler =
			this.resources.getPersistentResourceHandler(prsrcClass);

		// create execution plan
		final UpdateBuilder stmts = new UpdateBuilder(this.resources, this,
				prsrcHandler, rec, recTmpl, updatedProps);

		// execute it
		try {
			return stmts.execute(this.con);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> boolean delete(final Class<R> prsrcClass,
			final FilterSpec<R> filter, final Set<Class<?>> affectedResources) {

		// check that the transaction is authenticated
		if (this.actor == null)
			throw new UnsupportedOperationException("Deleting records is not"
					+ " allowed to unauthenticated transactions.");

		// get persistent resource handler
		final PersistentResourceHandler<R> prsrcHandler =
			this.resources.getPersistentResourceHandler(prsrcClass);

		// create execution plan
		final DeleteBuilder stmts = new DeleteBuilder(this.resources, this,
				prsrcHandler, filter);

		// execute it
		try {
			return stmts.execute(this.con, affectedResources);
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void lock(final LockType lockType, final Class<?>... prsrcClasses) {

		if (this.lockedTables != null)
			throw new IllegalStateException(
					"The transaction already has locked persistent resource"
					+ " collections.");

		if ((prsrcClasses == null) || (prsrcClasses.length == 0))
			throw new IllegalArgumentException(
					"Must specify persistent resource classes.");

		final String[] lockedTables = new String[prsrcClasses.length];
		for (int i = 0; i < prsrcClasses.length; i++) {
			final PersistentResourceHandler<?> prsrcHandler =
				this.resources.getPersistentResourceHandler(prsrcClasses[i]);
			lockedTables[i] = prsrcHandler.getPersistentCollectionName();
		}

		if (this.log.isDebugEnabled())
			this.log.debug("placing table locks on connection #"
					+ this.con.hashCode());

		final String stmtText = (lockType == LockType.SHARED ?
				this.dialect.lockTablesInShareMode(lockedTables) :
				this.dialect.lockTablesInExclusiveMode(lockedTables));
		try (final Statement stmt = this.con.createStatement()) {
			stmt.execute(stmtText);
			Utils.logWarnings(this.log, stmt.getWarnings());
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}

		this.lockedTables = lockedTables;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SQLDialect getSQLDialect() {

		return this.dialect;
	}


	/**
	 * Release any held locks.
	 */
	void releaseLocks() {

		if (this.lockedTables == null)
			return;

		final String stmtText = this.dialect.unlockTables(this.lockedTables);
		this.lockedTables = null;

		if (stmtText == null)
			return;

		if (this.log.isDebugEnabled())
			this.log.debug("releasing table locks on connection #"
					+ this.con.hashCode());

		try (final Statement stmt = this.con.createStatement()) {
			stmt.execute(stmtText);
			Utils.logWarnings(this.log, stmt.getWarnings());
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Register temporary table creation for the transaction.
	 *
	 * @param tableName Temporary table name.
	 *
	 * @return {@code false} if already existed.
	 */
	boolean addTempTable(final String tableName) {

		return this.tempTables.add(tableName);
	}

	/**
	 * Get used database connection.
	 *
	 * @return The database connection.
	 */
	Connection getRawConnection() {

		return this.con;
	}

	/**
	 * Get query parameter value handlers factory.
	 *
	 * @return The factory.
	 */
	ParameterValuesFactoryImpl getParameterValuesFactory() {

		return this.paramsFactory;
	}

	/**
	 * Get actor associated with the transaction.
	 *
	 * @return The actor, or {@code null} if anonymous.
	 */
	Actor getActor() {

		return this.actor;
	}
}
