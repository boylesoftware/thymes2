package org.bsworks.x2.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.services.monitor.ApplicationErrorContext;
import org.bsworks.x2.services.monitor.ApplicationErrorContextProperty;
import org.bsworks.x2.services.monitor.ApplicationErrorContextSection;
import org.bsworks.x2.services.serialization.ResourceSerializationService;


/**
 * Top-level executor of an endpoint call.
 *
 * @param <E> Request entity type expected by the handler.
 *
 * @author Lev Himmelfarb
 */
class EndpointCallExecutor<E>
	implements Runnable, AsyncListener {

	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Asynchronous HTTP request processing context.
	 */
	private final AsyncContext asyncCtx;

	/**
	 * Instance runtime context.
	 */
	private final RuntimeContextImpl runtimeCtx;

	/**
	 * Endpoint call responder.
	 */
	private final EndpointCallResponder callResponder;

	/**
	 * The endpoint call handler.
	 */
	private final EndpointCallHandler<E> handler;

	/**
	 * Call HTTP method.
	 */
	private final HttpMethod requestMethod;

	/**
	 * Tells if response entity should be omitted.
	 */
	private final boolean omitResponseEntity;

	/**
	 * Context-relative request URI.
	 */
	private final String requestURI;

	/**
	 * Unmodifiable list of positional URI parameters.
	 */
	private final List<String> uriParams;

	/**
	 * Endpoint call task within the assigned executor service.
	 */
	private Future<?> endpointCallTask = null;

	/**
	 * Object used to regulate response sending between executor and container
	 * threads.
	 */
	private Object responseSendingSync = new Object();

	/**
	 * Tells if the response has been sent.
	 */
	private AtomicBoolean responseSent = new AtomicBoolean(false);


	/**
	 * Create new executor for a call.
	 *
	 * @param asyncCtx Asynchronous HTTP request processing context.
	 * @param runtimeCtx Instance runtime context.
	 * @param callResponder Endpoint call responder.
	 * @param handler The endpoint call handler.
	 * @param requestMethod Call HTTP method.
	 * @param omitResponseEntity If {@code true}, the response entity will be
	 * forcefully omitted. Used, for example, for "HEAD" requests.
	 * @param requestURI Context-relative request URI.
	 * @param uriParams Unmodifiable list of positional URI parameters.
	 */
	EndpointCallExecutor(final AsyncContext asyncCtx,
			final RuntimeContextImpl runtimeCtx,
			final EndpointCallResponder callResponder,
			final EndpointCallHandler<E> handler,
			final HttpMethod requestMethod, final boolean omitResponseEntity,
			final String requestURI, final List<String> uriParams) {

		this.asyncCtx = asyncCtx;
		this.runtimeCtx = runtimeCtx;
		this.callResponder = callResponder;
		this.handler = handler;
		this.requestMethod = requestMethod;
		this.omitResponseEntity = omitResponseEntity;
		this.requestURI = requestURI;
		this.uriParams = uriParams;

		this.asyncCtx.addListener(this);
	}

	/**
	 * Set endpoint call task within the assigned executor service.
	 *
	 * @param endpointCallTask The task returned by the executor service upon
	 * submission.
	 */
	void setEndpointCallTask(final Future<?> endpointCallTask) {

		this.endpointCallTask = endpointCallTask;
	}


	/* (non-Javadoc)
	 * @see javax.servlet.AsyncListener#onComplete(javax.servlet.AsyncEvent)
	 */
	@Override
	public void onComplete(final AsyncEvent event) {

		if (this.log.isDebugEnabled())
			this.log.debug("async complete", event.getThrowable());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.AsyncListener#onTimeout(javax.servlet.AsyncEvent)
	 */
	@Override
	public void onTimeout(final AsyncEvent event) {

		final boolean debug = this.log.isDebugEnabled();
		if (debug)
			this.log.debug("async timeout", event.getThrowable());

		synchronized (this.responseSendingSync) {
			if (!this.responseSent.get()) {
				try {

					// cancel the task
					if (this.endpointCallTask != null)
						this.endpointCallTask.cancel(true);

					// send timeout error response
					this.callResponder.sendErrorResponse(
							(HttpServletRequest) this.asyncCtx.getRequest(),
							(HttpServletResponse) this.asyncCtx.getResponse(),
							null,
							new EndpointCallErrorException(
									HttpServletResponse.SC_REQUEST_TIMEOUT,
									"Request timeout."));

				} catch (final IOException e) {
					this.log.warn("I/O error sending timout error response", e);
				} finally {
					this.responseSent.set(true);
					this.asyncCtx.complete();
				}
			} else {
				if (debug)
					this.log.debug("response already sent, doing nothing");
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.AsyncListener#onError(javax.servlet.AsyncEvent)
	 */
	@Override
	public void onError(final AsyncEvent event) {

		if (this.log.isDebugEnabled())
			this.log.debug("async error", event.getThrowable());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.AsyncListener#onStartAsync(javax.servlet.AsyncEvent)
	 */
	@Override
	public void onStartAsync(final AsyncEvent event) {

		if (this.log.isDebugEnabled())
			this.log.debug("async start", event.getThrowable());
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		// log call processing start
		final boolean debug = this.log.isDebugEnabled();
		if (debug)
			this.log.debug("started call execution");

		// get the request
		final HttpServletRequest httpRequest =
			(HttpServletRequest) this.asyncCtx.getRequest();

		// request entity buffer
		byte[] buf = null;
		int dataLen = 0;

		// execute the call
		EndpointCallErrorException error = null;
		EndpointCallContextImpl ctx = null;
		EndpointCallResponse response = null;
		ByteArrayOutputStream responseEntityBuf = null;
		try {

			// check if aborted
			this.checkAborted();

			// get actor making the call
			final Actor actor =
				this.runtimeCtx.getAuthTokenHandler().getActor(httpRequest);

			// check if aborted
			this.checkAborted();

			//check if call is allowed to the actor
			if (!this.handler.isAllowed(this.requestMethod, this.requestURI,
					this.uriParams, actor)) {
				if (actor == null)
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_UNAUTHORIZED,
							"Authentication required.");
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_FORBIDDEN,
						"Insufficient permissions.");
			}

			// get resource serializer
			final ResourceSerializationService serializer =
				this.runtimeCtx.getResourceSerializationService();

			// read the request entity if required
			final E requestEntity;
			final Class<E> requestEntityClass =
				this.handler.getRequestEntityClass();
			if (requestEntityClass != null) {

				// check content type
				final String requestCType = httpRequest.getContentType();
				if ((requestCType == null) || !requestCType.matches(
						serializer.getContentType() + "(?:;.*)?"))
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
							"The request entity is not JSON.");

				// check content length
				final int maxSize = this.runtimeCtx.getMaxRequestSize();
				final int contentLength = httpRequest.getContentLength();
				if (contentLength > maxSize)
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
							"The request entity is too large.");

				// read the content
				buf = new byte[maxSize];
				try (final InputStream in = httpRequest.getInputStream()) {
					int bytesRead;
					while ((bytesRead = in.read(buf, dataLen,
							maxSize - dataLen)) != -1) {
						dataLen += bytesRead;
					}
				} catch (final IndexOutOfBoundsException e) {
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
							"The request entity is too large.");
				}

				// parse the content
				try {
					requestEntity = serializer.deserialize(
							new ByteArrayInputStream(buf, 0, dataLen),
							httpRequest.getCharacterEncoding(),
							requestEntityClass, actor);
				} catch (final UnsupportedEncodingException e) {
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
							"Invalid request entity character set.");
				} catch (final InvalidResourceDataException e) {
					if (debug)
						this.log.debug("invalid request entity", e);
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST,
							"Invalid request entity.");
				}

				// validate the request entity object
				final Set<ConstraintViolation<E>> cvs =
						this.runtimeCtx.getValidatorFactory().getValidator()
						.validate(requestEntity,
							this.handler.getRequestEntityValidationGroups());
				if (cvs.size() > 0) {

					// debug log
					if (debug) {
						final StringBuilder sb = new StringBuilder(1024);
						sb.append("constraint violations:");
						for (final ConstraintViolation<E> cv : cvs)
							sb.append('\n')
								.append(cv.getPropertyPath().toString())
								.append(" <= [").append(cv.getInvalidValue())
								.append("]: ").append(cv.getMessage());
						this.log.debug(sb.toString());
					}

					// build error
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST,
							"Invalid request entity.",
							new RequestEntityValidationErrors<>(cvs));
				}

			} else { // no request entity needed by the handler
				requestEntity = null;
			}

			// check if aborted
			this.checkAborted();

			// create call context and handle the call
			ctx = new EndpointCallContextImpl(this.runtimeCtx,
					this.requestMethod, this.requestURI, this.uriParams,
					httpRequest, actor, this.handler.isReadOnly());
			boolean commit = false;
			try {

				// handle the call and get the response
				response = this.handler.handleCall(ctx, requestEntity);

				// check if aborted
				this.checkAborted();

				// serialize response entity if any
				final Actor responseActor = ctx.getActor();
				final Object responseEntity =
					(response != null ? response.getEntity() : null);
				if (responseEntity != null) {
					responseEntityBuf = new ByteArrayOutputStream(1024);
					serializer.serialize(responseEntityBuf,
							Charset.forName("UTF-8"), responseEntity,
							responseActor);
				}

				// check if aborted
				this.checkAborted();

				// commit transaction if any
				commit = true;

			} finally {
				ctx.close(commit);
			}

		} catch (final EndpointCallErrorException e) {
			if (debug)
				this.log.debug("endpoint call error", e);

			error = e;

		} catch (final Exception e) {
			this.log.error("internal server error: " + e.getMessage(), e);

			this.runtimeCtx.getMonitorService().logApplicationError(e,
					this.createEndpointCallErrorContext(buf, dataLen));

			error = new EndpointCallErrorException(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					e.getMessage());

		} finally {

			// send the response
			synchronized (this.responseSendingSync) {
				if (!this.responseSent.get()) {
					try {
						final HttpServletResponse httpResponse =
							(HttpServletResponse) this.asyncCtx.getResponse();
						try {
							if (error != null)
								this.callResponder.sendErrorResponse(
										httpRequest, httpResponse,
										(ctx == null ? null : ctx.getActor()),
										error);
							else
								this.callResponder.sendSuccessResponse(
										httpRequest, httpResponse, ctx,
										response, responseEntityBuf,
										this.omitResponseEntity);
						} catch (final IOException e) {
							throw e;
						} catch (final Exception e) {
							this.log.error("error sending response", e);
							httpResponse.sendError(
								HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						}
					} catch (final IOException e) {
						this.log.warn("I/O error sending response", e);
					} finally {
						this.responseSent.set(true);
						this.asyncCtx.complete();
					}
				} else {
					if (debug)
						this.log.debug("response already sent, doing nothing");
				}
			}
		}
	}


	/**
	 * Create application error context for the current endpoint call.
	 *
	 * @param buf Buffer containing JSON request entity, or {@code null} if none
	 * or unknown.
	 * @param dataLen Number of bytes in the request entity buffer.
	 *
	 * @return The context.
	 */
	private ApplicationErrorContext createEndpointCallErrorContext(
			final byte[] buf, final int dataLen) {
		ApplicationErrorContextSection sec;

		final HttpServletRequest httpRequest =
			(HttpServletRequest) this.asyncCtx.getRequest();

		final ApplicationErrorContext errorCtx =
			new ApplicationErrorContext();

		sec = errorCtx.addSection("Request Basics");
		sec.addProperty("method").addValue(httpRequest.getMethod());
		sec.addProperty("URI").addValue(httpRequest.getRequestURI());
		sec.addProperty("query").addValue(httpRequest.getQueryString());
		sec.addProperty("source").addValue(httpRequest.getRemoteAddr());

		sec = errorCtx.addSection("Request Headers");
		for (final Enumeration<String> en = httpRequest.getHeaderNames();
				en.hasMoreElements();) {
			final String headerName = en.nextElement();
			final ApplicationErrorContextProperty prop =
				sec.addProperty(headerName);
			for (final Enumeration<String> en1 =
					httpRequest.getHeaders(headerName); en1.hasMoreElements();)
				prop.addValue(en1.nextElement());
		}

		sec = errorCtx.addSection("Request Parameters");
		for (final Enumeration<String> en = httpRequest.getParameterNames();
				en.hasMoreElements();) {
			final String paramName = en.nextElement();
			final ApplicationErrorContextProperty prop =
				sec.addProperty(paramName);
			for (final String paramVal :
					httpRequest.getParameterValues(paramName))
				prop.addValue(paramVal);
		}

		if (buf != null) {
			sec = errorCtx.addSection("Request Entity");
			final ApplicationErrorContextProperty prop =
				sec.addProperty("entity");

			final String enc = httpRequest.getCharacterEncoding();
			if (enc != null) {
				try {
					prop.addValue(new String(buf, 0, dataLen, enc));
				} catch (final UnsupportedEncodingException e) {
					prop.addValue("unknown encoding");
				}
			} else {
				prop.addValue("unspecified encoding");
			}
		}

		return errorCtx;
	}

	/**
	 * Check if the task is aborted (for example, due to the request timeout or
	 * system shutdown).
	 *
	 * @throws EndpointCallErrorException if aborted.
	 */
	private void checkAborted()
		throws EndpointCallErrorException {

		if (Thread.currentThread().isInterrupted()) {
			this.log.info("interrupted, aborting endpoint call execution");
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"Call execution aborted by the server.");
		}

		if ((this.endpointCallTask != null) && this.endpointCallTask.isDone()) {
			this.log.info("cancelled, aborting endpoint call execution");
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"Call execution aborted by the server.");
		}
	}
}
