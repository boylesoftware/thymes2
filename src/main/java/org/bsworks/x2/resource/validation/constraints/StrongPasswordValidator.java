package org.bsworks.x2.resource.validation.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.bsworks.x2.resource.validation.constraints.StrongPassword.Requirement;


/**
 * Validator for the {@link StrongPassword} constraint.
 *
 * @author Lev Himmelfarb
 */
public class StrongPasswordValidator
	implements ConstraintValidator<StrongPassword, String> {

	/**
	 * Constraint annotation.
	 */
	private StrongPassword constraintAnnotation;


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final StrongPassword constraintAnnotation) {

		this.constraintAnnotation = constraintAnnotation;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isValid(final String value,
			final ConstraintValidatorContext context) {

		if (value == null)
			return true;

		int numLowercase = 0;
		int numUppercase = 0;
		int numDigits = 0;
		int numSpecial = 0;
		final int len = value.length();
		for (int i = 0; i < len; i++) {
			final char c = value.charAt(i);
			if ((c >= 'a') && (c <= 'z'))
				numLowercase++;
			else if ((c >= 'A') && (c <= 'Z'))
				numUppercase++;
			else if ((c >= '0') && (c <= '9'))
				numDigits++;
			else if (c > ' ')
				numSpecial++;
		}

		for (final Requirement req : this.constraintAnnotation.requirements()) {
			switch (req) {
			case ONE_LOWERCASE:
				if (numLowercase == 0)
					return false;
				break;
			case ONE_UPPERCASE:
				if (numUppercase == 0)
					return false;
				break;
			case ONE_DIGIT:
				if (numDigits == 0)
					return false;
				break;
			case ONE_SPECIAL:
				if (numSpecial == 0)
					return false;
				break;
			default:
			}
		}

		return true;
	}
}
