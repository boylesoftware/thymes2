package org.bsworks.x2.services.persistence;

import java.util.List;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeResult;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.RefsFetchResult;
import org.bsworks.x2.resource.RefsFetchSpec;


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
	 * the fetch. By default, all properties are fetched.
	 *
	 * @param propsFetch The properties fetch specification.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setPropertiesFetch(
			PropertiesFetchSpec<R> propsFetch);

	/**
	 * Set condition for restricting the resource selection. By default, all
	 * records are returned.
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
	 * are returned.
	 *
	 * @param range The range specification.
	 * @param rangeResult Object that receives meta-data about the records
	 * collection upon the fetch execution. May be {@code null} if no such data
	 * is needed.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setRange(RangeSpec range,
			RangeResult rangeResult);

	/**
	 * Specify what other referred persistent resource records need to be
	 * fetched. By default, no references are resolved.
	 *
	 * @param refsFetch The references specification.
	 * @param refsResult Object that receives the fetched referred persistent
	 * resource records.
	 *
	 * @return This fetch builder.
	 */
	PersistentResourceFetch<R> setRefsFetch(RefsFetchSpec<R> refsFetch,
			RefsFetchResult refsResult);

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
	 * Get number of records that would be returned by the
	 * {@link #getResultList()} method. Note, that the range, if set, is not
	 * taken into the account.
	 *
	 * @return Number of records.
	 */
	long getCount();

	/**
	 * Execute the fetch and get the result list.
	 *
	 * @return Unmodifiable result list. Never {@code null}, but may be empty.
	 */
	List<R> getResultList();

	/**
	 * Execute the fetch and return the first fetched result.
	 *
	 * @return The result, or {@code null} if none.
	 */
	R getFirstResult();
}
