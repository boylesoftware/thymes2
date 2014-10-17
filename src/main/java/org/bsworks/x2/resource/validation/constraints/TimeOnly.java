package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;


/**
 * Time (without date, without seconds) validation constraint. The constraint is
 * applicable to string fields (or collections of strings) and makes sure that
 * the string is in format "HH:mm".
 *
 * @author Lev Himmelfarb
 */
@Pattern(regexp="(?:[0-1][0-9]|2[0-3]):[0-5][0-9]")
@Constraint(validatedBy={})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
	ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimeOnly {

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid time.";

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
	Class<? extends Payload>[] payload() default{};
}
