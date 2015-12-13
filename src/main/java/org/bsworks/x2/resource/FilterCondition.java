package org.bsworks.x2.resource;

import java.util.Collection;
import java.util.Deque;


/**
 * Filter condition.
 *
 * @author Lev Himmelfarb
 */
public interface FilterCondition {

	/**
	 * Get condition type.
	 *
	 * @return The condition type.
	 */
	FilterConditionType getType();

	/**
	 * Tell if the condition is negated.
	 *
	 * <p>There is an important difference between a negated condition and a
	 * condition that uses an inverse condition type (see
	 * {@link FilterConditionType#inverse()}). The difference only manifests
	 * itself when the tested property path includes any collections. When a
	 * negated condition is included, its meaning is interpreted as "records
	 * with the collection that does not contain elements that satisfy the
	 * condition". The a non-negated inverse condition is included, it is
	 * interpreted as "records with the collection that contains elements that
	 * do not satisfy the condition".
	 *
	 * @return {@code true} if negated.
	 */
	boolean isNegated();

	/**
	 * Get path of the property tested by the condition.
	 *
	 * @return The property path.
	 */
	String getPropertyPath();

	/**
	 * Get type of the property value tested by the condition.
	 *
	 * @return The property value type. Cannot be
	 * {@link FilterConditionOperandType#CONSTANT}.
	 */
	FilterConditionOperandType getPropertyValueType();

	/**
	 * Get function for the tested property value transformation.
	 *
	 * @return The value transformation function.
	 */
	PropertyValueFunction getValueFunction();

	/**
	 * Get tested property value transformation function parameters.
	 *
	 * @return The parameters, specific for the function returned by
	 * {@link #getValueFunction()}. May be empty but never {@code null}.
	 */
	Object[] getValueFunctionParams();

	/**
	 * Get chain of property handlers leading to the property tested by the
	 * condition.
	 *
	 * @return The property handlers chain. The first element is handler of a
	 * top persistent resource property. The last element is handler of the
	 * property tested by the condition.
	 */
	Deque<? extends ResourcePropertyHandler> getPropertyChain();

	/**
	 * Get condition operands.
	 *
	 * @return Unmodifiable collection of condition operands. May be empty for
	 * certain condition types, but never {@code null}.
	 */
	Collection<? extends FilterConditionOperand> getOperands();
}
