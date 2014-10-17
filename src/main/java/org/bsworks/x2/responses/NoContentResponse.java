package org.bsworks.x2.responses;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallResponse;


/**
 * Basic HTTP 204 (No Content) endpoint call response.
 *
 * @author Lev Himmelfarb
 */
public class NoContentResponse
	implements EndpointCallResponse {

	/**
	 * Value for "ETag" HTTP response header, or {@code null} for none.
	 */
	private final String eTag;

	/**
	 * Value for "Last-Modified" HTTP response header, or {@code null} for none.
	 */
	private final Date lastModificationTimestamp;


	/**
	 * Create new response.
	 *
	 * @param eTag Unquoted value for "ETag" HTTP response header, or
	 * {@code null} for none.
	 * @param lastModificationTimestamp Value for "Last-Modified" HTTP response
	 * header, or {@code null} for none.
	 */
	public NoContentResponse(final String eTag,
			final Date lastModificationTimestamp) {

		this.eTag = eTag;
		this.lastModificationTimestamp = lastModificationTimestamp;
	}


	/**
	 * Returns {@link HttpServletResponse#SC_NO_CONTENT}.
	 */
	@Override
	public final int getHttpStatusCode() {

		return HttpServletResponse.SC_NO_CONTENT;
	}

	/**
	 * Returns {@code null}.
	 */
	@Override
	public final Object getEntity() {

		return null;
	}

	/**
	 * Default implementation sets "ETag" and "Last-Modification" HTTP response
	 * headers depending on the values given to the response constructor.
	 */
	@Override
	public void prepareHttpResponse(final EndpointCallContext ctx,
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {

		if (this.eTag != null)
			httpResponse.setHeader("ETag", "\"" + this.eTag + "\"");
		if (this.lastModificationTimestamp != null)
			httpResponse.setDateHeader("Last-Modified",
					this.lastModificationTimestamp.getTime());
	}
}
