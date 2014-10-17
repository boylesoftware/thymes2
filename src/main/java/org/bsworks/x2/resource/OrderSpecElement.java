package org.bsworks.x2.resource;

import java.util.Deque;


/**
 * Order specification element for a single property.
 *
 * @author Lev Himmelfarb
 */
public interface OrderSpecElement {

	/**
	 * Get order type.
	 *
	 * @return The order type.
	 */
	OrderType getType();

	/**
	 * Get path of the used property.
	 *
	 * @return The property path.
	 */
	String getPropertyPath();

	/**
	 * Get chain of property handlers leading to the used property.
	 *
	 * @return The property handlers chain. The first element is handler of a
	 * top persistent resource property. The last element is handler of the
	 * property used for ordering.
	 */
	Deque<? extends ResourcePropertyHandler> getPropertyChain();
}
