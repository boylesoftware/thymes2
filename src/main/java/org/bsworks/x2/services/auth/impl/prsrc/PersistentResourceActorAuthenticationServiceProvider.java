package org.bsworks.x2.services.auth.impl.prsrc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletContext;

import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.ServiceProvider;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;
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
 * <dt>{@value #USERNAME_PROP_INITPARAM}</dt><dd>Path of the username property
 * in the actor persistent resource. If not specified, "username" is used.</dd>
 * <dt>{@value #ACTOR_PROPS_INITPARAM}</dt><dd>Comma-separated list of paths of
 * other properties of the actor persistent resource (not including the username
 * property) that need to be fetched because they participate in creating values
 * returned by the {@link Actor} interface methods.</dd>
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
	 * username property path in the actor persistent resource.
	 */
	public static final String USERNAME_PROP_INITPARAM =
		"x2.service.auth.prsrc.usernameProperty";

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
		if (!resources.isPersistentResource(actorPRsrcClass))
			throw new InitializationException("Specifiled actor class is not a"
					+ " persistent resource.");

		// get and validate username property
		final String usernamePropPath = StringUtils.defaultIfEmpty(
				sc.getInitParameter(USERNAME_PROP_INITPARAM), "username");
		try {
			resources.getPersistentResourceHandler(actorPRsrcClass)
				.getPersistentPropertyChain(usernamePropPath);
		} catch (final IllegalArgumentException e) {
			throw new InitializationException("Specified username property"
					+ " does not exist or is not persistent.", e);
		}

		// get list of other properties to fetch
		final String otherPropPathsStr = StringUtils.nullIfEmpty(
				sc.getInitParameter(ACTOR_PROPS_INITPARAM));
		final String[] otherPropPaths = (otherPropPathsStr == null ?
				new String[0] : otherPropPathsStr.trim().split("\\s*,\\s*"));

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
		return new PersistentResourceActorAuthenticationService(
				runtimeCtx,
				actorPRsrcClass,
				usernamePropPath,
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
