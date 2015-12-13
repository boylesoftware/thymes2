package org.bsworks.x2.resource;


/**
 * Builder for a {@link FilterSpec}. The builder extends the {@link FilterSpec}
 * and can be used as such.
 *
 * <p><em>Note, that builder instances must not be assumed to be
 * thread-safe!</em>
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface FilterSpecBuilder<R>
	extends FilterSpec<R> {

	/**
	 * If the filter is a conjunction, make it disjunction. If already a
	 * disjunction, do nothing.
	 *
	 * @return This filter specification (for chaining).
	 */
	FilterSpecBuilder<R> makeDisjunction();

	/**
	 * If the filter specification was created using {@link #addConjunction()}
	 * or {@link #addDisjunction()}, get the parent filter specification. The
	 * method is useful for building call chains during filter specification
	 * construction.
	 *
	 * @return Parent filter specification, or {@code null} if top-level.
	 */
	FilterSpecBuilder<R> getParent();

	/**
	 * Add nested logical conjunction ("AND") to the filter specification.
	 *
	 * @return Empty conjunction specification.
	 */
	FilterSpecBuilder<R> addConjunction();

	/**
	 * Add nested logical disjunction ("OR") to the filter specification.
	 *
	 * @return Empty disjunction specification.
	 */
	FilterSpecBuilder<R> addDisjunction();

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
	 * @param func Property value transformation function.
	 * @param funcParams Parameters for the value transformation function. May
	 * be {@code null} if the function takes no parameters.
	 * @param type The condition type.
	 * @param negate {@code true} to negate the condition. Mostly useful if the
	 * property path contains collections (see notes for
	 * {@link FilterCondition#isNegated()}).
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
	 * @throws InvalidSpecificationException If something is wrong with the
	 * condition specification.
	 */
	FilterSpecBuilder<R> addCondition(String propPath,
			PropertyValueFunction func, Object[] funcParams,
			FilterConditionType type, boolean negate, Object... operands);

	/**
	 * Shortcut for
	 * {@link #addCondition(String, PropertyValueFunction, Object[], FilterConditionType, boolean, Object...)}
	 * with value function {@link PropertyValueFunction#PLAIN} and "negate"
	 * parameter {@code false}.
	 *
	 * @param propPath Property path.
	 * @param type Condition type.
	 * @param operands Condition operands.
	 *
	 * @return This filter specification (for chaining).
	 *
	 * @throws InvalidSpecificationException If something is wrong with the
	 * condition specification.
	 */
	FilterSpecBuilder<R> addTrueCondition(String propPath,
			FilterConditionType type, Object... operands);
}
