package org.bsworks.x2.services.auth.impl.prsrc;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.bsworks.x2.Actor;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.impl.PasswordActorAuthenticationService;
import org.bsworks.x2.services.persistence.PersistenceTransactionHandler;


/**
 * Persistent resource actor authentication service implementation.
 *
 * @author Lev Himmelfarb
 */
class PersistentResourceActorAuthenticationService
	implements PasswordActorAuthenticationService {

	/**
	 * Application runtime context.
	 */
	private final RuntimeContext runtimeCtx;

	/**
	 * Class of the persistent resource representing actor record.
	 */
	private final Class<? extends Actor> actorPRsrcClass;

	/**
	 * Username property path.
	 */
	private final String usernamePropPath;

	/**
	 * Other property paths.
	 */
	private final String[] otherPropPaths;

	/**
	 * Password digest algorithm, or {@code null} for no digest.
	 */
	private final String passwordDigestAlg;


	/**
	 * Create new service instance.
	 *
	 * @param runtimeCtx Application runtime context.
	 * @param actorPRsrcClass Class of the persistent resource representing
	 * actor record.
	 * @param usernamePropPath Username property path.
	 * @param otherPropPaths Other property paths.
	 * @param passwordDigestAlg Password digest algorithm, or {@code null} for
	 * no digest.
	 */
	PersistentResourceActorAuthenticationService(
			final RuntimeContext runtimeCtx,
			final Class<? extends Actor> actorPRsrcClass,
			final String usernamePropPath, final String[] otherPropPaths,
			final String passwordDigestAlg) {

		this.runtimeCtx = runtimeCtx;
		this.actorPRsrcClass = actorPRsrcClass;
		this.usernamePropPath = usernamePropPath;
		this.otherPropPaths = otherPropPaths;
		this.passwordDigestAlg = passwordDigestAlg;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor getActor(final String username, final String opaque) {

		final Actor actor;
		try (final PersistenceTransactionHandler txh =
				this.runtimeCtx.getPersistenceService()
					.createPersistenceTransaction(null, true)) {

			actor = this.getActor(this.actorPRsrcClass, txh, username);

			txh.commitTransaction();
		}

		return actor;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor authenticate(final String username, final String password,
			final String opaque) {

		final Actor actor = this.getActor(username, opaque);
		if (actor == null)
			return null;

		final byte[] creds;
		if (this.passwordDigestAlg != null) {
			try {
				creds = MessageDigest.getInstance(this.passwordDigestAlg)
						.digest(password.getBytes(Charset.forName("UTF-8")));
			} catch (final NoSuchAlgorithmException e) {
				throw new RuntimeException("Invalid password digest algorithm.",
						e);
			}
		} else {
			creds = password.getBytes(Charset.forName("UTF-8"));
		}

		if (!Arrays.equals(creds, actor.getCredentials()))
			return null;

		return actor;
	}

	/**
	 * Get the actor record.
	 *
	 * @param prsrcClass Actor persistent resource class.
	 * @param txh Transaction handler.
	 * @param username Actor username.
	 *
	 * @return Actor record, or {@code null} if not found.
	 */
	private <A extends Actor> A getActor(final Class<A> prsrcClass,
			final PersistenceTransactionHandler txh, final String username) {

		final Resources resources = this.runtimeCtx.getResources();
		final PropertiesFetchSpec<A> propsFetch =
			resources.getPropertiesFetchSpec(prsrcClass);
		propsFetch.include(this.usernamePropPath);
		for (final String propPath : this.otherPropPaths)
			propsFetch.include(propPath);

		return txh
				.getTransaction()
				.createPersistentResourceFetch(prsrcClass)
				.setPropertiesFetch(propsFetch)
				.setFilter(resources
						.getFilterSpec(prsrcClass)
						.addCondition(this.usernamePropPath,
								FilterConditionType.EQ, false,
								username))
				.getFirstResult();
	}
}
