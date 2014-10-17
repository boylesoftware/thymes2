package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.bsworks.x2.util.Weekday;


/**
 * Date (without time) validation constraint. The constraint is applicable to
 * string fields (or collections of strings) and makes sure that the string is
 * in format "yyyy-MM-dd". The constraint also allows to restrict the dates only
 * to certain weekdays.
 *
 * @author Lev Himmelfarb
 */
@Constraint(validatedBy={ DateOnlyValidator.class,
		CollectionDateOnlyValidator.class })
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
	ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateOnly {

	/**
	 * Allowed weekdays.
	 *
	 * @return Allowed weekdays.
	 */
	Weekday[] weekdays() default {};

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid date.";

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
