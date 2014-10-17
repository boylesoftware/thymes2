package org.bsworks.x2.services.monitor.impl.dummy;

import org.bsworks.x2.services.monitor.ApplicationErrorContext;
import org.bsworks.x2.services.monitor.MonitorService;


/**
 * Dummy monitor service implementation.
 *
 * @author Lev Himmelfarb
 */
class DummyMonitorService
	implements MonitorService {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void logApplicationError(final Throwable error,
			final ApplicationErrorContext context) {

		// nothing
	}
}
