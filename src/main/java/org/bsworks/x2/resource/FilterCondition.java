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
