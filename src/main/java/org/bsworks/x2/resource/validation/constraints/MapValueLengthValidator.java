package org.bsworks.x2.resource.validation.constraints;

import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * Validator implementation for {@link MapValueLength} constraint.
 *
 * @author Lev Himmelfarb
 */
public class MapValueLengthValidator
	implements ConstraintValidator<MapValueLength,
		Map<?, ? extends CharSequence>> {

	/**
	 * Constraint annotation.
	 */
	private MapValueLength constraintAnnotation;


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void initialize(final MapValueLength constraintAnnotation) {

		this.constraintAnnotation = constraintAnnotation;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isValid(final Map<?, ? extends CharSequence> value,
			final ConstraintValidatorContext context) {

		if ((value == null) || value.isEmpty())
			return true;

		for (final Map.Entry<?, ? extends CharSequence> entry :
				value.entrySet()) {
			final CharSequence v = entry.getValue();
			final int vLength = (v == null ? 0 : v.length());
			if ((vLength < this.constraintAnnotation.min())
					|| (vLength > this.constraintAnnotation.max())) {
				context
					.buildConstraintViolationWithTemplate(
							context.getDefaultConstraintMessageTemplate())
					.addBeanNode()
						.inIterable().atKey(entry.getKey())
					.addConstraintViolation();
				context.disableDefaultConstraintViolation();
				return false;
			}
		}

		return true;
	}
}
