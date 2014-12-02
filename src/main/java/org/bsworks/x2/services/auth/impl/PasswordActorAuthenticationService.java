package org.bsworks.x2.services.auth.impl;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * Extension of the actor authentication service interface for services that
 * use password-based authentication.
 *
 * @author Lev Himmelfarb
 */
public interface PasswordActorAuthenticationService
	extends ActorAuthenticationService {

	/**
	 * Authenticate actor.
	 *
	 * @param ctx Endpoint call context.
	 * @param loginName User login name.
	 * @param password User password.
	 * @param opaque Service implementation specific value associated with the
	 * actor, or {@code null} if not used.
	 *
	 * @return The authenticated actor, or {@code null} if login name, password
	 * and opaque value combination is invalid.
	 */
	Actor authenticate(EndpointCallContext ctx, String loginName,
			String password, String opaque);
}
