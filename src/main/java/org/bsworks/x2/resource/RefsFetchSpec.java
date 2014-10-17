package org.bsworks.x2.resource;

import java.util.Map;


/**
 * Specification of a referred persistent resource records fetch.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface RefsFetchSpec<R> {

	/**
	 * Get class of the persistent resource at the root of the fetch.
	 *
	 * @return Root persistent resource class.
	 */
	Class<R> getPersistentResourceClass();

	/**
	 * Add reference property to the fetch.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The path may contain several
	 * references in it, in which case the intermediate references are also
	 * added to the fetch. The last property in the path must be a reference.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws IllegalArgumentException If the specified property path is
	 * invalid.
	 */
	RefsFetchSpec<R> add(String propPath);

	/**
	 * Tell if the specified reference property needs to be fetched. This is
	 * equivalent to calling
	 * {@code getFetchedRefProperties().containsKey(propPath)}.
	 *
	 * @param propPath Property path with nested properties separated with dots.
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
	Map<String, Class<?>> getFetchedRefProperties();
}
