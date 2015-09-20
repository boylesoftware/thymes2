package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * Character sequence map value length constraint.
 *
 * @author Lev Himmelfarb
 */
@Constraint(validatedBy=MapValueLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapValueLength {

	/**
	 * Minimum value length.
	 *
	 * @return Minimum value length.
	 */
	int min() default 0;

	/**
	 * Maximum value length.
	 *
	 * @return Maximum value length.
	 */
	int max() default Integer.MAX_VALUE;

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid map value length.";

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
