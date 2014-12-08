package org.bsworks.x2.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;


/**
 * Servlet that dispatches requests to the endpoint handlers. The servlet must
 * be mapped to a URL pattern by the web-application. It also must be marked as
 * supporting asynchronous request execution. It is recommended to load the
 * servlet upon web-application start-up.
 *
 * @author Lev Himmelfarb
 */
public class DispatcherServlet
	extends HttpServlet {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * The application.
	 */
	private Application app;


	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	@Override
	public void init()
		throws UnavailableException {

		this.log("initializing request dispatcher");

		this.app = ApplicationServletContextListener.getApplication(
				this.getServletContext());
		if (this.app == null)
			throw new UnavailableException(
					"The application has not been initialized.");
	}

	/* (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {

		this.log("shutting down request dispatcher");

		this.app = null;
	}


	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse)
		throws IOException {

		final boolean debug = this.log.isDebugEnabled();

		try {

			// find endpoint mapping
			final String requestURI = httpRequest.getRequestURI()
					.substring(httpRequest.getContextPath().length());
			final List<String> uriParams = new ArrayList<>();
			final EndpointMapping mapping =
				this.app.getMappings().findMapping(requestURI, uriParams);

			// check if no mapping
			if (mapping == null)
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_NOT_FOUND, null,
						"No API endpoint mapped to the request URI.");

			// got endpoint request
			if (debug)
				this.log.debug("received endpoint request "
						+ httpRequest.getMethod() + " " + requestURI);

			// respond to "OPTIONS" request
			final List<String> uriParamsRO =
				Collections.unmodifiableList(uriParams);
			final Set<HttpMethod> allowedMethods = mapping.getAllowedMethods();
			final String requestMethodStr = httpRequest.getMethod();
			if (requestMethodStr.equals("OPTIONS")) {
				this.app.getCallResponder().sendOptionsResponse(httpRequest,
						httpResponse, allowedMethods, mapping);
				return;
			}

			// verify the method
			final boolean isHead = requestMethodStr.equals("HEAD");
			HttpMethod requestMethod = null;
			boolean methodAllowed;
			try {
				requestMethod =
					HttpMethod.valueOf(isHead ? "GET" : requestMethodStr);
				methodAllowed = allowedMethods.contains(requestMethod);
			} catch (final IllegalArgumentException e) {
				methodAllowed = false;
			}
			if (!methodAllowed)
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_METHOD_NOT_ALLOWED, null,
						"The API endpoint does not allow the request method.");

			// get the call handler from the mapping
			final EndpointCallHandler<?> handler =
				mapping.getHandler(requestMethod);

			// check that it is not "HEAD" request for a "long job" call
			if (isHead && handler.isLongJob())
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_METHOD_NOT_ALLOWED, null,
						"The API endpoint does not allow the request method.");

			// submit the endpoint request for asynchronous execution
			if (debug)
				this.log.debug("starting asynchronous request processing");
			final AsyncContext asyncCtx = httpRequest.startAsync();
			final ExecutorService executorService;
			if (handler.isLongJob()) {
				executorService = this.app.getRuntimeContext()
						.getJobExecutorService();
				asyncCtx.setTimeout(this.app.getLongJobCallTimeout());
			} else {
				executorService = this.app.getRuntimeContext()
						.getCallExecutorService();
				asyncCtx.setTimeout(this.app.getRegularCallTimeout());
			}
			EndpointCallExecutor<?> endpointCallExecutor =
					new EndpointCallExecutor<>(asyncCtx,
							this.app.getRuntimeContext(),
							this.app.getCallResponder(), handler, requestMethod,
							isHead, requestURI, uriParamsRO);
			final Future<?> endpointCallTask =
				executorService.submit(endpointCallExecutor);
			endpointCallExecutor.setEndpointCallTask(endpointCallTask);

		} catch (final EndpointCallErrorException e) {
			this.app.getCallResponder().sendErrorResponse(httpRequest,
					httpResponse, null, e);
		}
	}
}
