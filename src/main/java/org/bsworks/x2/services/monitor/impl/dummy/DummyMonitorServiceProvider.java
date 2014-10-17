package org.bsworks.x2.services.monitor.impl.dummy;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.monitor.MonitorService;


/**
 * Provider for a "dummy" monitor service implementation that does nothing. The
 * "dummy" service is configured by default unless overridden by the
 * web-application.
 *
 * @author Lev Himmelfarb
 */
public class DummyMonitorServiceProvider
	implements ServiceProvider<MonitorService> {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<MonitorService> getServiceClass() {

		return MonitorService.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public MonitorService createService(final ServletContext sc,
			final String serviceInstanceId, final Resources resources,
			final RuntimeContext runtimeCtx) {

		LogFactory.getLog(this.getClass()).debug(
				"creating dummy monitor service");

		return new DummyMonitorService();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final MonitorService service) {

		// nothing
	}
}
