package org.bsworks.x2.services.versioning.impl.rdbms;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

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
 * number (an integer), and the collection last modification timestamp. Default
 * is {@value #DEFAULT_TABLE}.</dd>
 * <dt>{@value #NAME_COLUMN_INITPARAM}</dt><dd>Name of the persistent resource
 * name column. Default is {@value #DEFAULT_NAME_COLUMN}.</dd>
 * <dt>{@value #VERSION_COLUMN_INITPARAM}</dt><dd>Name of the persistent
 * resource collection version number column. Default is
 * {@value #DEFAULT_VERSION_COLUMN}.</dd>
 * <dt>{@value #TIMESTAMP_COLUMN_INITPARAM}</dt><dd>Name of the persistent
 * resource collection last modification timestamp column. Default is
 * {@value #DEFAULT_TIMESTAMP_COLUMN}.</dd>
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
	 * Default collection versioning information table name.
	 */
	public static final String DEFAULT_TABLE = "x2prsrccol";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource names.
	 */
	public static final String NAME_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.nameColumn";

	/**
	 * Default name of the column for the persistent resource names.
	 */
	public static final String DEFAULT_NAME_COLUMN = "name";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource collection versions.
	 */
	public static final String VERSION_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.versionColumn";

	/**
	 * Default name of the column for the persistent resource collection
	 * versions.
	 */
	public static final String DEFAULT_VERSION_COLUMN = "version";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * name of the column for the persistent resource collection last
	 * modification timestamps.
	 */
	public static final String TIMESTAMP_COLUMN_INITPARAM =
		"x2.service.versioning.rdbms.timestampColumn";

	/**
	 * Default name of the column for the persistent resource collection last
	 * modification timestamps.
	 */
	public static final String DEFAULT_TIMESTAMP_COLUMN = "modified_on";


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
			final Resources resources, final RuntimeContext runtimeCtx) {

		LogFactory.getLog(this.getClass()).debug("creating RDBMS table-based"
				+ " persistent resource collections versioning service");

		return new RDBMSPersistentResourceVersioningService(
				getInitParam(sc, TABLE_INITPARAM, DEFAULT_TABLE),
				getInitParam(sc, NAME_COLUMN_INITPARAM, DEFAULT_NAME_COLUMN),
				getInitParam(sc, VERSION_COLUMN_INITPARAM,
						DEFAULT_VERSION_COLUMN),
				getInitParam(sc, TIMESTAMP_COLUMN_INITPARAM,
						DEFAULT_TIMESTAMP_COLUMN));
	}

	/**
	 * Get required web-application context initialization parameter.
	 *
	 * @param sc Servlet context.
	 * @param paramName Parameter name.
	 * @param defaultVal Default value.
	 *
	 * @return Parameter value.
	 */
	private static String getInitParam(final ServletContext sc,
			final String paramName, final String defaultVal) {

		final String paramVal =
			StringUtils.nullIfEmpty(sc.getInitParameter(paramName));

		return (paramVal != null ? paramVal : defaultVal);
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
