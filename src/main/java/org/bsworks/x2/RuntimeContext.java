package org.bsworks.x2;

import javax.crypto.SecretKey;

import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;
import org.bsworks.x2.services.monitor.MonitorService;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.services.serialization.ResourceSerializationService;
import org.bsworks.x2.services.serialization.impl.json.JsonResourceSerializationServiceProvider;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.services.versioning.impl.memory.MemoryPersistentResourceVersioningServiceProvider;


/**
 * Instance runtime context.
 *
 * @author Lev Himmelfarb
 */
public interface RuntimeContext {

	/**
	 * Name of web-application context initialization parameter used to
	 * configure resource serialization service provider.
	 */
	static final String SERIALIZATION_PROVIDER_INITPARAM =
		"x2.service.serialization.provider";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure persistence service provider.
	 */
	static final String PERSISTENCE_PROVIDER_INITPARAM =
		"x2.service.persistence.provider";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure persistent resource versioning service provider.
	 */
	static final String VERSIONING_PROVIDER_INITPARAM =
		"x2.service.versioning.provider";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure actor authentication service provider.
	 */
	static final String AUTH_PROVIDER_INITPARAM =
		"x2.service.auth.provider";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure internal application monitor service provider.
	 */
	static final String MONITOR_PROVIDER_INITPARAM =
		"x2.service.monitor.provider";

