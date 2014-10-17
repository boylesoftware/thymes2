package org.bsworks.x2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Successful endpoint call response.
 *
 * @author Lev Himmelfarb
 */
public interface EndpointCallResponse {

	/**
	 * Get HTTP response status code.
	 *
	 * @return The code.
	 */
	int getHttpStatusCode();

	/**
	 * Get object to be sent in JSON format as the response entity. The object
	 * can be a Java bean or a {@link java.util.Collection}.
	 * {@link java.util.Map}s are not supported and are treated as Java beans.
	 *
	 * @return Object for the response entity, or {@code null} for nothing.
	 */
	Object getEntity();

	/**
	 * Put any additional information, such as HTTP headers, to the response.
	 * The response status code and entity are handled by the framework.
	 *
	 * @param ctx The endpoint call context.
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 */
	void prepareHttpResponse(EndpointCallContext ctx,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
