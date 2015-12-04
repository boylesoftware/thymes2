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
	 */
	boolean isIncluded(String propPath);

	/**
	 * Tell if the specified reference property is fetched by this
	 * specification. If the property path is not a template (see "propPath"
	 * parameter description), this is equivalent to calling
	 * {@code getFetchedRefProperties().containsKey(propPath)}.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * If ends with ".*", then checks if any nested properties of the specified
	 * path are fetched.
	 *
	 * @return {@code true} if needs to be fetched.
	 */
	boolean isFetchRequested(String propPath);

	/**
	 * Get all reference properties fetched by this specification with the
	 * reference target classes.
	 *
	 * @return Unmodifiable map referred persistent resource classes by
	 * reference property paths.
	 */
	SortedMap<String, Class<?>> getFetchedRefProperties();
}