	/**
	 * Name of web-application context initialization parameter used to
	 * configure additional application service providers.
	 */
	static final String SERVICE_PROVIDERS_INITPARAM =
		"x2.app.serviceProviders";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * the path to the application manifest file.
	 */
	static final String APP_MANIFEST_INITPARAM =
		"x2.app.manifest";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * application-wide secret key used in particular for endpoint calls
	 * authentication but also available to the application via the
	 * {@link #getAuthSecretKey()} method.
	 */
	static final String AUTH_SECRET_KEY_INITPARAM =
		"x2.auth.secretKey";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * the name of the symmetric algorithm used in the endpoint calls
	 * authentication token encryption. The algorithm matches the secret key
	 * specified by the {@value #AUTH_SECRET_KEY_INITPARAM} context
	 * initialization parameter.
	 */
	static final String AUTH_TOKEN_ENC_ALG_INITPARAM =
		"x2.auth.tokenEncAlg";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * the validity period of an authentication token used for the endpoint
	 * calls authentication. The time period is specified in milliseconds.
	 */
	static final String AUTH_TOKEN_TTL_INITPARAM =
		"x2.auth.tokenTTL";


	/**
	 * Get instance id. A unique application instance id is assigned to the
	 * instance automatically by the framework when the instance starts. To
	 * create an instance id value, the framework may use such material as the
	 * host name or IP address and the current system time.
	 *
	 * @return Instance id.
	 */
	String getInstanceId();

	/**
	 * Get version of the application that is using the framework. The version
	 * is read from "Implementation-Version" header of the manifest file
	 * specified by the {@value #APP_MANIFEST_INITPARAM} context initialization
	 * parameter. The parameter specifies the path relative to the
	 * web-application root. Unless overridden, the default is
	 * "/META-INF/MANIFEST.MF".
	 *
	 * <p>There are several conditions that make the instance run in so called
	 * development mode:
	 *
	 * <ul>
	 * <li>The manifest file is not present.</li>
	 * <li>The "Implementation-Version" value starts with "${" or ends with
	 * "-SNAPSHOT".</li>
	 * </ul>
	 *
	 * <p>In development mode, this method returns "yyyyMMddHHmmSS-SNAPSHOT"
	 * string with the instance startup timestamp.
	 *
	 * <p>Note, that if manifest file is present, it must contain
	 * "Implementation-Version" header, or the application will fail to start.
	 *
	 * @return The application version.
	 */
	String getApplicationVersion();

	/**
	 * Get application-wide secret key used, in particular, for the endpoint
	 * calls authentication. The key can be conveniently used by the custom
	 * application for encryption of various application-specific tokens and
	 * other data. The key and the encryption algorithm are specified by
	 * {@value #AUTH_SECRET_KEY_INITPARAM} and
	 * {@value #AUTH_TOKEN_ENC_ALG_INITPARAM} web-application context
	 * initialization parameters.
	 *
	 * @return The secret key.
	 */
	SecretKey getAuthSecretKey();

	/**
	 * Submit a job for execution in a "long job" thread. "Long jobs" are
	 * executed in a dedicated, low-priority thread.
	 *
	 * <p>The size of the thread pool used for "long jobs" can be configured
	 * using "x2.threads.JobRunners" web-application context initialization
	 * parameter. The default is 1 (a single thread).
	 *
	 * <p>The method submits the job for asynchronous execution and returns
	 * immediately.
	 *
	 * @param task The job.
	 */
	void submitLongJob(Runnable task);

	/**
	 * Submit a short, "fire-and-forget" task for asynchronous execution. Such
	 * tasks are executed in a separate low-priority thread. The thread pool
	 * size is configured by "x2.threads.SideTaskProcessors" web-application
	 * context initialization parameter. The default is 1 (a single thread).
	 *
	 * <p>The method submits the task for asynchronous execution and returns
	 * immediately.
	 *
	 * @param task The task.
	 */
	void submitSideTask(Runnable task);

	/**
	 * Get application resources manager.
	 *
	 * @return The resources manager.
	 */
	Resources getResources();

	/**
	 * Get resource serialization service used by the application.
	 *
	 * <p>The service implementation is configured by specifying a
	 * web-application context initialization parameter called
	 * {@value #SERIALIZATION_PROVIDER_INITPARAM}. The value must be a fully
	 * qualified class name of a {@link ServiceProvider} implementation that
	 * provides {@link ResourceSerializationService}. By default,
	 * {@link JsonResourceSerializationServiceProvider} is used.
	 *
	 * @return The resource serialization service.
	 */
	ResourceSerializationService getResourceSerializationService();

	/**
	 * Get persistence service used by the application.
	 *
	 * <p>The service implementation is configured by specifying a
	 * web-application context initialization parameter called
	 * {@value #PERSISTENCE_PROVIDER_INITPARAM}. The value must be a fully
	 * qualified class name of a {@link ServiceProvider} implementation that
	 * provides {@link PersistenceService}. There is no default value. The
	 * application must configure the provider.
	 *
	 * @return The persistence service.
	 */
	PersistenceService getPersistenceService();

	/**
	 * Get persistent resource versioning service used by the application.
	 *
	 * <p>The service implementation is configured by specifying a
	 * web-application context initialization parameter called
	 * {@value #VERSIONING_PROVIDER_INITPARAM}. The value must be a fully
	 * qualified class name of a {@link ServiceProvider} implementation that
	 * provides {@link PersistentResourceVersioningService}. By default,
	 * {@link MemoryPersistentResourceVersioningServiceProvider} is used.
	 *
	 * @return The persistent resource versioning service.
	 */
	PersistentResourceVersioningService
	getPersistentResourceVersioningService();

	/**
	 * Get actor authentication service used by the application.
	 *
	 * <p>The service implementation is configured by specifying a
	 * web-application context initialization parameter called
	 * {@value #AUTH_PROVIDER_INITPARAM}. The value must be a fully qualified
	 * class name of a {@link ServiceProvider} implementation that provides
	 * {@link ActorAuthenticationService}. There is no default value. The
	 * application must configure the provider.
	 *
	 * @return The actor authentication service.
	 */
	ActorAuthenticationService getActorAuthenticationService();

	/**
	 * Get internal application monitor service used by the application.
	 *
	 * <p>The service implementation is configured by specifying a
	 * web-application context initialization parameter called
	 * {@value #MONITOR_PROVIDER_INITPARAM}. The value must be a fully qualified
	 * class name of a {@link ServiceProvider} implementation that provides
	 * {@link MonitorService}. There is no default value. The application must
	 * configure the provider.
	 *
	 * @return The monitor service.
	 */
	MonitorService getMonitorService();

	/**
	 * Get additional application service of the given type.
	 *
	 * <p>Additional services used by the application are configured using a
	 * web-application context initialization parameter called
	 * {@value #SERVICE_PROVIDERS_INITPARAM}, which is a whitespace-separated
	 * list of fully qualified class names of {@link ServiceProvider}
	 * implementations. Optionally, any provider class name can be appended with
	 * a colon and a service instance id, which allows having multiple service
	 * instances for a given service interface. In such case, the service
	 * instance must be retrieved using {@link #getService(Class, String)}
	 * method.
	 *
	 * @param <S> The service type.
	 * @param serviceType The service type, usually an interface.
	 *
	 * @return The service.
	 *
	 * @throws IllegalArgumentException If service of the specified type does
	 * not exist.
	 */
	<S> S getService(Class<S> serviceType);

	/**
	 * Get additional application service of the given type. This method is used
	 * when multiple service instances exist for the same type. Different
	 * instances may be different {@code serviceType} implementations or may be
	 * configured differently for different purposes.
	 *
	 * @param <S> The service type.
	 * @param serviceType The service type, usually an interface.
	 * @param serviceInstanceId Service instance id.
	 *
	 * @return The service.
	 *
	 * @throws IllegalArgumentException If service of the specified type and
	 * instance id does not exist.
	 */
	<S> S getService(Class<S> serviceType, String serviceInstanceId);
}
