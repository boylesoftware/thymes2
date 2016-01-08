package org.bsworks.x2.services.persistence;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceFetchResult;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeSpec;


/**
 * Represents a sophisticated persistent storage query builder used to fetch
 * persistent resource records from the storage.
 *
 * @param <R> Persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PersistentResourceFetch<R> {

	/**
	 * Specify what persistent resource properties to include in or exclude from
	 * the fetch as well as what referred resources to include in the fetch. If
	 * left unset, all properties that are fetched by default are included and
	 * no referred resources are fetched.
	 *
	 * @param propsFetch The properties fetch specification.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setPropertiesFetch(
			PropertiesFetchSpec<R> propsFetch);

	/**
	 * Set condition for restricting the resource selection. By default, all
	 * records are fetched.
	 *
	 * @param filter The filter specification.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setFilter(FilterSpec<R> filter);

	/**
	 * Set result list order. By default, the order is undefined.
	 *
	 * @param order The order specification.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setOrder(OrderSpec<R> order);

	/**
	 * Set range for limiting the resource selection. By default, all records
	 * are fetched.
	 *
	 * @param range The range specification.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setRange(RangeSpec range);

	/**
	 * Tell the fetch to lock the fetched persistent resource records. The lock
	 * is held until the end of the transaction.
	 *
	 * <p>Note, that some complex fetches may place a shared lock implicitly
	 * even if not requested by this method. However, if the caller knows that
	 * it needs a shared lock on the result, it must request it explicitly.
	 *
	 * @param lockType Lock type.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> lockResult(LockType lockType);

	/**
	 * Get number of records that would be returned by the {@link #getResult()}
	 * method. Note, that the range, if set, is not taken into the account.
	 *
	 * @return Number of records.
	 */
	long getCount();

	/**
	 * Execute the fetch and get the result.
	 *
	 * @return The fetch result.
	 */
	PersistentResourceFetchResult<R> getResult();

	/**
	 * Execute the fetch and get the resource specific result.
	 *
	 * @param fetchResultClass Resource specific fetch result class.
	 *
	 * @return The fetch result.
	 *
	 * @throws ClassCastException If the specified fetch result class does not
	 * match the one specified for the persistent resource.
	 */
	<C extends PersistentResourceFetchResult<R>> C getResult(
			Class<C> fetchResultClass);

	/**
	 * Execute the fetch and get the first fetched record.
	 *
	 * @return The first fetched record, or {@code null} if none.
	 */
	R getSingleResult();
}
