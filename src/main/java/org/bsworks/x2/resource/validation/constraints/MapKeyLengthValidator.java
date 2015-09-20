package org.bsworks.x2.resource.validation.constraints;

import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link MapKeyLength} constraint.
 *
 * @author Lev Himmelfarb
 */
public class MapKeyLengthValidator
	implements ConstraintValidator<MapKeyLength,
		Map<? extends CharSequence, ?>> {

	/**
	 * Constraint annotation.
	 */
	private MapKeyLength constraintAnnotation;


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final MapKeyLength constraintAnnotation) {

		this.constraintAnnotation = constraintAnnotation;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isValid(final Map<? extends CharSequence, ?> value,
			final ConstraintValidatorContext context) {

		if ((value == null) || value.isEmpty())
			return true;

		for (final CharSequence k : value.keySet()) {
			final int kLength = (k == null ? 0 : k.length());
			if ((kLength < this.constraintAnnotation.min())
					|| (kLength > this.constraintAnnotation.max()))
				return false;
		}

		return true;
	}
}
