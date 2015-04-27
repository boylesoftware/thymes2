package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Dependent persistent resource aggregate property. The annotation is used to
 * mark a property of a persistent resource that contains result of aggregation
 * of a dependent persistent resource records associated with this resource
 * record. It is similar to {@link DependentRefProperty}, but instead of
 * fetching the reference value, when the aggregate property is requested to be
 * fetched, the associated dependent resource records are aggregated using the
 * specified in the annotation aggregation function and the result is stored in
 * the property.
 *
 * <p>An aggregate property does never participate in any updates, that is it is
 * always read-only. When the resource record is loaded from the persistent
 * storage, aggregate properties are not included unless explicitly requested.
 * If aggregate property is {@code null}, it is not included when the record is
 * serialized.
 *
 * <p>The type of the property marked with this annotation may not be a
 * collection or a map. The type depends on the aggregate function and the type
 * of its parameter (that is type of the dependent resource property used for
 * calculating the aggregation). It should not be a primitive type to allow
 * {@code null}.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependentAggregateProperty {

	/**
	 * Property access restrictions. Unless explicitly restricted, all types of
	 * access are allowed.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * Tells if associated records of the dependent resource may not exist. The
	 * default is {@code false}. See {@link Persistence#optional()} for more
	 * details.
	 *
	 * @return {@code true} if optional.
	 */
	boolean optional() default false;

	/**
	 * Dependent persistent resource class.
	 *
	 * @return Dependent persistent resource class.
	 */
	Class<?> resourceClass();

	/**
	 * Name of the property in the referred dependent resource that points back
	 * to this resource.
	 *
	 * @return The reverse reference property name.
	 */
	String reverseRefProperty();

	/**
	 * The aggregation function.
	 *
	 * <p>In the case of {@link AggregationFunction#COUNT},
	 * {@link #aggregationProperty} may be left unspecified, in which case the
	 * number of dependent resource record is counted. If it is specified, the
	 * number of non-null values is counted. All other aggregation functions
	 * require {@link #aggregationProperty} to be specified.</p>
	 *
	 * @return The aggregate function.
	 */
	AggregationFunction func();

	/**
	 * Name of the property in the referred dependent resource that is used for
	 * the aggregation. Must be specified unless the function is
	 * {@link AggregationFunction#COUNT}.
	 *
	 * @return The aggregation value property name.
	 */
	String aggregationProperty() default "";
}
