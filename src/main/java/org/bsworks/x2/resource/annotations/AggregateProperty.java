package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.ResourcePropertyAccess;


/**
 * Aggregate property. The annotation is used to mark a transient property of a
 * persistent resource that contains result of aggregation of a collection
 * property of the same resource or a resource down the properties chain. The
 * value of the aggregated collection may be a nested object or a reference to
 * another resource or dependent resource. Immediate, simple single-valued
 * properties of the aggregated resource are used in an aggregation value
 * expression that is passed to the selected aggregated function.
 *
 * <p>An aggregate property is considered transient (not persistent) and does
 * never participate in any updates. When the resource record is loaded from the
 * persistent storage, aggregate properties are not calculated and are not
 * included unless explicitly requested in the provided properties fetch
 * specification.
 *
 * <p>The type of the aggregate property depends on the aggregate function and
 * the type of its parameter (that is types of the properties used for
 * calculating the aggregation). It should not be a primitive type to allow
 * {@code null}, so that it can be excluded from the resource serialized form
 * passed over the API when the aggregation is not requested.
 *
 * <p>Aggregate property can be a single-valued property or a map, in which case
 * the aggregation is performed for each key in the map in addition to the
 * containing record id. Aggregate property may not be a non-map collection.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AggregateProperty {

	/**
	 * Property access restrictions. Unless explicitly restricted, all types of
	 * access are allowed. The only type of access that makes sense and is
	 * allowed to be restricted is {@link ResourcePropertyAccess#SEE}.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * Path to the collection property to aggregate. The path may contain
	 * intermediate references and dependent resource references. Each element
	 * of the path must represent a persistent property. The first element of
	 * the path must be a property of the resource, which contains the annotated
	 * aggregate property. The last property in the path must be a collection
	 * with values that can be a nested object, a reference or a dependent
	 * resource reference.
	 *
	 * @return The aggregated collection property path.
	 */
	String collection();

	/**
	 * The aggregation function.
	 *
	 * @return The aggregate function.
	 */
	AggregationFunction func();

	/**
	 * Expression for the value passed to the aggregation function to calculate
	 * the final aggregated property result. The expression is a simple
	 * arithmetic expression that can use names of properties of the aggregated
	 * object (specified by the {@link #collection} annotation attribute), four
	 * arithmetic operators (+, -, * and /) and parenthesis. The property
	 * references can also be dot-separated paths traversing references and
	 * nested objects. All of them must be in the same chain of nested
	 * properties though.
	 *
	 * <p>If the {@link #func} is {@link AggregationFunction#COUNT}, the value
	 * expression may be left unspecified, in which case the number of the
	 * aggregated collection elements is counted.
	 *
	 * @return The aggregated value expression.
	 */
	String valueExpression() default "";

	/**
	 * For a map aggregate property, name of the aggregated object property used
	 * as the key in the map. Must be specified if the property is a map.
	 *
	 * @return The map key property name.
	 */
	String key() default "";
}
