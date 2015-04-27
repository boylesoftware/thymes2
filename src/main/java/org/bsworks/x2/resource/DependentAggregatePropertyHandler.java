package org.bsworks.x2.resource;

import org.bsworks.x2.resource.annotations.AggregationFunction;


/**
 * Dependent persistent resource aggregate property handler.
 *
 * @author Lev Himmelfarb
 */
public interface DependentAggregatePropertyHandler
	extends DependentResourcePropertyHandler {

	/**
	 * Get aggregation function.
	 *
	 * @return The aggregation function.
	 */
	AggregationFunction getFunction();

	/**
	 * Get name of the property in the referred dependent resource class that is
	 * used for the aggregation.
	 *
	 * @return Name of the property. The property is always single-valued,
	 * simple value property that is not a reference nor a nested object. May be
	 * {@code null} if the aggregation function is
	 * {@link AggregationFunction#COUNT}. Otherwise, never {@code null}.
	 */
	String getAggregationPropertyName();
}
