package org.bsworks.x2.core;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Endpoint call error response entity.
 *
 * @author Lev Himmelfarb
 */
public class ErrorResponse {

	/**
	 * Error code.
	 */
	private final String errorCode;

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
	 * @param errorCode Application-specific error code.
	 * @param errorMessage Error message.
	 * @param errorDetails Error details descriptor, if any.
	 */
	ErrorResponse(final String errorCode, final String errorMessage,
			final Object errorDetails) {

		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.errorDetails = errorDetails;
	}


	/**
	 * Get application-specific error code.
	 *
	 * @return The error code.
	 */
	@Property
	public String getErrorCode() {

		return this.errorCode;
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
