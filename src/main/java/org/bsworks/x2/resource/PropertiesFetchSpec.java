package org.bsworks.x2.resource;

import java.util.SortedMap;


/**
 * Specification of what persistent resource properties to include in a fetch.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PropertiesFetchSpec<R> {

	/**
	 * Get class of the persistent resource at the root of the fetch.
	 *
	 * @return Root persistent resource class.
	 */
	Class<R> getPersistentResourceClass();

	/**
	 * Tell if the specified property needs to be included in the fetch.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 *
	 * @return {@code true} if needs to be included.
	 *
	 * @throws IllegalArgumentException If the property path is invalid.
	 */
	boolean isIncluded(String propPath);

	/**
	 * Tell if the specified reference property is requested to be fetched by
	 * this specification. If the property path is not a template (see
	 * "propPath" parameter description), this is equivalent to calling
	 * {@code getFetchedRefProperties().containsKey(propPath)}.
	 *
	 * @param propPath Reference property path with nested properties separated
	 * with dots. If ends with ".*", then checks if any nested property of the
	 * specified path is fetched.
	 *
	 * @return {@code true} if needs to be fetched.
	 */
	boolean isFetchRequested(String propPath);

	/**
	 * Get all reference properties fetched by this specification with the
	 * reference target classes.
	 *
	 * @return Unmodifiable map with referred persistent resource classes by
	 * reference property paths. Never {@code null}, but can be empty if no
	 * fetches have been requested.
	 */
	SortedMap<String, Class<?>> getFetchedRefProperties();

	/**
	 * Get filter used to limit resource records participating in the
	 * calculation of the specified aggregate property value.
	 *
	 * <p>The returned filter specification is based at the persistent resource,
	 * but all properties used in all filter conditions are nested properties in
	 * the aggregated collection.
	 *
	 * @param propPath Aggregate property path.
	 *
	 * @return The filter, or {@code null} if no filter.
	 */
	FilterSpec<R> getAggregateFilter(String propPath);
}
