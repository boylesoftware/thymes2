package org.bsworks.x2.services.versioning.impl.rdbms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bsworks.x2.services.persistence.PersistenceTransaction;
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.services.persistence.RDBMSPersistenceTransaction;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.services.versioning.impl.PersistentResourceVersionInfoImpl;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * RDBMS table-based persistent resource versioning service implementation.
 *
 * @author Lev Himmelfarb
 */
class RDBMSPersistentResourceVersioningService
	implements PersistentResourceVersioningService {

	/**
	 * Collections table name.
	 */
	private final String tableName;

	/**
	 * Persistent resource name column name.
	 */
	private final String nameColName;

	/**
	 * Collection version column name.
	 */
	private final String versionColName;

	/**
	 * Collection last modification timestamp column name.
	 */
	private final String timestampColName;

	/**
	 * Query used to select collection rows.
	 */
	private String selectQuery;

	/**
	 * Query used to update a collection row.
	 */
	private String updateQuery;

	/**
	 * Query used to lock collection rows.
	 */
	private String lockQuery;


	/**
	 * Create new service instance.
	 *
	 * @param tableName Collections table name.
	 * @param nameColName Persistent resource name column name.
	 * @param versionColName Collection version column name.
	 * @param timestampColName Collection last modification timestamp column
	 * name.
	 */
	RDBMSPersistentResourceVersioningService(final String tableName,
			final String nameColName, final String versionColName,
			final String timestampColName) {

		this.tableName = tableName;
		this.nameColName = nameColName;
		this.versionColName = versionColName;
		this.timestampColName = timestampColName;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceVersionInfo getCollectionsVersionInfo(
			final PersistenceTransaction _tx,
			final Set<Class<?>> prsrcClasses) {
		final RDBMSPersistenceTransaction tx =
			(RDBMSPersistenceTransaction) _tx;

		// get the query text
		if (this.selectQuery == null) {
			final SQLDialect dialect = tx.getSQLDialect();
			this.selectQuery = dialect.makeSelectWithShareLock(
					"SELECT "
						+ this.versionColName + " AS "
							+ dialect.quoteColumnLabel("vi$version")
						+ ", " + this.timestampColName + " AS "
							+ dialect.quoteColumnLabel(
									"vi$lastModificationTimestamp")
					+ " FROM "
						+ this.tableName
					+ " WHERE "
						+ this.nameColName + " IN (??prsrcNames)");
		}

		// get persistent resource names
		final int numCollections = prsrcClasses.size();
		if (numCollections == 0)
			throw new IllegalArgumentException(
					"Empty persistent resource classes set.");
		final Collection<String> prsrcNames = new ArrayList<>(
				numCollections > 10 ? numCollections : 10);
		for (final Class<?> prsrcClass : prsrcClasses)
			prsrcNames.add(prsrcClass.getSimpleName());

		// get the version info
		final List<PersistentResourceVersionInfoImpl> vis = tx
				.createQuery(this.selectQuery,
						PersistentResourceVersionInfoImpl.class)
				.setParameter("prsrcNames",
						PersistentValueType.STRING, prsrcNames)
				.getResultList(null);

		// check that we've got all the resources
		if (vis.size() != numCollections) {
			throw new RuntimeException("Collection versioning table must"
					+ " contain exactly one row for each of the persistent"
					+ " resources: " + prsrcNames + ".");
		}

		// calculate the result
		long version = 0;
		Date lastModTS = new Date(0);
		for (final PersistentResourceVersionInfoImpl vi : vis) {
			version += vi.getVersion();
			if (vi.getLastModificationTimestamp().after(lastModTS))
				lastModTS = vi.getLastModificationTimestamp();
		}

		// return the combined result
		return new PersistentResourceVersionInfoImpl(version, lastModTS);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void registerCollectionsModification(final PersistenceTransaction _tx,
			final Set<Class<?>> prsrcClasses) {
		final RDBMSPersistenceTransaction tx =
			(RDBMSPersistenceTransaction) _tx;

		// get the query text
		if (this.updateQuery == null) {
			this.updateQuery = "UPDATE " + this.tableName + " SET "
					+ this.versionColName + " = " + this.versionColName + " + 1"
					+ ", " + this.timestampColName + " = ?lastModTS WHERE "
					+ this.nameColName + " IN (??prsrcNames)";
		}

		// get persistent resource names
		final int numCollections = prsrcClasses.size();
		if (numCollections == 0)
			return;
		final Collection<String> prsrcNames = new ArrayList<>(
				numCollections > 10 ? numCollections : 10);
		for (final Class<?> prsrcClass : prsrcClasses)
			prsrcNames.add(prsrcClass.getSimpleName());

		// execute the update
		final long rowsUpdated = tx
				.createUpdate(this.updateQuery)
				.setParameter("prsrcNames",
						PersistentValueType.STRING, prsrcNames)
				.setParameter("lastModTS",
						PersistentValueType.DATE, new Date())
				.execute();

		// check that we had the row
		if (rowsUpdated != numCollections)
			throw new RuntimeException("Collection versioning table must"
					+ " contain exactly one row for each of the persistent"
					+ " resources: " + prsrcNames + ".");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void lockCollections(final PersistenceTransaction _tx,
			final Set<Class<?>> prsrcClasses) {
		final RDBMSPersistenceTransaction tx =
			(RDBMSPersistenceTransaction) _tx;

		// get number of collections to lock
		final int numCollections = prsrcClasses.size();
		if (numCollections == 0)
			return;

		// get the query text
		if (this.lockQuery == null) {
			final SQLDialect dialect = tx.getSQLDialect();
			this.lockQuery = dialect.makeSelectWithExclusiveLock(
					"SELECT COUNT(*) FROM " + this.tableName
					+ " WHERE " + this.nameColName + " IN (??prsrcNames)");
		}

		// get persistent resource names
		final Collection<String> prsrcNames = new ArrayList<>(
				numCollections > 10 ? numCollections : 10);
		for (final Class<?> prsrcClass : prsrcClasses)
			prsrcNames.add(prsrcClass.getSimpleName());

		// lock the rows and get the count
		final int count = tx
				.createQuery(this.lockQuery, Integer.class)
				.setParameter("prsrcNames",
						PersistentValueType.STRING, prsrcNames)
				.getFirstResult(null)
				.intValue();

		// check that we've got all the resources
		if (count != numCollections) {
			throw new RuntimeException("Collection versioning table must"
					+ " contain exactly one row for each of the persistent"
					+ " resources: " + prsrcNames + ".");
		}
	}
}
