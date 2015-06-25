package org.bsworks.x2.resource;

import java.util.Deque;


/**
 * Order specification element for a single property.
 *
 * @author Lev Himmelfarb
 */
public interface OrderSpecElement {

	/**
	 * Get sort direction.
	 *
	 * @return The sort direction.
	 */
	SortDirection getSortDirection();

	/**
	 * Get path of the used property.
	 *
	 * @return The property path.
	 */
	String getPropertyPath();

	/**
	 * Get function for the value used for sorting.
	 *
	 * @return The value transformation function.
	 */
	PropertyValueFunction getValueFunction();

	/**
	 * Get value function parameters.
	 *
	 * @return The parameters, specific for the function returned by
	 * {@link #getValueFunction()}. May be empty but never {@code null}.
	 */
	Object[] getValueFunctionParams();

	/**
	 * Get chain of property handlers leading to the used property.
	 *
	 * @return The property handlers chain. The first element is handler of a
	 * top persistent resource property. The last element is handler of the
	 * property used for ordering.
	 */
	Deque<? extends ResourcePropertyHandler> getPropertyChain();
}
