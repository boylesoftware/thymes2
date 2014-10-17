package org.bsworks.x2.responses;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallResponse;


/**
 * Basic HTTP 201 (Created) endpoint call response.
 *
 * @author Lev Himmelfarb
 */
public class CreatedResponse
	implements EndpointCallResponse {

	/**
	 * Response entity.
	 */
	private final Object entity;

	/**
	 * Context-relative URI that corresponds to the newly created resource,
	 * may be {@code null}.
	 */
	private final String locationPath;

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
	 * @param locationPath Context-relative URI that corresponds to the newly
	 * created resource, or {@code null} to not include the "Location" HTTP
	 * response header. The URI may be {@code null} only if the resource was
	 * created at the same request URI.
	 * @param eTag Unquoted value for "ETag" HTTP response header, or
	 * {@code null} for none.
	 * @param lastModificationTimestamp Value for "Last-Modified" HTTP response
	 * header, or {@code null} for none.
	 */
	public CreatedResponse(final Object entity, final String locationPath,
			final String eTag, final Date lastModificationTimestamp) {

		this.entity = entity;
		this.locationPath = locationPath;
		this.eTag = eTag;
		this.lastModificationTimestamp = lastModificationTimestamp;
	}


	/**
	 * Returns {@link HttpServletResponse#SC_CREATED}.
	 */
	@Override
	public final int getHttpStatusCode() {

		return HttpServletResponse.SC_CREATED;
	}

	/**
	 * Returns object passed to the constructor.
	 */
	@Override
	public final Object getEntity() {

		return this.entity;
	}

	/**
	 * Default implementation adds "Location" HTTP response header using
	 * location path passed to the constructor. It also sets "ETag" and
	 * "Last-Modification" headers depending on the values given to the
	 * constructor.
	 */
	@Override
	public void prepareHttpResponse(final EndpointCallContext ctx,
			final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {

		if (this.locationPath != null)
			httpResponse.setHeader("Location",
					httpRequest.getContextPath() + this.locationPath);

		if (this.eTag != null)
			httpResponse.setHeader("ETag", "\"" + this.eTag + "\"");
		if (this.lastModificationTimestamp != null)
			httpResponse.setDateHeader("Last-Modified",
					this.lastModificationTimestamp.getTime());
	}
}
