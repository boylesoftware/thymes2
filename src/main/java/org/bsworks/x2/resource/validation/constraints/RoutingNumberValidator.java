package org.bsworks.x2.resource.validation.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link RoutingNumber}.
 *
 * @author Lev Himmelfarb
 */
public class RoutingNumberValidator
	implements ConstraintValidator<RoutingNumber, String> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final RoutingNumber constraintAnnotation) {

		// nothing
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isValid(final String value,
			final ConstraintValidatorContext context) {

		if (value == null)
			return true;

		final int len = value.length();
		if (len != 9)
			return false;

		final int digits[] = new int[len];
		for (int i = 0; i < len; i++) {
			final char c = value.charAt(i);
			if (!Character.isDigit(c))
				return false;
			digits[i] = Character.digit(c, 10);
		}

		int sum =
				7 * (digits[0] + digits[3] + digits[6])
				+ 3 * (digits[1] + digits[4] + digits[7])
				+ 9 * (digits[2] + digits[5]);

		return (sum % 10 == digits[8]);
	}
}
