package org.bsworks.x2.toolbox.handlers;

import java.util.List;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.responses.OKResponse;


/**
 * Handler for an endpoint that simply returns currently authenticated actor.
 * The handler requires an authenticated actor. The handler can be used in an
 * application to ensure validity of the client session before making another
 * call.
 *
 * @author Lev Himmelfarb
 */
public class AuthedActorEndpointCallHandler
	extends ReadOnlyEndpointCallHandler {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isAllowed(final HttpMethod requestMethod,
			final String requestURI, final List<String> uriParams,
			final Actor actor) {

		return (actor != null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public EndpointCallResponse handleCall(final EndpointCallContext ctx,
			final Void requestEntity) {

		return new OKResponse(ctx.getActor(), null, null);
	}
}
