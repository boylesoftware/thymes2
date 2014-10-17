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
	 * If the filter specification was created using {@link #addConjunction()}
	 * or {@link #addDisjunction()}, get the parent filter specification. The
	 * method is useful for building call chains during filter specification
	 * construction.
	 *
	 * @return Parent filter specification, or {@code null} if top-level.
	 */
	FilterSpec<R> getParent();

	/**
	 * Add nested logical conjunction ("AND") to the filter specification.
	 *
	 * @return Empty conjunction specification.
	 */
	FilterSpec<R> addConjunction();

	/**
	 * Add nested logical disjunction ("OR") to the filter specification.
	 *
	 * @return Empty disjunction specification.
	 */
	FilterSpec<R> addDisjunction();

	/**
	 * Add filter condition.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. If the property at the end of the
	 * path is a nested object, the only filter types accepted are
	 * {@link FilterConditionType#EMPTY} and
	 * {@link FilterConditionType#NOT_EMPTY}. If the path contains multi-valued
	 * properties (collections and maps), the condition checks if the collection
	 * contains at least a single element that satisfies the condition, or, if
	 * negated, does not contain such element. Multiple conditions using the
	 * same collection within a single filter specification object are assumed
	 * to be for the same element in the collection. If the property at the end
	 * of the path is a reference, it can be suffixed with "/id" to use the
	 * referred resource record id as the value to test. Otherwise, the
	 * reference is treated as a nested object. The path may contain several
	 * intermediate references. If the property at the end of the path is a map,
	 * it can be suffixed with "/key" to use the element key as the value to
	 * test.
	 * @param type The condition type.
	 * @param negate {@code true} to negate the condition. Mostly useful if the
	 * property path contains collections.
	 * @param operands Operands, against which the condition tests the property
	 * value. {@link FilterConditionType#EMPTY} and
	 * {@link FilterConditionType#NOT_EMPTY} conditions do not take any
	 * operands. All other condition types require at least one operand. If more
	 * than one is specified, the condition test results are combined using
	 * logical disjunction ("OR"). The operand types must be either compatible
	 * with the property, or be instances of {@link String}, in which case
	 * {@link ResourcePropertyValueHandler#valueOf(String)} is used to convert
	 * the value. Operands, if specified, may not be {@code null}.
	 *
	 * @return This filter specification (for chaining).
	 *
	 * @throws IllegalArgumentException If something is wrong with the condition
	 * specification.
	 */
	FilterSpec<R> addCondition(String propPath, FilterConditionType type,
			boolean negate, Object... operands);

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
