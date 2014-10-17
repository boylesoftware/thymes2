package org.bsworks.x2.services.auth.impl.dummy;

import org.bsworks.x2.Actor;
import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * "Dummy" actor authentication service implementation.
 *
 * @author Lev Himmelfarb
 */
class DummyActorAuthenticationService
	implements ActorAuthenticationService {

	/**
	 * Always returns {@code null}.
	 */
	@Override
	public Actor getActor(final String username, final String opaque) {

		return null;
	}
}
