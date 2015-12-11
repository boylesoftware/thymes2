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
	 * Tell if the specified property is used by the order specification.
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
