package org.bsworks.x2.services.auth.impl.prsrc;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.RuntimeContext;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.impl.PasswordActorAuthenticationService;
import org.bsworks.x2.services.persistence.PersistenceTransactionHandler;


/**
 * Persistent resource actor authentication service implementation.
 *
 * @param <A> Actor persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceActorAuthenticationService<A extends Actor>
	implements PasswordActorAuthenticationService {

	/**
	 * Application runtime context.
	 */
	private final RuntimeContext runtimeCtx;

	/**
	 * Class of the persistent resource representing actor record.
	 */
	private final Class<A> actorPRsrcClass;

	/**
	 * Login name property path.
	 */
	private final String loginNamePropPath;

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
	 * @param loginNamePropPath Login name property path.
	 * @param otherPropPaths Other property paths.
	 * @param passwordDigestAlg Password digest algorithm, or {@code null} for
	 * no digest.
	 */
	PersistentResourceActorAuthenticationService(
			final RuntimeContext runtimeCtx,
			final Class<A> actorPRsrcClass, final String loginNamePropPath,
			final String[] otherPropPaths, final String passwordDigestAlg) {

		this.runtimeCtx = runtimeCtx;
		this.actorPRsrcClass = actorPRsrcClass;
		this.loginNamePropPath = loginNamePropPath;
		this.otherPropPaths = otherPropPaths;
		this.passwordDigestAlg = passwordDigestAlg;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public A getActor(final String actorId, final String opaque) {

		final Resources resources = this.runtimeCtx.getResources();

		final PersistentResourceHandler<A> actorPRsrcHandler =
			resources.getPersistentResourceHandler(this.actorPRsrcClass);
		final IdPropertyHandler idPropHandler =
			actorPRsrcHandler.getIdProperty();

		final PropertiesFetchSpec<A> propsFetch =
			resources.getPropertiesFetchSpec(this.actorPRsrcClass);
		propsFetch.include(this.loginNamePropPath);
		for (final String propPath : this.otherPropPaths)
			propsFetch.include(propPath);

		final A actor;
		try (final PersistenceTransactionHandler txh =
				this.runtimeCtx.getPersistenceService()
					.createPersistenceTransaction(null, true)) {

			actor = txh
					.getTransaction()
					.createPersistentResourceFetch(this.actorPRsrcClass)
					.setPropertiesFetch(propsFetch)
					.setFilter(resources.getFilterSpec(this.actorPRsrcClass)
							.addTrueCondition(idPropHandler.getName(),
									FilterConditionType.EQ,
									idPropHandler.getValueHandler().valueOf(
											actorId)))
					.getFirstResult();

			txh.commitTransaction();

		} catch (final InvalidResourceDataException e) {
			return null;
		}

		return actor;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor authenticate(final EndpointCallContext ctx,
			final String loginName, final String password,
			final String opaque) {

		final FilterSpec<A> filter =
			this.runtimeCtx.getResources().getFilterSpec(this.actorPRsrcClass);
		this.addAuthenticationFilter(filter, loginName, opaque);

		final A actor = ctx
				.getPersistenceTransaction()
				.createPersistentResourceFetch(this.actorPRsrcClass)
				.setFilter(filter)
				.getFirstResult();

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
	 * Add conditions to the filter for the
	 * {@link #authenticate(EndpointCallContext, String, String, String)}
	 * method. The default implementation adds condition for the login name
	 * equality.
	 *
	 * @param filter The filter, to which to add conditions.
	 * @param loginName Supplied login name.
	 * @param opaque Supplied opaque value.
	 */
	protected void addAuthenticationFilter(final FilterSpec<A> filter,
			final String loginName,
			@SuppressWarnings("unused") final String opaque) {

		filter.addTrueCondition(this.loginNamePropPath, FilterConditionType.EQ,
				loginName);
	}
}
