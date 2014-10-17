package org.bsworks.x2.toolbox.handlers;

import org.bsworks.x2.EndpointCallHandler;


/**
 * Simple abstract implementation for endpoint call handlers that do not use
 * request entity and do not modify any persistent data.
 *
 * @author Lev Himmelfarb
 */
public abstract class ReadOnlyEndpointCallHandler
	implements EndpointCallHandler<Void> {

	/**
	 * Returns {@code null}.
	 */
	@Override
	public Class<Void> getRequestEntityClass() {

		return null;
	}

	/**
	 * Not used, returns {@code null}.
	 */
	@Override
	public Class<?>[] getRequestEntityValidationGroups() {

		return null;
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean isLongJob() {

		return false;
	}

	/**
	 * Returns {@code true}.
	 */
	@Override
	public boolean isReadOnly() {

		return true;
	}
}
