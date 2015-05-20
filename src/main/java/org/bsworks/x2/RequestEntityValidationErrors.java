package org.bsworks.x2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ConstraintViolation;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Request entity validation errors. The errors object is passed to the
 * {@link EndpointCallErrorException} constructor as the error details object
 * when a handler performed validation of the submitted entity and wants to
 * report back to the caller that the entity is invalid and wants to associate
 * error messages with particular entity properties. This same object is used by
 * the framework itself when annotation-based resource validation fails before
 * passing control to the handler.
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
	 * Create empty errors object.
	 */
	public RequestEntityValidationErrors() {

		this.invalidProperties = new HashMap<>();
	}

	/**
	 * Create errors object with a single invalid property.
	 *
	 * @param propPath Invalid property path.
	 * @param message Error message associated with the invalid property.
	 */
	public RequestEntityValidationErrors(final String propPath,
			final String message) {
		this();

		this.invalidProperties.put(propPath, message);
	}

	/**
	 * Create new errors object from constraint violations collection.
	 *
	 * @param cvs Constraint violations.
	 */
	public RequestEntityValidationErrors(
			final Collection<ConstraintViolation<E>> cvs) {
		this();

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
	 * Add invalid property to the errors object. If already exists, replace the
	 * message associated with the property.
	 *
	 * @param propPath Invalid property path.
	 * @param message Error message associated with the invalid property.
	 */
	public void addInvalidProperty(final String propPath,
			final String message) {

		this.invalidProperties.put(propPath, message);
	}

	/**
	 * Tell if the errors object does not contain any invalid properties.
	 *
	 * @return {@code true} if empty.
	 */
	public boolean isEmpty() {

		return this.invalidProperties.isEmpty();
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
