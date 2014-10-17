package org.bsworks.x2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.app.DefaultPersistentResourceEndpointHandler;
import org.bsworks.x2.app.PersistentResourceEndpointHandler;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.Resources;


/**
 * Collection of endpoint mappings used by the dispatcher servlet.
 *
 * @author Lev Himmelfarb
 */
class EndpointMappings {

	/**
	 * Pattern used to parse mappings.
	 */
	private static final Pattern MAPPING_PATTERN = Pattern.compile(
			"(?:^\\s*|\\G)"
			+ "(/\\S*)\\s+"
			+ "((?:(?:(?:GET|POST|PUT|DELETE)\\s+)?"
			+ "[a-zA-Z]\\w*(?:\\.[a-zA-Z]\\w*)*(?:\\s+|\\s*$))+)");

	/**
	 * Pattern used to parse handlers in a mapping.
	 */
	private static final Pattern HANDLER_PATTERN = Pattern.compile(
			"(?:^|\\G)(?:(GET|POST|PUT|DELETE)\\s+)?"
			+ "([a-zA-Z]\\w*(?:\\.[a-zA-Z]\\w*)*)\\s*");

	/**
	 * All HTTP methods.
	 */
	private static final Collection<HttpMethod> ALL_METHODS =
		Arrays.asList(HttpMethod.values());


	/**
	 * Endpoint mapping descriptor.
	 */
	private static final class EndpointMappingDesc {

		/**
		 * Original URI pattern.
		 */
		public final String uriPattern;

		/**
		 * Corresponding group number in that master URI pattern.
		 */
		public final int masterPatternGroup;

		/**
		 * Number of URI parameter placeholders.
		 */
		public final int numURIParams;

		/**
		 * The mapping.
		 */
		public final EndpointMapping mapping;


		/**
		 * Create new descriptor.
		 *
		 * @param uriPattern Original URI pattern.
		 * @param masterPatternGroup Corresponding group number in that master
		 * URI pattern.
		 * @param numURIParams Number of URI parameter placeholders.
		 * @param mapping The mapping.
		 */
		EndpointMappingDesc(final String uriPattern,
				final int masterPatternGroup, final int numURIParams,
				final EndpointMapping mapping) {

			this.uriPattern = uriPattern;
			this.masterPatternGroup = masterPatternGroup;
			this.numURIParams = numURIParams;
			this.mapping = mapping;
		}
	}


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Master URI pattern.
	 */
	private final Pattern masterURIPattern;

	/**
	 * Mappings.
	 */
	private final ArrayList<EndpointMappingDesc> mappings;


