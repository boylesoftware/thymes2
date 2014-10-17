package org.bsworks.x2.responses;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallResponse;


/**
 * Basic HTTP 304 (Not Modified) endpoint call response.
 *
 * @author Lev Himmelfarb
 */
public class NotModifiedResponse
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
	 * header, or {@code null} for none. Used only if {@code eTag} parameter is
	 * {@code null}.
	 */
	public NotModifiedResponse(final String eTag,
			final Date lastModificationTimestamp) {

		this.eTag = eTag;
		this.lastModificationTimestamp = lastModificationTimestamp;
	}


	/**
	 * Returns {@link HttpServletResponse#SC_NOT_MODIFIED}.
	 */
	@Override
	public final int getHttpStatusCode() {

		return HttpServletResponse.SC_NOT_MODIFIED;
	}

	/**
	 * Returns {@code null}.
	 */
	@Override
	public final Object getEntity() {

		return null;
	}

	/**
	 * Default implementation sets "ETag" or "Last-Modification" HTTP response
	 * header depending on the values given to the response constructor.
	 */
	@Override
	public void prepareHttpResponse(final EndpointCallContext ctx,
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {

		if (this.eTag != null)
			httpResponse.setHeader("ETag", "\"" + this.eTag + "\"");
		else if (this.lastModificationTimestamp != null)
			httpResponse.setDateHeader("Last-Modified",
					this.lastModificationTimestamp.getTime());
	}
}
