package org.bsworks.x2.core;

import javax.servlet.ServletContext;

import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.impl.ResourcesImpl;


/**
 * Represents the application. An instance is stored in the servlet context.
 *
 * @author Lev Himmelfarb
 */
class Application {

	/**
	 * The instance runtime context.
	 */
	private final RuntimeContextImpl runtimeCtx;

	/**
	 * Endpoint mappings.
	 */
	private final EndpointMappings mappings;

	/**
	 * Endpoint call responder.
	 */
	private final EndpointCallResponder callResponder;

	/**
	 * Timeout in milliseconds for processing regular endpoint calls.
	 */
	private final long regularCallTimeout;

	/**
	 * Timeout in milliseconds for processing "long job" endpoint calls.
	 */
	private final long longJobCallTimeout;


	/**
	 * Create and initialize the application.
	 *
	 * @param sc Servlet context.
	 *
	 * @throws InitializationException If an error happens.
	 * @throws RuntimeException If an error happens.
	 */
	Application(final ServletContext sc)
		throws InitializationException {

		sc.log("initializing application");

		boolean success = false;
		try {

			// call processing timeouts
			this.regularCallTimeout = Long.parseLong(sc.getInitParameter(
					EndpointCallHandler.CALL_TIMEOUT_INITPARAM));
			this.longJobCallTimeout = Long.parseLong(sc.getInitParameter(
					EndpointCallHandler.LONG_JOB_CALL_TIMEOUT_INITPARAM));

			// create resources manager
			final Resources resources = new ResourcesImpl(sc);

			// load endpoint mappings
			this.mappings = new EndpointMappings(sc, resources);

			// initialize runtime context
			this.runtimeCtx = new RuntimeContextImpl(sc, resources);

			// create call responder
			this.callResponder = new EndpointCallResponder(sc, this.runtimeCtx);

			// done
			success = true;

		} finally {
			if (!success)
				this.shutdown(sc);
		}
	}


	/**
	 * Shutdown the application.
	 *
	 * @param sc Servlet context.
	 */
	void shutdown(final ServletContext sc) {

		sc.log("shutting down the application");

		if (this.runtimeCtx != null)
			this.runtimeCtx.shutdown();
	}


	/**
	 * Get runtime context.
	 *
	 * @return The runtime context.
	 */
	RuntimeContextImpl getRuntimeContext() {

		return this.runtimeCtx;
	}

	/**
	 * Get endpoint mappings configuration.
	 *
	 * @return The endpoint mappings configuration.
	 */
	EndpointMappings getMappings() {

		return this.mappings;
	}

	/**
	 * Get call responder.
	 *
	 * @return The call responder.
	 */
	EndpointCallResponder getCallResponder() {

		return this.callResponder;
	}

	/**
	 * Get regular call timeout.
	 *
	 * @return The timeout in milliseconds.
	 */
	long getRegularCallTimeout() {

		return this.regularCallTimeout;
	}

	/**
	 * Get "long job" call timeout.
	 *
	 * @return The timeout in milliseconds.
	 */
	long getLongJobCallTimeout() {

		return this.longJobCallTimeout;
	}
}
