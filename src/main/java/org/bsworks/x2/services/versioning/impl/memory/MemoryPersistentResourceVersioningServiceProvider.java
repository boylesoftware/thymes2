package org.bsworks.x2.services.versioning.impl.memory;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;


/**
 * Provider of a simple {@link PersistentResourceVersioningService}
 * implementation that keeps versioning information in the instance memory. This
 * is the default implementation, which is suitable for single instance setups.
 * For multi-instance setups, a more sophisticated implementation is
 * recommended.
 *
 * @author Lev Himmelfarb
 */
public class MemoryPersistentResourceVersioningServiceProvider
	implements ServiceProvider<PersistentResourceVersioningService> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<PersistentResourceVersioningService> getServiceClass() {

		return PersistentResourceVersioningService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceVersioningService createService(
			final ServletContext sc, final String serviceInstanceId,
			final Resources resources, final RuntimeContext runtimeCtx) {

		LogFactory.getLog(this.getClass()).debug("creating memory-based"
				+ " persistent resource collections versioning service");

		return new MemoryPersistentResourceVersioningService();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(
			final PersistentResourceVersioningService service) {

		// nothing
	}
}
