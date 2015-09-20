package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * Character sequence map key length constraint.
 *
 * @author Lev Himmelfarb
 */
@Constraint(validatedBy=MapKeyLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapKeyLength {

	/**
	 * Minimum key length.
	 *
	 * @return Minimum key length.
	 */
	int min() default 0;

	/**
	 * Maximum key length.
	 *
	 * @return Maximum key length.
	 */
	int max() default Integer.MAX_VALUE;

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid map key length.";

	/**
	 * Constraint groups.
	 *
	 * @return Constraint groups.
	 */
	Class<?>[] groups() default {};

	/**
	 * Constraint payload.
	 *
	 * @return Constraint payload.
	 */
	Class<? extends Payload>[] payload() default {};
}
