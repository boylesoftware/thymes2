package org.bsworks.x2.services.persistence.impl.dummy;

import javax.servlet.ServletContext;

import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceService;


/**
 * Provider of a "dummy" persistence service implementation that fails at any
 * attempt to perform any operation with the persistent storage. This stub
 * service implementation can be used only if the application does not use
 * persistence.
 *
 * @author Lev Himmelfarb
 */
public class DummyPersistenceServiceProvider
	implements ServiceProvider<PersistenceService> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<PersistenceService> getServiceClass() {

		return PersistenceService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceService createService(final ServletContext sc,
			final String serviceInstanceId, final Resources resources,
			final RuntimeContext runtimeCtx) {

		return new DummyPersistenceService();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final PersistenceService service) {

		// nothing
	}
}
