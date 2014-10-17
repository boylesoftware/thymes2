package org.bsworks.x2.services.serialization.impl.json;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.serialization.ResourceSerializationService;


/**
 * Provider of a resource serialization service that represents serialized
 * resources as JSON.
 *
 * @author Lev Himmelfarb
 */
public class JsonResourceSerializationServiceProvider
	implements ServiceProvider<ResourceSerializationService> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<ResourceSerializationService> getServiceClass() {

		return ResourceSerializationService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceSerializationService createService(final ServletContext sc,
			final String serviceInstanceId, final Resources resources,
			final RuntimeContext runtimeCtx) {

		LogFactory.getLog(this.getClass()).debug(
				"creating JSON resource serialization service");

		return new JsonResourceSerializationService(resources);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final ResourceSerializationService service) {

		// nothing
	}
}
