package org.bsworks.x2.services.auth;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EssentialService;


/**
 * Service provided by the web-application to the framework used by the
 * framework to authenticate actors making calls.
 *
 * @author Lev Himmelfarb
 */
public interface ActorAuthenticationService
	extends EssentialService {

	/**
	 * Get actor.
	 *
	 * @param actorId Actor id as in {@link Actor#getActorId()}.
	 * @param opaque Service implementation specific value associated with the
	 * actor, or {@code null} if not used (see {@link Actor#getOpaque()}).
	 *
	 * @return The actor, or {@code null} if not found.
	 */
	Actor getActor(String actorId, String opaque);
}
