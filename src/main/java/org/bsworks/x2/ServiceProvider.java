package org.bsworks.x2;

import javax.servlet.ServletContext;

import org.bsworks.x2.resource.Resources;


/**
 * Interface for service providers.
 *
 * @param <S> Service interface implemented by the service.
 *
 * @author Lev Himmelfarb
 */
public interface ServiceProvider<S> {

	/**
	 * Get provided service class (or interface).
	 *
	 * @return Service class.
	 */
	Class<? super S> getServiceClass();

	/**
	 * Create service instance. Invoked once during application initialization.
	 * Any exception thrown from the method results in application
	 * initialization failure.
	 *
	 * @param sc Application servlet context.
	 * @param serviceInstanceId Service instance id, or {@code null} if not a
	 * multiple instance service.
	 * @param resources Fully initialized application resources manager.
	 * @param runtimeCtx Instance runtime context. The context passed into this
	 * method is not fully initialized yet. The reference may be stored in
	 * the created service instance, but it should not be used until the
	 * application finishes initialization and enters normal operation state.
	 *
	 * @return The service implementation.
	 *
	 * @throws InitializationException If an error happens and the service
	 * cannot be created. Throwing this exception makes the whole
	 * web-application startup fail.
	 */
	S createService(ServletContext sc, String serviceInstanceId,
			Resources resources, RuntimeContext runtimeCtx)
		throws InitializationException;

	/**
	 * Destroy the service instance. Invoked once during application shut down.
	 * Any exception thrown from the method is logged, but otherwise ignored and
	 * the application shut down procedure continues.
	 *
	 * @param service The service instance to destroy.
	 */
	void destroyService(S service);
}
