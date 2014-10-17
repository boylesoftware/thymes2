package org.bsworks.x2.services.auth.impl;

import org.bsworks.x2.Actor;
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
	 * @param username Username.
	 * @param password Password.
	 * @param opaque Service implementation specific value associated with the
	 * actor, or {@code null} if not used.
	 *
	 * @return The authenticated actor, or {@code null} if username, password
	 * and opaque values combination is invalid.
	 */
	Actor authenticate(String username, String password, String opaque);
}
