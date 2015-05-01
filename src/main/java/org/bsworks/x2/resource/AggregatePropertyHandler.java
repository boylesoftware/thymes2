package org.bsworks.x2.resource;

import java.util.Set;
import java.util.regex.Matcher;

import org.bsworks.x2.resource.annotations.AggregationFunction;


/**
 * Persistent resource aggregate property handler.
 *
 * @author Lev Himmelfarb
 */
public interface AggregatePropertyHandler
	extends ResourcePropertyHandler {

	/**
	 * Get aggregated collection property path.
	 *
	 * @return Aggregated collection property path.
	 */
	String getAggregatedCollectionPropertyPath();

	/**
	 * Get classes of persistent resources that participate in the calculation
	 * of the aggregate property value, excluding the persistent resource, which
	 * contains the aggregate property.
	 *
	 * @return Unmodifiable set of persistent resource classes used in the
	 * aggregated collection property path. May be empty, but never
	 * {@code null}.
	 */
	Set<Class<?>> getUsedPersistentResourceClasses();

	/**
	 * Get path of the last (the longest path) intermediate reference or
	 * dependent resource reference property in the aggregated collection path.
	 *
	 * @return Reference property path, or {@code null} if the collection
	 * property can be reached without fetching any intermediate references.
	 */
	String getLastIntermediateRefPath();

	/**
	 * Get aggregation function.
	 *
	 * @return The aggregation function.
	 */
	AggregationFunction getFunction();

	/**
	 * Get handler of the aggregated collection property. The handler is the
	 * container of the properties used in the aggregation value expression.
	 *
	 * @return Aggregated collection handler.
	 */
	ResourcePropertiesContainer getAggregatedCollectionHandler();

	/**
	 * Get aggregation value expression.
	 *
	 * @return The aggregation value expression. May be {@code null} if the
	 * aggregation function is {@link AggregationFunction#COUNT}. Otherwise,
	 * never {@code null}.
	 */
	String getAggregationValueExpression();

	/**
	 * Get names of properties used in the aggregation value expression.
	 *
	 * @return Unmodifiable set of property names.
	 */
	Set<String> getUsedValuePropertyNames();

	/**
	 * Get regular expression matcher for all property names in the aggregation
	 * value expression.
	 *
	 * @return Initialized property references matcher, or {@code null} if no
	 * value expression.
	 */
	Matcher getValuePropertiesMatcher();
}
