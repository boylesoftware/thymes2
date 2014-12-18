package org.bsworks.x2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.util.StringUtils;


/**
 * Exception used to send error response to an endpoint call.
 *
 * @author Lev Himmelfarb
 */
public class EndpointCallErrorException
	extends Exception
	implements EndpointCallResponse {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * HTTP response status code.
	 */
	private final int httpStatusCode;

	/**
	 * Application-specific error code.
	 */
	private final String errorCode;

	/**
	 * Optional error details object.
	 */
	private final Object errorDetails;


	/**
	 * Create new exception.
	 *
	 * @param httpStatusCode HTTP response status code.
	 * @param errorCode Application-specific error code. If {@code null}, the
	 * error code is initialized with "X2-CCC", where "CCC" is the HTTP response
	 * status code.
	 * @param errorMessage Error message. Can be retrieved via
	 * {@link Exception#getMessage()} method. May be {@code null}, in which case
	 * the error message in the call response is simply "Error.".
	 * @param errorDetails Optional error details object, or {@code null} for
	 * none.
	 */
	public EndpointCallErrorException(final int httpStatusCode,
			final String errorCode, final String errorMessage,
			final Object errorDetails) {
		super(errorMessage);

		this.httpStatusCode = httpStatusCode;
		this.errorCode = StringUtils.defaultIfEmpty(errorCode,
				"X2-" + httpStatusCode);
		this.errorDetails = errorDetails;
	}

	/**
	 * Create new exception for an error with no properties. Equivalent to
	 * calling
	 * {@code EndpointCallErrorException(httpStatusCode, errorCode, errorMessage, null)}.
	 *
	 * @param httpStatusCode HTTP response status code.
	 * @param errorCode Application-specific error code.
	 * @param errorMessage Error message.
	 */
	public EndpointCallErrorException(final int httpStatusCode,
			final String errorCode, final String errorMessage) {
		this(httpStatusCode, errorCode, errorMessage, null);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public int getHttpStatusCode() {

		return this.httpStatusCode;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object getEntity() {

		return this;
	}

	/**
	 * The default implementation does nothing.
	 */
	@Override
	public void prepareHttpResponse(final EndpointCallContext ctx,
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {

		// nothing
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

		return StringUtils.defaultIfEmpty(this.getMessage(), "Error.");
	}

	/**
	 * Get optional error details.
	 *
	 * @return Error details descriptor, or {@code null} if none.
	 */
	@Property
	public Object getErrorDetails() {

		return this.errorDetails;
	}
}
