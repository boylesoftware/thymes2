package org.bsworks.x2;

import java.util.Set;

import javax.crypto.SecretKey;

import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * Actor, such as a user, performing operation. Actor objects are provided by an
 * implementation of {@link ActorAuthenticationService}, provided by the
 * web-application. Actor objects are used by the framework for the "AuthToken"
 * HTTP authentication scheme.
 *
 * @author Lev Himmelfarb
 */
public interface Actor {

	/**
	 * Get actor username.
	 *
	 * @return The username.
	 */
	String getUsername();

	/**
	 * Get actor authentication service implementation specific value associated
	 * with the actor. The opaque value, if used, is communicated in an
	 * unencrypted form between the client and the server using HTTP headers
	 * "Authentication-Info" and "Authorization".
	 *
	 * @return The opaque value, or {@code null} if not used.
	 */
	String getOpaque();

	/**
	 * Get secret key associated with the actor. The secret key is used by the
	 * framework to encrypt and decrypt the authentication token passed between
	 * the client and the server using HTTP headers "Authentication-Info" and
	 * "Authorization".
	 *
	 * <p>The secret key is normally assigned by the web-application and is not
	 * known to the actor. It is recommended to have an individual secret key
	 * for each individual actor. This way, if the secret key is compromised for
	 * a given actor, only that actor's account is compromised. In simple
	 * setups, however, that do not support storing additional secret key with
	 * the actor account, the key can be shared for the whole web-application.
	 *
	 * <p>The symmetric encryption algorithm used by the framework to encrypt
	 * and decrypt the authentication token is taken from this key. "AES"
	 * algorithm is recommended and most commonly used.
	 *
	 * @return The secret key.
	 */
	SecretKey getSecretKey();

	/**
	 * Get value the represents actor credentials. Actor credentials are known
	 * to the actor, but not anyone else. It can be, for example, a digest of
	 * the actor password. The credentials value is included in the encrypted
	 * authentication token.
	 *
	 * @return The credentials.
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
