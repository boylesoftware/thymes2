package org.bsworks.x2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Request entity validation errors.
 *
 * @param <E> The entity type.
 *
 * @author Lev Himmelfarb
 */
public class RequestEntityValidationErrors<E> {

	/**
	 * Property validation error messages by property paths.
	 */
	private final Map<String, String> invalidProperties;


	/**
	 * Create new errors object.
	 *
	 * @param cvs Constraint violations.
	 */
	RequestEntityValidationErrors(final Set<ConstraintViolation<E>> cvs) {

		this.invalidProperties = new HashMap<>();
		for (final ConstraintViolation<E> cv : cvs) {
			final String propName = cv.getPropertyPath().toString();
			final String message = cv.getMessage();
			final String currentMessage = this.invalidProperties.get(propName);
			this.invalidProperties.put(propName,
					(currentMessage != null ?
							currentMessage + " " + message : message));
		}
	}


	/**
	 * Get invalid properties information.
	 *
	 * @return Property validation error messages by property paths.
	 */
	@Property
	public Map<String, String> getInvalidProperties() {

		return this.invalidProperties;
	}
}
