package org.bsworks.x2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Hook for adjusting endpoint call HTTP response before it is sent to the
 * caller.
 *
 * @author Lev Himmelfarb
 */
public interface EndpointCallHttpResponseHook {

	/**
	 * Make adjustments, such as add HTTP headers, to the response.
	 *
	 * @param ctx The endpoint call context.
	 * @param error Error, or {@code null} if success response.
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 */
	void adjustHttpResponse(EndpointCallContext ctx,
			EndpointCallErrorException error, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse);
}
