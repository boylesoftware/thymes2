package org.bsworks.x2.services.persistence;

import java.util.Set;

import org.bsworks.x2.resource.FilterSpec;


/**
 * Persistent storage (database) transaction. This is the main interface,
 * provided by the framework, through which the application performs operations
 * on the database.
 *
 * @author Lev Himmelfarb
 */
public interface PersistenceTransaction {

	/**
	 * Commit this transaction and start new one. After this method call, this
	 * transaction object starts representing the new transaction.
	 */
	void commitAndStartNew();

	/**
	 * Create persistent storage query.
	 *
	 * @param <X> Query result type.
	 * @param queryText The query text. The query language is specific to the
	 * persistence service implementation.
	 * @param resultClass Query result class. May be an application resource
	 * class, or a simple value class such as {@link String}, {@link Integer},
	 * etc.
	 *
	 * @return The query.
	 */
	<X> PersistenceQuery<X> createQuery(String queryText, Class<X> resultClass);

	/**
	 * Create persistent storage update statement.
	 *
	 * @param stmtText The statement text. The statement language is specific to
	 * the persistence service implementation.
	 *
	 * @return The statement.
	 */
	PersistenceUpdate createUpdate(String stmtText);

	/**
	 * Create persistent resource fetch builder. The new fetch builder is
	 * initially configured to fetch all records of the specified persistent
	 * resource without any particular order and without fetching any referred
	 * persistent resource records.
	 *
	 * @param <R> Persistent resource type.
	 * @param prsrcClass Persistent resource class.
	 *
	 * @return The fetch builder.
	 *
	 * @throws IllegalArgumentException If the specified class is not a
	 * registered persistent resource class.
	 */
	<R> PersistentResourceFetch<R> createPersistentResourceFetch(
			Class<R> prsrcClass);

	/**
	 * Create new persistent resource record.
	 *
	 * <p>The method is allowed only for authenticated transactions.
	 *
	 * @param <R> Persistent resource type.
	 * @param prsrcClass Persistent resource class.
	 * @param recTmpl Record template. After the record is created, the template
	 * object is updated: the record id, if the id is auto-generated, and any
	 * record meta-properties are set.
	 *
	 * @throws IllegalArgumentException If the specified class is not a
	 * registered persistent resource class.
	 * @throws UnsupportedOperationException If the transaction is not
	 * authenticated.
	 */
	<R> void persist(Class<R> prsrcClass, R recTmpl);

	/**
	 * Update existing persistent resource record.
	 *
	 * <p>The method is allowed only for authenticated transactions.
	 *
	 * @param <R> Persistent resource type.
	 * @param prsrcClass Persistent resource class.
	 * @param rec The existing record. The record should be loaded before
	 * calling this method, usually using the same transaction. Also, it is
	 * often valid to put some type of lock on it.
	 * @param recTmpl Record template containing new data for the record.
	 * @param updatedProps Set, to which to add paths of the record properties
	 * that were updated, or {@code null} if such information is not needed by
	 * the caller.
	 *
	 * @return {@code true} if any properties were updated.
	 *
	 * @throws IllegalArgumentException If the specified class is not a
	 * registered persistent resource class.
	 * @throws UnsupportedOperationException If the transaction is not
	 * authenticated.
	 */
	<R> boolean update(Class<R> prsrcClass, R rec, R recTmpl,
			Set<String> updatedProps);

	/**
	 * Delete persistent resource records as well as records of any dependent
	 * persistent resources.
	 *
	 * <p>The method is allowed only for authenticated transactions.
	 *
	 * @param <R> Persistent resource type.
	 * @param prsrcClass Persistent resource class.
	 * @param filter Filter that selects records to delete from the entire
	 * persistent resource records collection, or {@code null} to empty the
	 * entire collection.
	 * @param affectedResources Set, to which this method add classes of
	 * persistent resources, records of which were actually deleted, or
	 * {@code null} if such information is not needed by the caller.
	 *
	 * @return {@code true} if any persistent resource records matched the
	 * filter and were deleted.
	 *
	 * @throws IllegalArgumentException If the specified class is not a
	 * registered persistent resource class.
	 * @throws UnsupportedOperationException If the transaction is not
	 * authenticated.
	 */
	<R> boolean delete(Class<R> prsrcClass, FilterSpec<R> filter,
			Set<Class<?>> affectedResources);
}
