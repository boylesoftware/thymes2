package org.bsworks.x2.resource;

import java.util.List;
import java.util.Set;


/**
 * Specification for a persistent resource records fetch sorting order.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface OrderSpec<R> {

	/**
	 * Get class of the persistent resource at the root of the fetch.
	 *
	 * @return Root persistent resource class.
	 */
	Class<R> getPersistentResourceClass();

	/**
	 * Get order specification elements.
	 *
	 * @return Unmodifiable list of order specification elements. May be empty,
	 * but never {@code null}.
	 */
	List<OrderSpecElement> getElements();

	/**
	 * Tell if the order specification is empty, that is no ordering rule has
	 * been added to it.
	 *
	 * @return {@code true} if empty.
	 */
	boolean isEmpty();

	/**
	 * Tell if the specified property is used by the order specification. The
	 * method also returned {@code true} for parent paths of properties used in
	 * any order specification elements.
	 *
	 * <p>If the specified property path ends with ".*", it is considered a
	 * template and the method checks if any of the specified path's nested
	 * property is used by the filter, but not the property itself.
	 *
	 * @param propPath Property path to check.
	 *
	 * @return {@code true} if used.
	 */
	boolean isUsed(String propPath);

	/**
	 * Get all persistent resources that participate in the ordering, not
	 * including the root resource returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return Unmodifiable set of participating persistent resource classes.
	 */
	Set<Class<?>> getParticipatingPersistentResources();
}
