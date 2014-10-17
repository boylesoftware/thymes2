package org.bsworks.x2.resource.validation.constraints;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.bsworks.x2.util.Weekday;


/**
 * Validator implementation for {@link DateOnly}.
 *
 * @author Lev Himmelfarb
 */
public class DateOnlyValidator
	implements ConstraintValidator<DateOnly, String> {

	/**
	 * The date format.
	 */
	static final String FORMAT = "yyyy-MM-dd";


	/**
	 * The annotation.
	 */
	private DateOnly anno;


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final DateOnly constraintAnnotation) {

		this.anno = constraintAnnotation;
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
		df.setLenient(false);

		return isValid(value, df, this.anno);
	}


	/**
	 * Validate a date.
	 *
	 * @param value The date value. Must not be {@code null}.
	 * @param df Date format to use.
	 * @param anno The annotation.
	 *
	 * @return {@code true} if valid.
	 */
	static boolean isValid(final String value, final DateFormat df,
			final DateOnly anno) {

		try {
			df.parse(value);
		} catch (final ParseException e) {
			return false;
		}

		final Weekday[] weekdays = anno.weekdays();
		if ((weekdays != null) && (weekdays.length > 0)) {
			final Weekday weekday = Weekday.values()
					[df.getCalendar().get(Calendar.DAY_OF_WEEK) - 1];
			boolean found = false;
			for (final Weekday d : weekdays) {
				if (d == weekday) {
					found = true;
					break;
				}
			}
			return found;
		}

		return true;
	}
}
