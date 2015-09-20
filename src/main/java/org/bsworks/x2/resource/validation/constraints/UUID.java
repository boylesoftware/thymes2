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
 * UUID (canonical string representation) validation constraint.
 *
 * @author Lev Himmelfarb
 */
@Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}"
		+ "-[0-9a-f]{12}")
@Constraint(validatedBy={})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UUID {

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid UUID.";

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
