package org.bsworks.x2.services.auth.impl.prsrc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;
import org.bsworks.x2.services.auth.impl.PasswordActorAuthenticationService;
import org.bsworks.x2.util.StringUtils;


/**
 * Provider of an actor authentication service implementation that uses an
 * application persistent resource for the actor records in the application's
 * persistent storage.
 *
 * <p>The service is configured using the following web-application context
 * initialization parameters:
 *
 * <dl>
 * <dt>{@value #ACTOR_PRSRC_CLASS_INITPARAM}</dt><dd>Fully qualified name of the
 * application persistent resource class used to represent actor records. In
 * addition of being a persistent resource, the class also must implement
 * {@link Actor} interface. There is no default value for this parameter, it
 * must be specified.</dd>
 * <dt>{@value #LOGIN_NAME_PROP_INITPARAM}</dt><dd>Path of the login name
 * property in the actor persistent resource. Must be specified if the
 * implementation uses it.</dd>
 * <dt>{@value #ACTOR_PROPS_INITPARAM}</dt><dd>Comma-separated list of paths of
 * other properties of the actor persistent resource (not including the login
 * name property) that need to be fetched because they participate in creating
 * values returned by the {@link Actor} interface methods. These affect the
 * object returned by the
 * {@link ActorAuthenticationService#getActor(String, String)} method. The
 * {@link PasswordActorAuthenticationService#authenticate(String, String, String)}
 * method always returns the completely fetched resource record.</dd>
 * <dt>{@value #PASSWORD_DIGEST_ALG_INITPARAM}</dt><dd>Password digest
 * algorithm, such as "SHA-1". If not specified, plain password bytes are
 * used.</dd>
 * </dl>
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceActorAuthenticationServiceProvider
	implements ServiceProvider<ActorAuthenticationService> {

	/**
	 * Name of web-application context initialization parameter used to specify
	 * actor persistent resource class.
	 */
	public static final String ACTOR_PRSRC_CLASS_INITPARAM =
		"x2.service.auth.prsrc.actorResourceClass";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * login name property path in the actor persistent resource.
	 */
	public static final String LOGIN_NAME_PROP_INITPARAM =
		"x2.service.auth.prsrc.loginNameProperty";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * list of other actor record properties to fetch.
	 */
	public static final String ACTOR_PROPS_INITPARAM =
		"x2.service.auth.prsrc.actorProperties";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * password digest algorithm.
	 */
	public static final String PASSWORD_DIGEST_ALG_INITPARAM =
		"x2.service.auth.prsrc.passwordDigestAlg";


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
	public ActorAuthenticationService createService(
			final ServletContext sc, final String serviceInstanceId,
			final Resources resources, final RuntimeContext runtimeCtx)
		throws InitializationException {

		LogFactory.getLog(this.getClass()).debug(
				"creating persistent resource actor authentication service");

		// get and validate actor record persistent resource class
		final String actorPRsrcClassName = sc.getInitParameter(
				ACTOR_PRSRC_CLASS_INITPARAM);
		if (actorPRsrcClassName == null)
			throw new InitializationException("Web-application context"
					+ " initialization paramster " + ACTOR_PRSRC_CLASS_INITPARAM
					+ " is not specified.");
		final Class<? extends Actor> actorPRsrcClass;
		try {
			actorPRsrcClass =
				Class.forName(actorPRsrcClassName).asSubclass(Actor.class);
		} catch (final ClassNotFoundException | ClassCastException e) {
			throw new InitializationException(
					"Invalid actor persistent resource class.", e);
		}
		final PersistentResourceHandler<? extends Actor> actorPRsrcHandler;
		try {
			actorPRsrcHandler =
				resources.getPersistentResourceHandler(actorPRsrcClass);
		} catch (final IllegalArgumentException e) {
			throw new InitializationException("Specifiled actor class is not a"
					+ " persistent resource.", e);
		}

		// get and validate login name property
		final String loginNamePropPath =
			sc.getInitParameter(LOGIN_NAME_PROP_INITPARAM);
		if (loginNamePropPath != null) {
			try {
				actorPRsrcHandler.getPersistentPropertyChain(loginNamePropPath);
			} catch (final IllegalArgumentException e) {
				throw new InitializationException("Specified login name"
						+ " property does not exist or is not persistent.", e);
			}
		}

		// get and validate list of other properties to fetch
		final String otherPropPathsStr = StringUtils.nullIfEmpty(
				sc.getInitParameter(ACTOR_PROPS_INITPARAM));
		final String[] otherPropPaths = (otherPropPathsStr == null ?
				new String[0] : otherPropPathsStr.trim().split("\\s*,\\s*"));
		for (final String propPath : otherPropPaths) {
			try {
				actorPRsrcHandler.getPersistentPropertyChain(propPath);
			} catch (final IllegalArgumentException e) {
				throw new InitializationException("Specified actor resource"
						+ " property " + propPath
						+ " does not exist or is not persistent.", e);
			}
		}

		// get and validate password digest algorithm
		final String passwordDigestAlg =
			sc.getInitParameter(PASSWORD_DIGEST_ALG_INITPARAM);
		if (passwordDigestAlg != null) {
			try {
				MessageDigest.getInstance(passwordDigestAlg);
			} catch (final NoSuchAlgorithmException e) {
				throw new InitializationException(
						"Invalid password digest algorithm.", e);
			}
		}

		// create the service
		return this.createActorAuthenticationService(runtimeCtx,
				actorPRsrcClass, loginNamePropPath, otherPropPaths,
				passwordDigestAlg);
	}

	/**
	 * Create service instance. Default implementation returns new instance of
	 * {@link PersistentResourceActorAuthenticationService}.
	 *
	 * @param runtimeCtx Runtime context.
	 * @param actorPRsrcClass Actor persistent resource class.
	 * @param loginNamePropPath Login name property path, or {@code null} if not
	 * used (default implementation requires it!).
	 * @param otherPropPaths Other actor resource properties to fetch. May be
	 * empty but never {@code null}.
	 * @param passwordDigestAlg Password digest algorithm or {@code null} if no
	 * digest.
	 *
	 * @return Service instance.
	 */
	protected ActorAuthenticationService createActorAuthenticationService(
			final RuntimeContext runtimeCtx,
			final Class<? extends Actor> actorPRsrcClass,
			final String loginNamePropPath, final String[] otherPropPaths,
			final String passwordDigestAlg) {

		return new PersistentResourceActorAuthenticationService<>(
				runtimeCtx,
				actorPRsrcClass,
				loginNamePropPath,
				otherPropPaths,
				passwordDigestAlg);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void destroyService(final ActorAuthenticationService service) {

		// nothing
	}
}
