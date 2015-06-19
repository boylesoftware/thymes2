package org.bsworks.x2.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.services.serialization.ResourceSerializationService;


/**
 * Collection of methods used to send endpoint call responses.
 *
 * @author Lev Himmelfarb
 */
class EndpointCallResponder {

	/**
	 * Exposed header for CORS.
	 */
	private static final String EXPOSE_HEADERS = "Authentication-Info";


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * The instance runtime context.
	 */
	private final RuntimeContextImpl runtimeCtx;

	/**
	 * Resource serialization service.
	 */
	private final ResourceSerializationService serializer;

	/**
	 * Allowed CORS origins pattern, or {@code null} for the wildcard.
	 */
	private final Pattern allowedOriginsPattern;


	/**
	 * Create the responder.
	 *
	 * @param sc Servlet context.
	 * @param runtimeCtx The instance runtime context.
	 */
	EndpointCallResponder(final ServletContext sc,
			final RuntimeContextImpl runtimeCtx) {

		final String allowedOriginsPattern =
			sc.getInitParameter("x2.allowedOriginsPattern").trim();
		this.allowedOriginsPattern = (allowedOriginsPattern.equals("*") ?
				null : Pattern.compile(allowedOriginsPattern));

		this.runtimeCtx = runtimeCtx;
		this.serializer = this.runtimeCtx.getResourceSerializationService();
	}


	/**
	 * Send response to an "OPTIONS" request. Normally, it would be a CORS
	 * pre-flight request.
	 *
	 * @param httpRequest The HTTP "OPTIONS" request.
	 * @param httpResponse The HTTP response.
	 * @param allowedMethods Allowed methods from the endpoint handler.
	 * @param mapping The mapping.
	 */
	void sendOptionsResponse(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse,
			final Set<HttpMethod> allowedMethods,
			final EndpointMapping mapping) {

		// build allowed methods list
		final StringBuilder sb = new StringBuilder(64);
		for (final HttpMethod method : allowedMethods)
			sb.append(method).append(", ");
		if (allowedMethods.contains(HttpMethod.GET)) {
			final EndpointCallHandler<?> handler =
				mapping.getHandler(HttpMethod.GET);
			if (!handler.isLongJob())
				sb.append("HEAD, ");
		}
		sb.append("OPTIONS");
		final String allowedMethodsList = sb.toString();

		// set response status code
		httpResponse.setStatus(HttpServletResponse.SC_OK);

		// set OPTIONS request response headers
		httpResponse.setHeader("Allow", allowedMethodsList);
		httpResponse.setContentLength(0);

		// set CORS headers
		final String origin = httpRequest.getHeader("Origin");
		if (origin != null) {
			final boolean allowOrigin;
			if (this.allowedOriginsPattern == null) {
				allowOrigin = true;
				httpResponse.setHeader("Access-Control-Allow-Origin", "*");
			} else if (this.allowedOriginsPattern.matcher(origin).matches()) {
				allowOrigin = true;
				httpResponse.setHeader("Access-Control-Allow-Origin", origin);
				httpResponse.setHeader("Access-Control-Allow-Credentials",
						"true");
			} else {
				allowOrigin = false;
				httpResponse.setHeader("Access-Control-Allow-Origin", "null");
			}
			if (allowOrigin) {
				if (!this.runtimeCtx.getAuthTokenHandler().isUseCookie())
					httpResponse.setHeader("Access-Control-Expose-Headers",
							EXPOSE_HEADERS);
				httpResponse.setHeader("Access-Control-Allow-Methods",
						allowedMethodsList);
				final String requestHeaders =
					httpRequest.getHeader("Access-Control-Request-Headers");
				if (requestHeaders != null)
					httpResponse.setHeader("Access-Control-Allow-Headers",
							requestHeaders);
			}
			httpResponse.setIntHeader("Access-Control-Max-Age", 20 * 24 * 3600);
		}

		// log it
		if (this.log.isDebugEnabled())
			this.logResponse(httpRequest, httpResponse, "OPTIONS", null);
	}

