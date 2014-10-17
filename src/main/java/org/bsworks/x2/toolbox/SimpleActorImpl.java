package org.bsworks.x2.toolbox;

import java.io.Serializable;
import java.util.Set;

import javax.crypto.SecretKey;

import org.bsworks.x2.Actor;


/**
 * Simple, generic implementation of {@link Actor} that can be used by
 * applications.
 *
 * @author Lev Himmelfarb
 */
public class SimpleActorImpl
	implements Actor, Serializable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * The username.
	 */
	private final String username;

	/**
	 * The "opaque" value.
	 */
	private final String opaque;

	/**
	 * Actor secret key.
	 */
	private final SecretKey secretKey;

	/**
	 * Actor credentials.
	 */
	private final byte[] credentials;

	/**
	 * Actor roles.
	 */
	private final Set<String> roles;


	/**
	 * Create new actor object.
	 *
	 * @param username The username.
	 * @param opaque Application-specific value always attached to the
	 * authentication-related HTTP headers, or {@code null} if not used.
	 * @param secretKey Actor secret key.
	 * @param credentials Actor credentials (such as the password represented as
	 * bytes).
	 * @param roles Actor roles.
	 */
	public SimpleActorImpl(final String username, final String opaque,
			final SecretKey secretKey, final byte[] credentials,
			final Set<String> roles) {

		this.username = username;
		this.opaque = opaque;
		this.secretKey = secretKey;
		this.credentials = credentials;
		this.roles = roles;
	}


	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#getUsername()
	 */
	@Override
	public String getUsername() {

		return this.username;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#getOpaque()
	 */
	@Override
	public String getOpaque() {

		return this.opaque;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#getSecretKey()
	 */
	@Override
	public SecretKey getSecretKey() {

		return this.secretKey;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#getCredentials()
	 */
	@Override
	public byte[] getCredentials() {

		return this.credentials;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#hasRole(java.lang.String)
	 */
	@Override
	public boolean hasRole(final String role) {

		return this.roles.contains(role);
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.Actor#hasAnyRole(java.util.Set)
	 */
	@Override
	public boolean hasAnyRole(final Set<String> roles) {

		for (final String r : this.roles)
			if (roles.contains(r))
				return true;

		return false;
	}
}
