package org.bsworks.x2.resource.validation.constraints;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link DateTime}.
 *
 * @author Lev Himmelfarb
 */
public class DateTimeValidator
	implements ConstraintValidator<DateTime, String> {

	/**
	 * The date and time format.
	 */
	static final String FORMAT = "yyyy-MM-dd HH:mm";


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final DateTime constraintAnnotation) {

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

		final DateFormat df = new SimpleDateFormat(FORMAT);
		try {
			df.parse(value);
		} catch (final ParseException e) {
			return false;
		}

		return true;
	}
}