	/**
	 * Load mappings.
	 *
	 * @param sc Servlet context.
	 * @param resources Application resources manager.
	 *
	 * @throws InitializationException If error happens loading mappings.
	 */
	EndpointMappings(final ServletContext sc, final Resources resources)
		throws InitializationException {

		final StringBuffer masterURIPattern = new StringBuffer(1024);
		int nextMasterPatternGroup = 1;
		this.mappings = new ArrayList<>();
		final String defs = sc.getInitParameter("x2.app.endpoints");
		final Matcher defMatcher = MAPPING_PATTERN.matcher(defs);
		final Matcher handlersMatcher = HANDLER_PATTERN.matcher("");
		final Map<HttpMethod, EndpointCallHandler<?>> handlersToAdd =
			new HashMap<>();
		int lastMatchEnd = 0;
		while (defMatcher.find()) {

			final int masterPatternGroup = nextMasterPatternGroup++;

			final String uriPatternStr = defMatcher.group(1);
			final Pattern uriPattern = Pattern.compile(uriPatternStr);
			final int numURIParams = uriPattern.matcher("").groupCount();
			nextMasterPatternGroup += numURIParams;

			if (masterURIPattern.length() > 0)
				masterURIPattern.append('|');
			masterURIPattern.append('(').append(uriPatternStr).append(')');

			final EndpointMapping mapping = new EndpointMapping();
			handlersMatcher.reset(defMatcher.group(2));
			while (handlersMatcher.find()) {
				final String requestMethodStr = handlersMatcher.group(1);
				final HttpMethod requestMethod =
					(requestMethodStr != null ?
							HttpMethod.valueOf(requestMethodStr) : null);
				final String handlerClassName = handlersMatcher.group(2);
				handlersToAdd.clear();
				try {
					final Class<?> handlerClass =
						Class.forName(handlerClassName);
					if (resources.isPersistentResource(handlerClass)) {
						if (requestMethod != null)
							throw new InitializationException(
									"Endpoint mapping for " + uriPatternStr
									+ " uses persistent resource class and"
									+ " therefore cannot specify HTTP method.");
						createDefaultEndpointCallHandlers(
								resources.getPersistentResourceHandler(
										handlerClass), handlersToAdd);
					} else if ((PersistentResourceEndpointHandler.class)
							.isAssignableFrom(handlerClass)) {
						if (requestMethod != null)
							throw new InitializationException(
									"Endpoint mapping for " + uriPatternStr
									+ " uses persistent resource endpoint"
									+ " handler class and therefore cannot"
									+ " specify HTTP method.");
						createDefaultEndpointCallHandlers(
								handlerClass.asSubclass(
										PersistentResourceEndpointHandler.class)
									.getConstructor(ServletContext.class,
											Resources.class)
									.newInstance(sc, resources),
								handlersToAdd);
					} else {
						final EndpointCallHandler<?> handler = handlerClass
								.asSubclass(EndpointCallHandler.class)
								.newInstance();
						final Collection<HttpMethod> requestMethods =
							(requestMethod != null ?
									Collections.singleton(requestMethod) :
										ALL_METHODS);
						for (final HttpMethod m : requestMethods)
							handlersToAdd.put(m, handler);
					}
				} catch (final ClassCastException
						| ReflectiveOperationException e) {
					throw new InitializationException("Error creating handler"
							+ " for endpoint URI pattern " + uriPatternStr
							+ ".", e);
				}
				for (final Map.Entry<HttpMethod, EndpointCallHandler<?>> entry :
						handlersToAdd.entrySet()) {
					if (!mapping.addHandler(entry.getKey(), entry.getValue()))
						throw new InitializationException("Multiple handlers"
								+ " for the same method for endpoint URI"
								+ " pattern " + uriPatternStr + ".");
				}
			}
			this.mappings.add(new EndpointMappingDesc(uriPatternStr,
					masterPatternGroup, numURIParams, mapping));

			lastMatchEnd = defMatcher.end();
		}
		if (lastMatchEnd != defs.length())
			throw new InitializationException(
					"Invalid endpoint definitions syntax.");
		this.masterURIPattern = Pattern.compile(masterURIPattern.toString());
		this.mappings.trimToSize();

		if (this.log.isDebugEnabled()) {
			final StringBuilder sb = new StringBuilder(1024);
			sb.append("configured endpoint mappings:");
			for (final EndpointMappingDesc mappingDesc : this.mappings) {
				sb.append("\n * ").append(mappingDesc.uriPattern);
				for (final HttpMethod requestMethod :
					mappingDesc.mapping.getAllowedMethods()) {
					sb.append("\n       ").append(requestMethod).append(" -> ")
					.append(mappingDesc.mapping.getHandler(requestMethod)
								.getClass().getName());
				}
			}
			this.log.debug(sb.toString());
		}
	}

	/**
	 * Create endpoint call handlers for the specified persistent resource using
	 * default persistent resource endpoint handler and add the call handlers to
	 * the specified map.
	 *
	 * @param prsrcHandler Persistent resource handler.
	 * @param callHandlers Output map for the created endpoint call handlers.
	 */
	private static void createDefaultEndpointCallHandlers(
			final PersistentResourceHandler<?> prsrcHandler,
			final Map<HttpMethod, EndpointCallHandler<?>> callHandlers) {

		final PersistentResourceEndpointHandler<?> endpointHandler =
			new DefaultPersistentResourceEndpointHandler<>(prsrcHandler);

		createDefaultEndpointCallHandlers(endpointHandler, callHandlers);
	}

	/**
	 * Create endpoint call handler(s) using specified persistent resource
	 * endpoint handler and add the call handler(s) to the specified map.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 * @param callHandlers Output map for the created endpoint call handlers.
	 */
	private static void createDefaultEndpointCallHandlers(
			final PersistentResourceEndpointHandler<?> endpointHandler,
			final Map<HttpMethod, EndpointCallHandler<?>> callHandlers) {

		for (final HttpMethod requestMethod : HttpMethod.values()) {
			final EndpointCallHandler<?> callHandler =
				endpointHandler.getCallHandler(requestMethod);
			if (callHandler != null)
				callHandlers.put(requestMethod, callHandler);
		}
	}


	/**
	 * Find endpoint mapping matching the specified request URI.
	 *
	 * @param requestURI Context-relative request URI.
	 * @param uriParams List, to which to add extracted positional URI
	 * parameters.
	 *
	 * @return Corresponding mapping descriptor, or {@code null} if none
	 * matched.
	 */
	EndpointMapping findMapping(final String requestURI,
			final List<String> uriParams) {

		final Matcher m = this.masterURIPattern.matcher(requestURI);
		if (!m.matches())
			return null;

		for (final EndpointMappingDesc mappingDesc : this.mappings) {
			final int masterPatternGroup = mappingDesc.masterPatternGroup;
			if (m.group(masterPatternGroup) != null) {
				for (int i = 1; i <= mappingDesc.numURIParams; i++)
					uriParams.add(m.group(masterPatternGroup + i));
				return mappingDesc.mapping;
			}
		}

		// should never happen
		throw new RuntimeException(
				"Master pattern matched, but empty master group.");
	}
}
