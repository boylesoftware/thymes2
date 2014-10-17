package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * Credit card number validation constraint.
 *
 * @author Lev Himmelfarb
 */
@Constraint(validatedBy=CreditCardNumberValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreditCardNumber {

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Invalid credit card number.";

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
