package org.bsworks.x2.services.monitor;

import org.bsworks.x2.EssentialService;


/**
 * Interface of the special service used for internal monitoring of the running
 * instance.
 *
 * @author Lev Himmelfarb
 */
public interface MonitorService
	extends EssentialService {

	/**
	 * Log application error. The method schedules saving the error
	 * asynchronously and returns immediately.
	 *
	 * @param error The error.
	 * @param context The context, in which the error happened.
	 */
	void logApplicationError(Throwable error, ApplicationErrorContext context);
}
