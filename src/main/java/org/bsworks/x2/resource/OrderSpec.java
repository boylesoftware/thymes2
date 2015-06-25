package org.bsworks.x2.resource;

import java.util.List;
import java.util.Set;


/**
 * Specification of sorting order for a persistent resource records fetch.
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
	 * Add ordering by the specified property.
	 *
	 * @param orderType Order type.
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The math may not contain any
	 * collection or map properties. The property at the end of the path may not
	 * be a nested object. It also may not be a reference, unless it is suffixed
	 * with "/id", in which case the referred record id is used for ordering.
	 * The path may contain several intermediate references.
	 *
	 * @return This order specification object (for chaining).
	 *
	 * @throws IllegalArgumentException If the specified property path is
	 * invalid.
	 */
	OrderSpec<R> add(OrderType orderType, String propPath);

	/**
	 * Add segmentation by the specified condition. Adding a segment splits the
	 * result list in two: all records, for which the condition is false appear
	 * first, followed by all records, for which the condition is true. This is
	 * achieved by including ordering by the result of the condition evaluation.
	 * Multiple segments can be added to create sub-segmentation.
	 *
	 * <p>Note, that segmentation is always performed before sorting specified
	 * by the {@link #add(OrderType, String)} method calls.
	 *
	 * @param split Filter specification that expresses the segment split
	 * condition. Records included by the filter will appear after the records
	 * excluded by the filter.
	 *
	 * @return This order specification object (for chaining).
	 *
	 * @throws IllegalArgumentException If the specified property path is
	 * invalid.
	 */
	OrderSpec<R> addSegment(FilterSpec<R> split);

	/**
	 * Get order specification elements.
	 *
	 * @return Unmodifiable list of order specification elements. May be empty,
	 * but never {@code null}.
	 */
	List<? extends OrderSpecElement> getElements();

	/**
	 * Get segments.
	 *
	 * @return Unmodifiable list of segment splits. May be empty, but never
	 * {@code null}.
	 */
	List<FilterSpec<R>> getSegments();

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
