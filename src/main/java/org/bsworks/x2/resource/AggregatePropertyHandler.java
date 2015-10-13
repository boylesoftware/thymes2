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
	 * Get the longest property path used in the aggregated value expression.
	 * The path includes the aggregated collection property path (the path
	 * returned by the {@link #getAggregatedCollectionPropertyPath()}
	 * method) and is equal to it if the value expression is empty or uses no
	 * nested properties.
	 *
	 * @return Aggregated resource property path.
	 */
	String getDeepAggregatedResourcePropertyPath();

	/**
	 * Get level of the deepest aggregated resource used in the aggregation
	 * value expression. If path returned by
	 * {@link #getDeepAggregatedResourcePropertyPath()} is the same as the path
	 * returned by {@link #getAggregatedCollectionPropertyPath()}, that is no
	 * value expression or the value expression uses only direct properties of
	 * the aggregated collection resource, the depth value is zero.
	 *
	 * @return Aggregated depth.
	 */
	int getAggregationDepth();

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
	 * dependent resource reference property in the path returned by the
	 * {@link #getDeepAggregatedResourcePropertyPath()} method.
	 *
	 * @return Reference property path, or {@code null} if the deepest
	 * aggregated resource can be reached without fetching any intermediate
	 * references.
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
	 * Get regular expression matcher for all property references in the
	 * aggregation value expression.
	 *
	 * @return Initialized property references matcher, or {@code null} if no
	 * value expression.
	 */
	Matcher getValuePropertiesMatcher();

	/**
	 * Get name of the key property in the aggregated object if the aggregate
	 * property is a map.
	 *
	 * @return Key property name, or {@code null} if not a map.
	 */
	String getKeyPropertyName();

	/**
	 * Get paths of properties used in the aggregation value expression plus the
	 * key property, if any.
	 *
	 * @return Unmodifiable set of property paths relative to the aggregated
	 * collection.
	 */
	Set<String> getAggregatedPropertyPaths();
}