	/**
	 * Send successful endpoint call response.
	 *
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 * @param ctx Request context.
	 * @param response The response, or {@code null} for "No Content" response.
	 * @param responseEntityBuf Buffer containing response entity, or
	 * {@code null} for no entity.
	 * @param omitResponseEntity {@code true} if sending response entity should
	 * be omitted (as a result of an HTTP "HEAD" request, for example).
	 *
	 * @throws IOException If an I/O error happens sending the response.
	 */
	void sendSuccessResponse(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse,
			final EndpointCallContextImpl ctx,
			final EndpointCallResponse response,
			final ByteArrayOutputStream responseEntityBuf,
			final boolean omitResponseEntity)
		throws IOException {

		// set up the HTTP response
		if (response == null) {
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else {
			httpResponse.setStatus(response.getHttpStatusCode());
			response.prepareHttpResponse(ctx, httpRequest, httpResponse);
		}

		// apply response hooks from the context
		ctx.applyHttpResponseHooks(httpResponse, null);

		// add authentication information to the response
		final Actor actor = ctx.getActor();
		if (actor != null)
			this.runtimeCtx.getAuthTokenHandler().addAuthInfo(httpResponse,
					actor);

		// add CORS headers to the response
		this.addCORSHeaders(httpRequest, httpResponse);

		// send the response entity if any
		if (responseEntityBuf != null) {
			httpResponse.setContentType(this.serializer.getContentType());
			httpResponse.setCharacterEncoding("UTF-8");
			httpResponse.setContentLength(responseEntityBuf.size());
			if (!omitResponseEntity)
				responseEntityBuf.writeTo(httpResponse.getOutputStream());
		} else { // no entity
			httpResponse.setContentLength(0);
		}

		// log it
		if (this.log.isDebugEnabled())
			this.logResponse(httpRequest, httpResponse, "SUCCESS",
					responseEntityBuf);
	}

	/**
	 * Send endpoint call error response.
	 *
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 * @param ctx Request context, or {@code null} if unavailable.
	 * @param error The error.
	 *
	 * @throws IOException If an I/O error happens sending the response.
	 */
	void sendErrorResponse(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse,
			final EndpointCallContextImpl ctx,
			final EndpointCallErrorException error)
		throws IOException {

		// build error response JSON body
		final ByteArrayOutputStream buf = new ByteArrayOutputStream(512);
		this.serializer.serialize(buf, Charset.forName("UTF-8"), error,
				(ctx == null ? null : ctx.getActor()));

		// set HTTP response status code and message
		httpResponse.setStatus(error.getHttpStatusCode());

		// apply response hooks from the context
		if (ctx != null)
			ctx.applyHttpResponseHooks(httpResponse, error);

		// add CORS headers to the response
		this.addCORSHeaders(httpRequest, httpResponse);

		// set response content type and length
		httpResponse.setContentType(this.serializer.getContentType());
		httpResponse.setCharacterEncoding("UTF-8");
		httpResponse.setContentLength(buf.size());

		// send response body
		buf.writeTo(httpResponse.getOutputStream());

		// log it
		if (this.log.isDebugEnabled())
			this.logResponse(httpRequest, httpResponse, "ERROR", buf);
	}

	/**
	 * Add CORS headers to the response if needed.
	 *
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 */
	private void addCORSHeaders(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse) {

		final String origin = httpRequest.getHeader("Origin");
		if (origin == null)
			return;

		final boolean allowOrigin;
		if (this.allowedOriginsPattern == null) {
			allowOrigin = true;
			httpResponse.setHeader("Access-Control-Allow-Origin", "*");
		} else if (this.allowedOriginsPattern.matcher(origin).matches()) {
			allowOrigin = true;
			httpResponse.setHeader("Access-Control-Allow-Origin", origin);
			httpResponse.setHeader("Access-Control-Allow-Credentials",
					"true");
		} else {
			allowOrigin = false;
			httpResponse.setHeader("Access-Control-Allow-Origin", "null");
		}

		if (allowOrigin && !this.runtimeCtx.getAuthTokenHandler().isUseCookie())
			httpResponse.setHeader("Access-Control-Expose-Headers",
					EXPOSE_HEADERS);
	}

	/**
	 * Log response.
	 *
	 * @param httpRequest The HTTP request.
	 * @param httpResponse The HTTP response.
	 * @param requestType Request type description.
	 * @param responseEntityBuf Response entity buffer, or {@code null} if none.
	 */
	private void logResponse(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse, final String requestType,
			final ByteArrayOutputStream responseEntityBuf) {

		final StringBuilder msg = new StringBuilder(1024);
		msg.append("sending ").append(requestType).append(" response:");

		msg.append("\n * REQUEST: ").append(httpRequest.getMethod()).append(" ")
		.append(httpRequest.getRequestURI());

		msg.append("\n * STATUS: ").append(httpResponse.getStatus());

		msg.append("\n * HEADERS:");
		for (final String hdrName : httpResponse.getHeaderNames())
			msg.append("\n    - ").append(hdrName).append(": ")
			.append(httpResponse.getHeader(hdrName));

		if (responseEntityBuf != null) {
			msg.append("\n * ENTITY: ");
			try {
				msg.append(responseEntityBuf.toString("UTF-8"));
			} catch (final UnsupportedEncodingException e) { // impossible
				throw new RuntimeException(e);
			}
		}

		this.log.debug(msg.toString());
	}
}
