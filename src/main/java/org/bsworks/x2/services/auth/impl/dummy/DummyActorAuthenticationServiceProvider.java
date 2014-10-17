package org.bsworks.x2.services.auth.impl.dummy;

import javax.servlet.ServletContext;

import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * Provider of a "dummy" implementation of actor authentication service that
 * allows only unauthenticated requests. The "dummy" service is configured by
 * default unless overridden by the web-application.
 *
 * @author Lev Himmelfarb
 */
public class DummyActorAuthenticationServiceProvider
	implements ServiceProvider<ActorAuthenticationService> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<ActorAuthenticationService> getServiceClass() {

		return ActorAuthenticationService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ActorAuthenticationService createService(final ServletContext sc,
			final String serviceInstanceId, final Resources resources,
			final RuntimeContext runtimeCtx) {

		return new DummyActorAuthenticationService();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final ActorAuthenticationService service) {

		// nothing
	}
}
