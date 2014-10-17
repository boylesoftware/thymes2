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
 * E-mail address validation constraint.
 *
 * @author Lev Himmelfarb
 */
@Pattern(regexp="[a-z0-9._%+'-]+"
		+ "@[a-z0-9][a-z0-9-]{0,63}(?:\\.[a-z0-9][a-z0-9-]{0,63})+")
@Constraint(validatedBy={})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Email {

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid e-mail address.";

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
