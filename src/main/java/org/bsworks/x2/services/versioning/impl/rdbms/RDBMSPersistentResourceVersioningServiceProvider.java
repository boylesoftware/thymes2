package org.bsworks.x2.services.versioning.impl.rdbms;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.services.persistence.RDBMSPersistenceTransaction;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.util.StringUtils;


/**
 * Persistent resource versioning service provider that uses an RDBMS table to
 * keep track of persistent resource collection versions. This service
 * provider can be used only with a {@link PersistenceService} implementation
 * that provides {@link RDBMSPersistenceTransaction} transaction handlers.
 *
 * <p>In order to use the implementation, it needs to be configured using the
 * following web-application context initialization parameters:
 *
 * <dl>
 * <dt>{@value #TABLE_INITPARAM}</dt><dd>Name of the table used for the
 * persistent resource collections versioning information. The table needs to
 * have three columns: the persistent resource name, which is the key (short
 * name is used, see {@link Class#getSimpleName()}), the collection version
 * number (an integer), and the collection last modification timestamp.</dd>
 * <dt>{@value #NAME_COLUMN_INITPARAM}</dt><dd>Name of the persistent resource
 * name column.</dd>
 * <dt>{@value #VERSION_COLUMN_INITPARAM}</dt><dd>Name of the persistent
 * resource collection version number column.</dd>
 * <dt>{@value #TIMESTAMP_COLUMN_INITPARAM}</dt><dd>Name of the persistent
 * resource collection last modification timestamp column.</dd>
 * </dl>
 *
 * <p>There are no default values for any of these parameters. They must be
 * specified by the application. Also, the table must exist and it must have a
 * row for every persistent resource used by the application.
 *
 * @author Lev Himmelfarb
 */
public class RDBMSPersistentResourceVersioningServiceProvider
	implements ServiceProvider<PersistentResourceVersioningService> {

	/**
	 * Name of web-application context initialization parameter used to specify
	 * collection versioning information table name.
	 */
	public static final String TABLE_INITPARAM =
		"x2.service.versioning.rdbms.table";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource names.
	 */
	public static final String NAME_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.nameColumn";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource collection versions.
	 */
	public static final String VERSION_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.versionColumn";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource collection last
	 * modification timestamps.
	 */
	public static final String TIMESTAMP_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.timestampColumn";


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<PersistentResourceVersioningService> getServiceClass() {

		return PersistentResourceVersioningService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceVersioningService createService(
			final ServletContext sc, final String serviceInstanceId,
			final Resources resources, final RuntimeContext runtimeCtx)
		throws InitializationException {

		LogFactory.getLog(this.getClass()).debug("creating RDBMS table-based"
				+ " persistent resource collections versioning service");

		return new RDBMSPersistentResourceVersioningService(
				getInitParam(sc, TABLE_INITPARAM),
				getInitParam(sc, NAME_COLUMN_INITPARAM),
				getInitParam(sc, VERSION_COLUMN_INITPARAM),
				getInitParam(sc, TIMESTAMP_COLUMN_INITPARAM));
	}

	/**
	 * Get required web-application context initialization parameter.
	 *
	 * @param sc Servlet context.
	 * @param paramName Parameter name.
	 *
	 * @return Parameter value.
	 *
	 * @throws InitializationException If parameter is not specified.
	 */
	private static String getInitParam(final ServletContext sc,
			final String paramName)
		throws InitializationException {

		final String paramVal =
			StringUtils.nullIfEmpty(sc.getInitParameter(paramName));
		if (paramVal == null)
			throw new InitializationException("Web-application must specify "
					+ paramName + " context initialization parameter.");

		return paramVal;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(
			final PersistentResourceVersioningService service) {

		// nothing
	}
}
