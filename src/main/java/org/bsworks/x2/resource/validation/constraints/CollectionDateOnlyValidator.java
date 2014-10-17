package org.bsworks.x2.resource.validation.constraints;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link DateOnly}.
 *
 * @author Lev Himmelfarb
 */
public class CollectionDateOnlyValidator
	implements ConstraintValidator<DateOnly, Collection<String>> {

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
	public boolean isValid(final Collection<String> value,
			final ConstraintValidatorContext context) {

		if (value == null)
			return true;

		final DateFormat df = new SimpleDateFormat(DateOnlyValidator.FORMAT);
		df.setLenient(false);
		for (final String v : value) {
			if (!DateOnlyValidator.isValid(v, df, this.anno))
				return false;
		}

		return true;
	}
}
