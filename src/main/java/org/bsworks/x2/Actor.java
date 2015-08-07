package org.bsworks.x2;

import java.util.Set;

import javax.crypto.SecretKey;

import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * Actor, such as a user, performing an operation. Actor objects are looked up
 * by the framework in an implementation of {@link ActorAuthenticationService},
 * provided by the web-application.
 *
 * @author Lev Himmelfarb
 */
public interface Actor {

	/**
	 * Get actor id in the actor authentication service. The actor id together
	 * with the opaque value, if used, are used to lookup the actor in the
	 * authentication service. The actor id is included in the authentication
	 * token.
	 *
	 * @return The actor id.
	 */
	String getActorId();

	/**
	 * Get actor record version in the actor authentication service. Whenever
	 * the actor data provided by the authentication service changes (for
	 * example, the actor roles change), a new version must be reported. The
	 * resources available via the application API may be presented differently
	 * for different actors depending on the actor roles. For that reason, the
	 * resources included in the API responses are tagged with the actor id,
	 * actor version and the actor opaque value.
	 *
	 * @return The actor record version, or {@code null} if not provided by the
	 * authentication service.
	 */
	String getActorVersion();

	/**
	 * Get actor name. The actor name is used by the framework to identify the
	 * actor for logging and auditing purposes. That includes, for example,
	 * such persistent resource meta-properties as "created by" and
	 * "last modified by". The actor name is not used to identify the actor and
	 * can change over time.
	 *
	 * @return The actor name.
	 */
	String getActorName();

	/**
	 * Get value associated with the actor by the authentication service
	 * implementation. The opaque value, if used, is included in the
	 * authentication token and together with the actor id is used to lookup the
	 * actor in the authentication service. The exact meaning of the value is
	 * specific to the implementation and therefore is "opaque" from the point
	 * of view of the framework.
	 *
	 * @return The opaque value, or {@code null} if not used.
	 */
	String getOpaque();

	/**
	 * Get secret key associated with the actor. The secret key is used by the
	 * framework to encrypt and decrypt the authentication token.
	 *
	 * <p>The secret key is normally assigned by the web-application and is not
	 * known to the actor. It is recommended to have an individual secret key
	 * for each individual actor. That way, if the secret key is compromised for
	 * a given actor, only that actor's account is compromised. In simple
	 * setups, however, that do not support storing additional secret keys with
	 * actor accounts, the key can be shared for the whole web-application.
	 *
	 * <p>The symmetric encryption algorithm used by the framework to encrypt
	 * and decrypt the authentication token is determined by this key. "AES"
	 * algorithm is recommended and most commonly used.
	 *
	 * @return The secret key, or {@code null} to use application-wide key for
	 * the authentication token encryption.
	 */
	SecretKey getSecretKey();

	/**
	 * Get value the represents actor credentials. Actor credentials are known
	 * to the actor, but not anyone else. It can be, for example, a digest of
	 * the actor password. The credentials value is included in the encrypted
	 * authentication token.
	 *
	 * @return The credentials. Must not be {@code null}.
	 */
	byte[] getCredentials();

	/**
	 * Tell if the actor has the specified security role.
	 *
	 * @param role The role.
	 *
	 * @return {@code true} if the actor has the role.
	 */
	boolean hasRole(String role);

	/**
	 * Tell if the actor has any of the specified security roles.
	 *
	 * @param roles The roles.
	 *
	 * @return {@code true} if the actor has any of the roles.
	 */
	boolean hasAnyRole(Set<String> roles);
}
