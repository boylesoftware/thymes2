package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Resource property. The annotation is used with three types of resource
 * properties: simple value, such as a string or a number, nested object, or
 * reference to another persistent resource. Each can be a collection or a map.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Property {

	/**
	 * Property access restrictions. Unless explicitly restricted, all types of
	 * access are allowed.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * If {@code true}, which is the default, the property value is set to
	 * {@code null} when an existing resource is being updated and in the
	 * incoming update data the property is {@code null}. If {@code false} and
	 * in the incoming update data the property is {@code null}, the property in
	 * the existing resource is left unchanged.
	 *
	 * @return {@code true} to set to {@code null}, {@code false} to leave
	 * unchanged.
	 */
	boolean updateIfNull() default true;

	/**
	 * Property persistence attributes. By default, the property is transient
	 * and is not stored in the persistent storage.
	 *
	 * @return Persistence attributes.
	 */
	Persistence persistence() default @Persistence(persistent=false);

	/**
	 * Possible concrete value types for a polymorphic nested object property.
	 *
	 * @return The value type descriptors.
	 */
	ValueType[] valueTypes() default {};
}
