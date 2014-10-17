package org.bsworks.x2;


/**
 * Exception used to send error response to an endpoint call.
 *
 * @author Lev Himmelfarb
 */
public class EndpointCallErrorException
	extends Exception {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * HTTP response status code.
	 */
	private final int httpStatusCode;

	/**
	 * Optional error details object.
	 */
	private final Object errorDetails;


	/**
	 * Create new exception.
	 *
	 * @param httpStatusCode HTTP response status code.
	 * @param errorMessage Error message. Can be retrieved via
	 * {@link Exception#getMessage()} method.
	 * @param errorDetails Optional error details object, or {@code null} for
	 * none.
	 */
	public EndpointCallErrorException(final int httpStatusCode,
			final String errorMessage, final Object errorDetails) {
		super(errorMessage);

		this.httpStatusCode = httpStatusCode;
		this.errorDetails = errorDetails;
	}

	/**
	 * Create new exception for an error with no properties.
	 *
	 * @param httpStatusCode HTTP response status code.
	 * @param errorMessage Error message. Can be retrieved via
	 * {@link Exception#getMessage()} method.
	 */
	public EndpointCallErrorException(final int httpStatusCode,
			final String errorMessage) {
		this(httpStatusCode, errorMessage, null);
	}


	/**
	 * Get HTTP response status code.
	 *
	 * @return The status code.
	 */
	public int getHttpStatusCode() {

		return this.httpStatusCode;
	}

	/**
	 * Get optional error details object. The object is included in the error
	 * response body, so it needs to be an application resource.
	 *
	 * @return Error specific object, or {@code null} if none.
	 */
	public Object getErrorDetails() {

		return this.errorDetails;
	}
}
