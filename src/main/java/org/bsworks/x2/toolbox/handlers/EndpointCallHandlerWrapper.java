package org.bsworks.x2.toolbox.handlers;

import java.util.List;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.HttpMethod;


/**
 * Delegating wrapper for an endpoint call handler.
 *
 * @param <E> Request entity type.
 *
 * @author Lev Himmelfarb
 */
public abstract class EndpointCallHandlerWrapper<E>
	implements EndpointCallHandler<E> {

	/**
	 * The wrapped handler.
	 */
	protected final EndpointCallHandler<E> handler;


	/**
	 * Create new handler wrapper.
	 *
	 * @param handler Handler to wrap.
	 */
	protected EndpointCallHandlerWrapper(final EndpointCallHandler<E> handler) {

		this.handler = handler;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isAllowed(final HttpMethod requestMethod,
			final String requestURI, final List<String> uriParams,
			final Actor actor) {

		return this.handler.isAllowed(requestMethod, requestURI, uriParams,
				actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<E> getRequestEntityClass() {

		return this.handler.getRequestEntityClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?>[] getRequestEntityValidationGroups() {

		return this.handler.getRequestEntityValidationGroups();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isLongJob() {

		return this.handler.isLongJob();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isReadOnly() {

		return this.handler.isReadOnly();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public EndpointCallResponse handleCall(final EndpointCallContext ctx,
			final E requestEntity)
		throws EndpointCallErrorException {

		return this.handler.handleCall(ctx, requestEntity);
	}
}
