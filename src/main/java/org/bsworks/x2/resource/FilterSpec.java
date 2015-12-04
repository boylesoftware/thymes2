package org.bsworks.x2.resource;

import java.util.Collection;
import java.util.Set;


/**
 * Specification of a filter for a persistent resource records fetch.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface FilterSpec<R> {

	/**
	 * Get class of the persistent resource at the root of the fetch.
	 *
	 * @return Root persistent resource class.
	 */
	Class<R> getPersistentResourceClass();

	/**
	 * Tell if the conditions in the filter specification are combined using
	 * logical conjunction ("AND") or disjunction ("OR").
	 *
	 * @return {@code true} if disjunction ("OR"), {@code false} if conjunction
	 * ("AND").
	 */
	boolean isDisjunction();

	/**
	 * Get filter conditions.
	 *
	 * @return Unmodifiable collection of conditions. May be empty, never
	 * {@code null}.
	 */
	Collection<? extends FilterCondition> getConditions();

	/**
	 * Get nested con- and disjunctions.
	 *
	 * @return Unmodifiable collection of nested con- and disjunctions. May be
	 * empty, never {@code null}.
	 */
	Collection <? extends FilterSpec<R>> getJunctions();

	/**
	 * Tell if the filter specification is empty, that is no condition was ever
	 * added to it or to any of its nested con- or disjunctions.
	 *
	 * @return {@code true} if empty.
	 */
	boolean isEmpty();

	/**
	 * Tell if the specified property is used by the filter, including by any of
	 * its nested con- and disjunctions.
	 *
	 * @param propPath Property path to check.
	 *
	 * @return {@code true} if used.
	 */
	boolean isUsed(String propPath);

	/**
	 * Tell if the only property used by the filter is the persistent record id,
	 * including any of the nested con- and disjunctions.
	 *
	 * @return {@code true} if only the record id is used by the filter.
	 */
	boolean isByIdOnly();

	/**
	 * Get all persistent resources that participate in the filter, not
	 * including the root resource returned by
	 * {@link #getPersistentResourceClass()}, but including all resources from
	 * the nested con- and disjunctions.
	 *
	 * @return Unmodifiable set of participating persistent resource classes.
	 */
	Set<Class<?>> getParticipatingPersistentResources();
}
