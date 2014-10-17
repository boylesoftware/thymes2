package org.bsworks.x2;

import java.util.List;

import org.bsworks.x2.resource.validation.groups.Create;
import org.bsworks.x2.resource.validation.groups.Update;


/**
 * Handler of an endpoint call.
 *
 * <p>A handler can be regular or a "long job" handler, which is determined by
 * the handler implementation's {@link #isLongJob()} method.
 *
 * <p>Regular calls are processed using Servlet container's asynchronous request
 * processing facilities using a thread pool, size of which can be configured
 * using web-application context initialization parameter called
 * "x2.threads.EndpointCallProcessors". The default is 2 threads. The request
 * processing timeout can be configured using initialization parameter called
 * {@value #CALL_TIMEOUT_INITPARAM}, which specifies the timeout in
 * milliseconds. If the handler does not manage to complete call processing
 * within the timeout period, the framework sends HTTP 408 (Request Timeout)
 * response back to the client. The default timeout for regular requests is
 * 10000 (10 seconds).
 *
 * <p>"Long job" calls, on the other hand, are executed in a separate
 * low-priority thread (see {@link RuntimeContext#submitLongJob(Runnable)}) and
 * are usually multi-transactional processes. The timeout for a "long job" call
 * completion can be configured using a web-application context initialization
 * parameter called {@value #LONG_JOB_CALL_TIMEOUT_INITPARAM}, which specifies
 * the timeout in milliseconds. The default is 60000 (1 minute).
 *
 * <p>Note also, that "long job" calls do not support "HEAD" HTTP method.
 *
 * <p>A handler may expect an entity in the HTTP requests that it accepts. This
 * is determined by the {@link #getRequestEntityClass()} method. The maximum
 * size of the request entity is limited. If it is larger than the limit, the
 * framework send an HTTP 413 (Request Entity Too Large) response back to the
 * client. The limit can be configured using a web-application context
 * initialization parameter called
 * {@value #MAX_REQUEST_SIZE_INITPARAM}, which specifies the maximum size in
 * bytes. The default is 2048 (2 kilobytes).
 *
 * @param <E> Request entity type. Use {@link Void} if the handler does not
 * expect requests with entities.
 *
 * @author Lev Himmelfarb
 */
public interface EndpointCallHandler<E> {

	/**
	 * Name of web-application context initialization parameter used to
	 * configure maximum request entity size in bytes.
	 */
	static final String MAX_REQUEST_SIZE_INITPARAM = "x2.maxRequestSize";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure the timeout in milliseconds for processing regular
	 * ({@link #isLongJob()} returns {@code false}) endpoint calls.
	 */
	static final String CALL_TIMEOUT_INITPARAM = "x2.regularCallTimeout";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure the timeout in milliseconds for processing long job
	 * ({@link #isLongJob()} returns {@code true}) endpoint calls.
	 */
	static final String LONG_JOB_CALL_TIMEOUT_INITPARAM =
			"x2.longJobCallTimeout";

	/**
	 * Standard validation groups for new data.
	 */
	static final Class<?>[] CREATE_VAIDATION_GROUPS = {
		javax.validation.groups.Default.class,
		Create.class
	};

	/**
	 * Standard validation groups for update data.
	 */
	static final Class<?>[] UPDATE_VAIDATION_GROUPS = {
		javax.validation.groups.Default.class,
		Update.class
	};


	/**
	 * Tell if the actor has sufficient permissions to perform call.
	 *
	 * <p>If the method returns {@code false} and the actor is {@code null},
	 * which means an unauthenticated call, the call execution is aborted and an
	 * HTTP 401 (Unauthorized) response is returned to the client. If the actor
	 * is not {@code null}, that is the call is authenticated, the call
	 * execution is aborted and an HTTP 403 (Forbidden) response is returned to
	 * the client.
	 *
	 * @param requestMethod Request method.
	 * @param requestURI Web-application context-relative request URI.
	 * @param uriParams Unmodifiable list of URI parameters.
	 * @param actor Actor making the call, or {@code null} for an
	 * unauthenticated request.
	 *
	 * @return {@code true} if the call is allowed.
	 */
	boolean isAllowed(HttpMethod requestMethod, String requestURI,
			List<String> uriParams, Actor actor);

	/**
	 * Get class of the expected request entity. If returns {@code null}, no
	 * request entity is required. If returns a class, and the request does not
	 * contain an entity, the entity cannot be parsed by the application's
	 * resource serialization service, or the resulting object fails to
	 * validate, the call execution is aborted and an HTTP 400 (Bad Request)
	 * response is returned to the client. If the request contains an entity,
	 * but the content type is not the one expected by the application's
	 * resource serialization service, the call execution is aborted and an
	 * HTTP 414 (Unsupported Media Type) response is returned to the client.
	 *
	 * @return Request entity class, or {@code null} if no request entity is
	 * expected.
	 */
	Class<E> getRequestEntityClass();

	/**
	 * Get request entity validation groups. The method is called only if
	 * {@link #getRequestEntityClass()} returns something.
	 *
	 * @return Validation groups.
	 */
	Class<?>[] getRequestEntityValidationGroups();

	/**
	 * Tell if handling a call is a "long job".
	 *
	 * @return {@code true} to execute the handler in the special low-priority
	 * thread used for jobs.
	 *
	 * @see RuntimeContext#submitLongJob(Runnable)
	 */
	boolean isLongJob();

	/**
	 * Tell if the handler modifies any persistent data in the back-end. This
	 * information may be used by the framework for some persistence layer
	 * optimizations.
	 *
	 * @return {@code true} if the handler does not modify any persistent data.
	 */
	boolean isReadOnly();

	/**
	 * Handle a call.
	 *
	 * @param ctx Call context.
	 * @param requestEntity Request entity (validated), or {@code null} if no
	 * request entity is expected by the handler.
	 *
	 * @return The call response. If {@code null} is returned, HTTP 204
	 * (No Content) response is sent back to the client.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	EndpointCallResponse handleCall(EndpointCallContext ctx, E requestEntity)
		throws EndpointCallErrorException;
}
