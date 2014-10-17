package org.bsworks.x2.core;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Endpoint call error response entity.
 *
 * @author Lev Himmelfarb
 */
public class ErrorResponse {

	/**
	 * Error message.
	 */
	private final String errorMessage;

	/**
	 * Error details.
	 */
	private final Object errorDetails;


	/**
	 * Create new error response entity.
	 *
	 * @param errorMessage Error message.
	 * @param errorDetails Error details descriptor, if any.
	 */
	ErrorResponse(final String errorMessage, final Object errorDetails) {

		this.errorMessage = errorMessage;
		this.errorDetails = errorDetails;
	}


	/**
	 * Get error message.
	 *
	 * @return The error message.
	 */
	@Property
	public String getErrorMessage() {

		return this.errorMessage;
	}

	/**
	 * Get error details.
	 *
	 * @return Error details descriptor.
	 */
	@Property
	public Object getErrorDetails() {

		return this.errorDetails;
	}
}
