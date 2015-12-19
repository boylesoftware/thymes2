package org.bsworks.x2.resource;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;


/**
 * Specification for a persistent resource records fetch filter.
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
	 * Get paths of all properties <em>directly</em> used in all conditions of
	 * the filter, recursively including nested con- and disjunctions. The set
	 * does not include parent paths in of nested properties.
	 *
	 * @return Unmodifiable set of property paths. Never {@code null}, but may
	 * be empty (in which case {@link #isEmpty()} also returns {@code true}).
	 */
	SortedSet<String> getUsedProperties();

	/**
	 * Tell if the filter specification is empty, that is no condition was ever
	 * added to it or to any of its nested con- or disjunctions. This is
	 * practically a shortcut for {@code getUsedProperties().isEmpty()}.
	 *
	 * @return {@code true} if empty.
	 */
	boolean isEmpty();

	/**
	 * Tell if the specified property is used by the filter, including by any of
	 * its nested con- and disjunctions. The method also returns {@code true}
	 * for parent paths of any property directly used in a condition, which
	 * makes it different from {@code getUsedProperties().contains(propPath)}.
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
