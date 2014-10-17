package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.util.StringUtils;
import org.bsworks.x2.util.sql.dialect.SQLDialect;
import org.bsworks.x2.util.sql.dialect.SQLDialectSelector;


/**
 * Provider of a persistence service implementation that uses JDBC.
 *
 * <p>The implementation expects a {@link DataSource} in the web-application's
 * JNDI environment under name {@value #DS_JNDINAME}.
 *
 * <p>The transaction isolation level can be configured using web-application
 * context initialization parameter called {@value #TXISOLEVEL_INITPARAM}.
 * The values are "READ_UNCOMMITTED", "READ_COMMITTED" (the default),
 * "REPEATABLE_READ" and "SERIALIZABLE".
 *
 * @author Lev Himmelfarb
 */
public class JDBCPersistenceServiceProvider
	implements ServiceProvider<PersistenceService> {

	/**
	 * Data source JNDI name.
	 */
	public static final String DS_JNDINAME = "java:comp/env/jdbc/ds";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure the transaction isolation level.
	 */
	public static final String TXISOLEVEL_INITPARAM =
		"x2.service.persistence.jdbc.isolationLevel";


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<PersistenceService> getServiceClass() {

		return PersistenceService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceService createService(final ServletContext sc,
			String serviceInstanceId, final Resources resources,
			final RuntimeContext runtimeCtx)
		throws InitializationException {

		final Log log = LogFactory.getLog(this.getClass());
		final boolean debug = log.isDebugEnabled();
		if (debug)
			log.debug("creating JDBC persistence manager");

		// get the data source
		final DataSource ds;
		try {
			final Context jndi = new InitialContext();
			try {
				ds = (DataSource) jndi.lookup(DS_JNDINAME);
			} finally {
				jndi.close();
			}
		} catch (final NamingException e) {
			throw new InitializationException("Error looking up database data"
					+ " source in the JNDI environment.", e);
		}

		// test the data source and detect the database type
		if (debug)
			log.debug("establishing connection to the database...");
		final SQLDialect dialect;
		try (final Connection con = ds.getConnection()) {
			final DatabaseMetaData db = con.getMetaData();
			final String dbProductName = db.getDatabaseProductName();
			if (debug)
				log.debug("connected as " + db.getUserName()
						+ " to the database at " + db.getURL()
						+ ", database " + dbProductName + " "
						+ db.getDatabaseProductVersion() + ", driver "
						+ db.getDriverName() + " " + db.getDriverVersion());
			dialect = SQLDialectSelector.selectDialect(db);
		} catch (final SQLException e) {
			throw new InitializationException(
					"Error connecting to the database.", e);
		}

		// get transaction isolation level
		final int txIsoLevel;
		switch (StringUtils.defaultIfEmpty(
				sc.getInitParameter(TXISOLEVEL_INITPARAM),
				"READ_COMMITTED")) {
		case "READ_UNCOMMITTED":
			txIsoLevel = Connection.TRANSACTION_READ_UNCOMMITTED;
			break;
		case "READ_COMMITTED":
			txIsoLevel = Connection.TRANSACTION_READ_COMMITTED;
			break;
		case "REPEATABLE_READ":
			txIsoLevel = Connection.TRANSACTION_REPEATABLE_READ;
			break;
		case "SERIALIZABLE":
			txIsoLevel = Connection.TRANSACTION_SERIALIZABLE;
			break;
		default:
			throw new InitializationException("Invalid transaction isolation"
					+ " level " + sc.getInitParameter(TXISOLEVEL_INITPARAM)
					+ ".");
		}

		// create and return the service
		return new JDBCPersistenceService(runtimeCtx, ds, dialect, txIsoLevel);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final PersistenceService service) {

		// nothing
	}
}
