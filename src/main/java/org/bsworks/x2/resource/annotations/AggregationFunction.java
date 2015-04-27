package org.bsworks.x2.resource.annotations;


/**
 * Aggregate functions.
 *
 * @author Lev Himmelfarb
 */
public enum AggregationFunction {

	/**
	 * Count number of records.
	 */
	COUNT,

	/**
	 * Count number of distinct values.
	 */
	COUNT_DISTINCT,

	/**
	 * Calculate sum of the values.
	 */
	SUM,

	/**
	 * Find minimum value.
	 */
	MIN,

	/**
	 * Find maximum value.
	 */
	MAX,

	/**
	 * Calculate average value.
	 */
	AVG
}
