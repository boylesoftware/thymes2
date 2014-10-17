package org.bsworks.x2.responses;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallResponse;


/**
 * Basic HTTP 200 (OK) endpoint call response.
 *
 * @author Lev Himmelfarb
 */
public class OKResponse
	implements EndpointCallResponse {

	/**
	 * Response entity.
	 */
	private final Object entity;

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
	 * @param entity Response entity.
	 * @param eTag Unquoted value for "ETag" HTTP response header, or
	 * {@code null} for none.
	 * @param lastModificationTimestamp Value for "Last-Modified" HTTP response
	 * header, or {@code null} for none.
	 */
	public OKResponse(final Object entity, final String eTag,
			final Date lastModificationTimestamp) {

		this.entity = entity;
		this.eTag = eTag;
		this.lastModificationTimestamp = lastModificationTimestamp;
	}


	/**
	 * Returns {@link HttpServletResponse#SC_OK}.
	 */
	@Override
	public final int getHttpStatusCode() {

		return HttpServletResponse.SC_OK;
	}

	/**
	 * Returns object passed to the constructor.
	 */
	@Override
	public final Object getEntity() {

		return this.entity;
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
