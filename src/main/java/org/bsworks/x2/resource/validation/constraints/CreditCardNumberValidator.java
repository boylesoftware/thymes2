package org.bsworks.x2.resource.validation.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link CreditCardNumber}.
 *
 * @author Lev Himmelfarb
 */
public class CreditCardNumberValidator
	implements ConstraintValidator<CreditCardNumber, String> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final CreditCardNumber constraintAnnotation) {

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
		if ((len < 13) || (len > 16))
			return false;

		final int digits[] = new int[len];
		for (int i = 0; i < len; i++) {
			final char c = value.charAt(i);
			if (!Character.isDigit(c))
				return false;
			digits[i] = Character.digit(c, 10);
		}

		int sum = 0;
		boolean even = false;
		for (int i = digits.length - 1; i >= 0; i--) {
			int digit = digits[i];
			if (even)
				digit *= 2;
			if (digit > 9)
				digit = digit / 10 + digit % 10;
			sum += digit;
			even = !even;
		}

		return (sum % 10 == 0);
	}
}
