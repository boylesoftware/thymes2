package org.bsworks.x2.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;


/**
 * Endpoint mapping for a given request URI pattern.
 *
 * @author Lev Himmelfarb
 */
class EndpointMapping {

	/**
	 * Handlers by method.
	 */
	private final Map<HttpMethod, EndpointCallHandler<?>> handlers;


	/**
	 * Create new mapping.
	 */
	EndpointMapping() {

		this.handlers = new HashMap<>();
	}


	/**
	 * Add handler to the mapping.
	 *
	 * @param requestMethod HTTP method.
	 * @param handler Handler.
	 *
	 * @return {@code true} if did not already exist and was indeed added.
	 */
	boolean addHandler(final HttpMethod requestMethod,
			final EndpointCallHandler<?> handler) {

		return (this.handlers.put(requestMethod, handler) == null);
	}


	/**
	 * Get HTTP methods allowed for the mapping's URI. If the call uses
	 * a different HTTP method, the call execution is aborted and an HTTP 405
	 * (Method Not Allowed) response is returned to the client.
	 *
	 * @return Allowed HTTP methods.
	 */
	Set<HttpMethod> getAllowedMethods() {

		return this.handlers.keySet();
	}

	/**
	 * Get endpoint call handler for the specified request method.
	 *
	 * @param requestMethod HTTP request method.
	 *
	 * @return Endpoint call handler.
	 */
	EndpointCallHandler<?> getHandler(final HttpMethod requestMethod) {

		final EndpointCallHandler<?> handler = this.handlers.get(requestMethod);
		if (handler == null)
			throw new IllegalArgumentException("Invalid HTTP method "
					+ requestMethod + ".");

		return handler;
	}
}
