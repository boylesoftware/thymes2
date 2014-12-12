package org.bsworks.x2.resource.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;


/**
 * Strong password validation constraint.
 *
 * @author Lev Himmelfarb
 */
@Constraint(validatedBy=StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StrongPassword {

	/**
	 * Password requirement.
	 */
	enum Requirement {

		/**
		 * Must contain at least one lower-case letter.
		 */
		ONE_LOWERCASE,

		/**
		 * Must contain at least one upper-case letter.
		 */
		ONE_UPPERCASE,

		/**
		 * Must contain at least one digit.
		 */
		ONE_DIGIT,

		/**
		 * Must contain at least one special character (ASCII ranges 33-47,
		 * 58-64, 91-96, 123-126).
		 */
		ONE_SPECIAL
	}


	/**
	 * List of requirements for the password. Default is an empty list, which
	 * means no special requirements.
	 *
	 * @return List of requirements.
	 */
	Requirement[] requirements() default {};

	/**
	 * Constraint violation message.
	 *
	 * @return Constraint violation message.
	 */
	String message() default "Password is too weak.";

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
