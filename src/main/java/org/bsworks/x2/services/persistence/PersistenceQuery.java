package org.bsworks.x2.services.persistence;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.ResourceReadSessionCache;


/**
 * Represents a query used to fetch data from the persistence storage.
 *
 * @param <X> Query result type.
 *
 * @author Lev Himmelfarb
 */
public interface PersistenceQuery<X> {

	/**
	 * Set session cache instance to use with the query.
	 *
	 * @param sessionCache Session cache instance.
	 *
	 * @return This query.
	 */
	PersistenceQuery<X> setSessionCache(ResourceReadSessionCache sessionCache);

	/**
	 * Set query parameter.
	 *
	 * @param paramName Parameter name.
	 * @param paramType Parameter value type.
	 * @param paramValue Parameter value. May be {@code null}.
	 *
	 * @return This query.
	 */
	PersistenceQuery<X> setParameter(String paramName,
			PersistentValueType paramType, Object paramValue);

	/**
	 * Set query result list range.
	 *
	 * <p>Note, that some persistence service implementations (e.g. those that
	 * use SQL as the query language) may disallow using this method for queries
	 * that include fetching nested collections. In the case of SQL, for
	 * example, this is because the database returns multiple rows for each
	 * result element depending on the number of element in the nested
	 * collection. Since the length of the nested collection is variable and
	 * unpredictable, it is impossible to correctly position the result set
	 * cursor. To solve this problem, multiple queries can be used to first
	 * select the main records into a temporary table, and then join the nested
	 * collection table in a separate query.
	 *
	 * @param firstRecord Zero-based index of the first record to include in the
	 * result list.
	 * @param maxRecords Maximum number of records to include in the result
	 * list.
	 *
	 * @return This query.
	 */
	PersistenceQuery<X> setResultRange(int firstRecord, int maxRecords);

	/**
	 * Execute the query and get the result list.
	 *
	 * @param refsFetchResult If the query contains instructions to fetch
	 * referred persistent resource records from other persistent collections,
	 * this map receives the resulting records by string representations of the
	 * corresponding references. May be {@code null} if no referred records
	 * fetch is needed.
	 *
	 * @return Unmodifiable result list. Never {@code null}, but may be empty.
	 */
	List<X> getResultList(Map<String, Object> refsFetchResult);

	/**
	 * Execute the query that returns a single result.
	 *
	 * @param refsFetchResult If the query contains instructions to fetch
	 * referred persistent resource records from other persistent collections,
	 * this map receives the resulting records by string representations of the
	 * corresponding references. May be {@code null} if no referred records
	 * fetch is needed.
	 *
	 * @return The result, or {@code null} if none.
	 */
	X getFirstResult(Map<String, Object> refsFetchResult);
}
