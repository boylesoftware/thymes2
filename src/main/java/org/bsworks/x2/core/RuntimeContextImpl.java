package org.bsworks.x2.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletContext;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;
import org.bsworks.x2.services.monitor.MonitorService;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.services.serialization.ResourceSerializationService;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.util.Hex;


/**
 * Instance runtime context implementation.
 *
 * @author Lev Himmelfarb
 */
class RuntimeContextImpl
	implements RuntimeContext {

	/**
	 * Application service holder.
	 *
	 * @param <S> Service type.
	 */
	private static final class ServiceHolder<S> {

		/**
		 * Service provider.
		 */
		public final ServiceProvider<S> provider;

		/**
		 * Service instance.
		 */
		public final S instance;


		/**
		 * Create new holder.
		 *
		 * @param sc Servlet context.
		 * @param resources Application resources manager.
		 * @param runtimeCtx Runtime context being created.
		 * @param provider Service provider.
		 * @param serviceInstanceId Service instance id, or {@code null}.
		 *
		 * @throws InitializationException If an error happens.
		 */
		ServiceHolder(final ServletContext sc, final Resources resources,
				final RuntimeContext runtimeCtx,
				final ServiceProvider<S> provider,
				final String serviceInstanceId)
			throws InitializationException {

			this.provider = provider;
			this.instance = provider.createService(sc, serviceInstanceId,
					resources, runtimeCtx);
		}


		/**
		 * Destroy the service.
		 */
		void destroy() {

			this.provider.destroyService(this.instance);
		}
	}


	/**
	 * The log.
	 */
	private final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Instance id.
	 */
	private final String instanceId;

	/**
	 * Application version.
	 */
	private final String appVersion;

	/**
	 * Authentication token secret key.
	 */
	private final SecretKey appSecretKey;

	/**
	 * Executor service used to execute top-level endpoint calls.
	 */
	private final ExecutorService callExecutorService;

	/**
	 * Executor service used to execute long running jobs.
	 */
	private final ExecutorService jobExecutorService;

	/**
	 * Executor service used to asynchronously execute short background side
	 * tasks.
	 */
	private final ExecutorService sideTaskExecutorService;

	/**
	 * Resources manager.
	 */
	private final Resources resources;

	/**
	 * Resource serialization service holder.
	 */
	private final ServiceHolder<ResourceSerializationService>
	resourceSerializationService;

	/**
	 * Persistence service holder.
	 */
	private final ServiceHolder<PersistenceService> persistenceService;

	/**
	 * Persistent resource versioning service holder.
	 */
	private final ServiceHolder<PersistentResourceVersioningService>
	persistentResourceVersioningService;

	/**
	 * Actor authentication service holder.
	 */
	private final ServiceHolder<ActorAuthenticationService> authService;

	/**
	 * Monitor service holder.
	 */
	private final ServiceHolder<MonitorService> monitorService;

	/**
	 * The authenticator.
	 */
	private final AuthTokenHandler authTokenHandler;

	/**
	 * Validator factory.
	 */
	private final ValidatorFactory validatorFactory;

	/**
	 * Maximum allowed request entity size in bytes (applies only to JSON
	 * request entities).
	 */
	private final int maxRequestSize;

	/**
	 * Additional application services.
	 */
	private final Map<String, ServiceHolder<?>> services;


	/**
	 * Create new runtime context. The constructor is transactional, meaning
	 * that if it throws an exception, it makes sure it shuts down any services
	 * that it managed to initialize before failing.
	 *
	 * @param sc Servlet context.
	 * @param resources Application resources manager.
	 *
	 * @throws InitializationException If an error happens.
	 */
	RuntimeContextImpl(final ServletContext sc, final Resources resources)
		throws InitializationException {

		this.log.info("initializing runtime context");

		// save resources manager
		this.resources = resources;

		// initialize
		boolean success = false;
		try {

			// create instance id
			String hostName;
			try {
				final InetAddress hostAddress = InetAddress.getLocalHost();
				hostName = hostAddress.getHostName();
				final int dotInd = hostName.indexOf('.');
				if (dotInd > 0)
					hostName = hostName.substring(0, dotInd);
			} catch (final UnknownHostException e) {
				this.log.warn("error getting local host address,"
						+ " will use generic host name", e);
				hostName = "localhost";
			}
			this.instanceId = hostName + "." + System.currentTimeMillis();
			this.log.info("instance id " + this.instanceId);

			// read the manifest and get the application version
			final boolean devMode;
			try (final InputStream mfIn = sc.getResourceAsStream(
					sc.getInitParameter(APP_MANIFEST_INITPARAM))) {
				String mfImplVersion = null;
				if (mfIn != null) {
					final Manifest mf = new Manifest(mfIn);
					mfImplVersion = mf.getMainAttributes().getValue(
							Attributes.Name.IMPLEMENTATION_VERSION);
					if (mfImplVersion == null)
						throw new InitializationException(
								"Application manifest at "
								+ sc.getInitParameter(APP_MANIFEST_INITPARAM)
								+ " does not contain "
								+ Attributes.Name.IMPLEMENTATION_VERSION
								+ " header.");
				}
				devMode = ((mfImplVersion == null)
						|| mfImplVersion.startsWith("${")
						|| mfImplVersion.endsWith("-SNAPSHOT"));
				if (devMode)
					this.appVersion =
						(new SimpleDateFormat("yyyyMMddHHmmss")).format(
								new Date()) + "-SNAPSHOT";
				else
					this.appVersion = mfImplVersion;
			} catch (final IOException e) {
				throw new InitializationException(
						"Error reading application manifest file.", e);
			}
			this.log.info("application version " + this.appVersion);

			// announce development mode
			if (devMode) {
				this.log.info("RUNNING IN DEVELOPMENT MODE");
				sc.log("\n###################################################"
					+ "\n# !!! INSTANCE IS RUNNING IN DEVELOPMENT MODE !!! #"
					+ "\n###################################################");
			}

			// get the authentication token secret key
			try {
				this.appSecretKey = new SecretKeySpec(
						Hex.decode(sc.getInitParameter(
								RuntimeContext.AUTH_SECRET_KEY_INITPARAM)),
						sc.getInitParameter(
								RuntimeContext.AUTH_TOKEN_ENC_ALG_INITPARAM));
				final Cipher cipher =
					Cipher.getInstance(this.appSecretKey.getAlgorithm());
				cipher.init(Cipher.ENCRYPT_MODE, this.appSecretKey);
			} catch (final IllegalArgumentException |
					GeneralSecurityException e) {
				throw new InitializationException("Invalid authentication token"
						+ " encryption configuration.", e);
			}

			// create executor services
			this.sideTaskExecutorService =
				createExecutorService(sc, "SideTaskProcessors", -1, this.log);
			this.jobExecutorService =
				createExecutorService(sc, "JobRunners", -1, this.log);
			this.callExecutorService =
				createExecutorService(sc, "EndpointCallProcessors", 0,
						this.log);

			// get resource serialization service
			final ServiceProvider<ResourceSerializationService>
			resourceSerializationServiceProvider = createServiceProvider(
					sc.getInitParameter(SERIALIZATION_PROVIDER_INITPARAM),
					ResourceSerializationService.class);
			this.resourceSerializationService =
				new ServiceHolder<>(sc, this.resources, this,
						resourceSerializationServiceProvider, null);

			// get persistence service
			final ServiceProvider<PersistenceService>
			persistenceServiceProvider = createServiceProvider(
					sc.getInitParameter(PERSISTENCE_PROVIDER_INITPARAM),
					PersistenceService.class);
			this.persistenceService =
				new ServiceHolder<>(sc, this.resources, this,
						persistenceServiceProvider, null);

			// get persistent resource versioning service
			final ServiceProvider<PersistentResourceVersioningService>
			persistentResourceVersioningServiceProvider = createServiceProvider(
					sc.getInitParameter(VERSIONING_PROVIDER_INITPARAM),
					PersistentResourceVersioningService.class);
			this.persistentResourceVersioningService =
				new ServiceHolder<>(sc, this.resources, this,
						persistentResourceVersioningServiceProvider, null);

			// get actor authentication service
			final ServiceProvider<ActorAuthenticationService>
			authServiceProvider = createServiceProvider(
					sc.getInitParameter(AUTH_PROVIDER_INITPARAM),
					ActorAuthenticationService.class);
			this.authService =
				new ServiceHolder<>(sc, this.resources, this,
						authServiceProvider, null);

			// get monitor service
			final ServiceProvider<MonitorService>
			monitorServiceProvider = createServiceProvider(
					sc.getInitParameter(MONITOR_PROVIDER_INITPARAM),
					MonitorService.class);
			this.monitorService =
				new ServiceHolder<>(sc, this.resources, this,
						monitorServiceProvider, null);

			// create AuthToken handler
			this.authTokenHandler = new AuthTokenHandler(sc, this);

			// get bean validator factory
			this.validatorFactory = Validation.buildDefaultValidatorFactory();

			// get maximum allowed request entity size
			this.maxRequestSize = Integer.parseInt(sc.getInitParameter(
					EndpointCallHandler.MAX_REQUEST_SIZE_INITPARAM));

			// create additional application services
			this.services = new HashMap<>();
			try {
				for (final String serviceProviderDef :
					sc.getInitParameter(SERVICE_PROVIDERS_INITPARAM)
						.trim().split("\\s+")) {
					if (serviceProviderDef.isEmpty())
						continue;
					final String serviceProviderClassName;
					final String serviceInstanceId;
					final int colInd = serviceProviderDef.lastIndexOf(':');
					if (colInd < 0) {
						serviceProviderClassName = serviceProviderDef;
						serviceInstanceId = null;
					} else {
						serviceProviderClassName =
							serviceProviderDef.substring(0, colInd);
						serviceInstanceId =
							serviceProviderDef.substring(colInd + 1);
					}
					final ServiceProvider<?> serviceProvider =
						createServiceProvider(serviceProviderClassName, null);
					final String serviceKey =
						serviceProvider.getServiceClass().getName()
						+ (serviceInstanceId != null ?
								":" + serviceInstanceId : "");
					if (this.services.containsKey(serviceKey))
						throw new InitializationException("More than one"
								+ " provider is defined for service "
								+ serviceKey + ".");
					this.log.info("creating service " + serviceKey);
					this.services.put(serviceKey,
							new ServiceHolder<>(sc, this.resources, this,
									serviceProvider, serviceInstanceId));
				}
			} catch (final IndexOutOfBoundsException e) {
				throw new InitializationException("Invalid value of "
						+ SERVICE_PROVIDERS_INITPARAM
						+ " context initialization parameter.", e);
			}

			// done
			success = true;

		} finally {
			if (!success)
				this.shutdown();
		}
	}

	/**
	 * Create executor service.
	 *
	 * @param sc Servlet context.
	 * @param executorServiceName Name of the executor service.
	 * @param threadPriorityShift Threads priority shift.
	 * used to specify number of threads for the executor service.
	 * @param log The log.
	 *
	 * @return The executor service.
	 */
	private static ExecutorService createExecutorService(
			final ServletContext sc, final String executorServiceName,
			final int threadPriorityShift, final Log log) {

		final int numThreads = Integer.parseInt(
				sc.getInitParameter("x2.threads." + executorServiceName));

		final ThreadFactory threadFactory = new ThreadFactory() {

			private final AtomicInteger nextThreadId = new AtomicInteger();

			@Override
			public Thread newThread(final Runnable r) {

				final Thread thread = new Thread(r,
						executorServiceName + "-Thread-"
								+ this.nextThreadId.getAndIncrement());
				if (threadPriorityShift != 0)
					thread.setPriority(
							thread.getPriority() + threadPriorityShift);

				return thread;
			}
		};

		log.info("creating " + executorServiceName + " executor service with "
				+ numThreads + " threads");

		return (numThreads == 1
				? Executors.newSingleThreadExecutor(threadFactory)
				: Executors.newFixedThreadPool(numThreads, threadFactory));
	}

	/**
	 * Create service provider.
	 *
	 * @param providerClassName Service provider class name.
	 * @param serviceClass Optional expected service class, or {@code null} for
	 * no check.
	 *
	 * @return Service provider instance.
	 *
	 * @throws InitializationException If an error happens.
	 */
	@SuppressWarnings("unchecked")
	private static <S> ServiceProvider<S> createServiceProvider(
			final String providerClassName, final Class<?> serviceClass)
		throws InitializationException {

		if ("UNSPECIFIED".equals(providerClassName) && (serviceClass != null))
			throw new InitializationException("No provider for "
					+ serviceClass.getName()
					+ " is defined by the web-application.");

		try {
			final ServiceProvider<?> provider = Class
					.forName(providerClassName)
					.asSubclass(ServiceProvider.class)
					.newInstance();
			if ((serviceClass != null)
					&& !provider.getServiceClass().equals(serviceClass))
				throw new InitializationException("Service provider "
					+ providerClassName + " is expected to provide service "
					+ serviceClass.getName() + ", but provides "
					+ provider.getServiceClass().getName() + " instead.");
			return (ServiceProvider<S>) provider;
		} catch (final ClassNotFoundException | IllegalAccessException |
				InstantiationException e) {
			throw new InitializationException("Error creating service provider "
					+ providerClassName + ".", e);
		}
	}


	/**
	 * Shut down the runtime context.
	 */
	void shutdown() {

		// shutdown executor services
		if (this.callExecutorService != null)
			this.destroyExecutorService("EndpointCallProcessors",
					this.callExecutorService);
		if (this.jobExecutorService != null)
			this.destroyExecutorService("JobRunners",
					this.jobExecutorService);
		if (this.sideTaskExecutorService != null)
			this.destroyExecutorService("SideTaskProcessors",
					this.sideTaskExecutorService);

		// shutdown additional application services
		if (this.services != null) {
			for (final Map.Entry<String, ServiceHolder<?>> entry :
				this.services.entrySet()) {
				final String serviceKey = entry.getKey();
				this.log.info("shutting down service " + serviceKey);
				try {
					entry.getValue().destroy();
				} catch (final Exception e) {
					this.log.warn("error shutting down service " + serviceKey,
							e);
				}
			}
		}

		// shutdown monitor service
		if (this.monitorService != null) {
			try {
				this.monitorService.destroy();
			} catch (final Exception e) {
				this.log.warn("error shutting down monitor service", e);
			}
		}

		// shutdown actor authentication service
		if (this.authService != null) {
			try {
				this.authService.destroy();
			} catch (final Exception e) {
				this.log.warn("error shutting down authentication service", e);
			}
		}

		// shutdown persistent resource versioning service
		if (this.persistentResourceVersioningService != null) {
			try {
				this.persistentResourceVersioningService.destroy();
			} catch (final Exception e) {
				this.log.warn("error shutting down persistent resource"
						+ " versioning service", e);
			}
		}

		// shutdown persistence service
		if (this.persistenceService != null) {
			try {
				this.persistenceService.destroy();
			} catch (final Exception e) {
				this.log.warn("error shutting down persistence service", e);
			}
		}

		// shutdown resource serialization service
		if (this.resourceSerializationService != null) {
			try {
				this.resourceSerializationService.destroy();
			} catch (final Exception e) {
				this.log.warn("error shutting down resource serialization"
						+ " service", e);
			}
		}

		// close bean validator factory
		if (this.validatorFactory != null) {
			try {
				this.validatorFactory.close();
			} catch (final Exception e) {
				this.log.warn("error closing bean validator factory", e);
			}
		}
	}

	/**
	 * Shutdown executor service.
	 *
	 * @param executorServiceName Name of the executor service.
	 * @param executorService The executor service.
	 */
	private void destroyExecutorService(final String executorServiceName,
			final ExecutorService executorService) {

		this.log.info("shutting down " + executorServiceName
				+ " executor service");
		executorService.shutdown();
		try {
			boolean done =
				executorService.awaitTermination(20, TimeUnit.SECONDS);
			if (!done) {
				this.log.info("normal shutdown timed out, forcing shutdown");
				executorService.shutdownNow();
				done = executorService.awaitTermination(10, TimeUnit.SECONDS);
				if (!done)
					this.log.debug("still not shut down, abandoning");
			}
		} catch (final InterruptedException e) {
			this.log.debug("wait interrupted, abandoning", e);
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getInstanceId() {

		return this.instanceId;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getApplicationVersion() {

		return this.appVersion;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SecretKey getAuthSecretKey() {

		return this.appSecretKey;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createAuthToken(final Actor actor) {

		return this.authTokenHandler.createAuthToken(actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void purgeCachedActor(final Actor actor) {

		this.authTokenHandler.purgeCachedActor(actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void submitLongJob(final Runnable task) {

		this.jobExecutorService.submit(task);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void submitSideTask(final Runnable task) {

		this.sideTaskExecutorService.submit(task);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Resources getResources() {

		return this.resources;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourceSerializationService getResourceSerializationService() {

		return this.resourceSerializationService.instance;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceService getPersistenceService() {

		return this.persistenceService.instance;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceVersioningService
	getPersistentResourceVersioningService() {

		return this.persistentResourceVersioningService.instance;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ActorAuthenticationService getActorAuthenticationService() {

		return this.authService.instance;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public MonitorService getMonitorService() {

		return this.monitorService.instance;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <S> S getService(final Class<S> serviceType) {

		return this.getService(serviceType, null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <S> S getService(final Class<S> serviceType,
			final String serviceInstanceId) {

		final S service =
			this.getServiceInternal(serviceType, serviceInstanceId);
		if (service == null)
			throw new IllegalArgumentException("Service "
					+ serviceType.getName()
					+ (serviceInstanceId != null ?
							", instance id " + serviceInstanceId : "")
					+ " does not exist.");

		return service;
	}

	/**
	 * Get application service.
	 *
	 * @param serviceType Service type.
	 * @param serviceInstanceId Service instance id, or {@code null}.
	 *
	 * @return The service, or {@code null} if does not exist.
	 */
	private <S> S getServiceInternal(final Class<S> serviceType,
			final String serviceInstanceId) {

		final String serviceKey = serviceType.getName()
				+ (serviceInstanceId != null ? ":" + serviceInstanceId : "");
		final ServiceHolder<?> serviceHolder = this.services.get(serviceKey);
		if (serviceHolder == null)
			return null;

		return serviceType.cast(serviceHolder.instance);
	}


	/**
	 * Get executor service used to execute top-level endpoint calls.
	 *
	 * @return The endpoint call executor service.
	 */
	ExecutorService getCallExecutorService() {

		return this.callExecutorService;
	}

	/**
	 * Get executor service used to execute long running jobs.
	 *
	 * @return The job executor service.
	 */
	ExecutorService getJobExecutorService() {

		return this.jobExecutorService;
	}

	/**
	 * Get AuthToken handler.
	 *
	 * @return The handler.
	 */
	AuthTokenHandler getAuthTokenHandler() {

		return this.authTokenHandler;
	}

	/**
	 * Get validator factory.
	 *
	 * @return The validator factory.
	 */
	ValidatorFactory getValidatorFactory() {

		return this.validatorFactory;
	}

	/**
	 * Get maximum allowed JSON request entity.
	 *
	 * @return Maximum allowed request entity size in bytes.
	 */
	int getMaxRequestSize() {

		return this.maxRequestSize;
	}
}
