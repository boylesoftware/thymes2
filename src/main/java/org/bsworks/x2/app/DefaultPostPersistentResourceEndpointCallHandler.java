package org.bsworks.x2.app;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.responses.CreatedResponse;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;


/**
 * Default implementation of a persistent resource endpoint call handler for
 * HTTP POST requests. The handler takes persistent resource record template in
 * the request body and attempts to create a new record. The handler assumes
 * that the last URI parameter in the endpoint URI mapping is for an existing
 * record id. It makes sure that the call URL does not include the record id
 * parameter. If it does, the handler sends HTTP error 405 (Method Not Allowed)
 * back to the client.
 *
 * <p>The handler does not utilize conditional HTTP requests, but upon success,
 * it does generate "ETag" and "Last-Modified" HTTP response headers.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultPostPersistentResourceEndpointCallHandler<R>
	extends AbstractPersistentResourceEndpointCallHandler<R, R> {

	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	public DefaultPostPersistentResourceEndpointCallHandler(
			final PersistentResourceEndpointHandler<R> endpointHandler) {
		super(endpointHandler);
	}


	/**
	 * Returns handled persistent resource class.
	 */
	@Override
	public final Class<R> getRequestEntityClass() {

		return this.prsrcClass;
	}

	/**
	 * The default implementation returns
	 * {@link EndpointCallHandler#CREATE_VAIDATION_GROUPS}.
	 */
	@Override
	public Class<?>[] getRequestEntityValidationGroups() {

		return CREATE_VAIDATION_GROUPS;
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public final boolean isLongJob() {

		return false;
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public final boolean isReadOnly() {

		return false;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public EndpointCallResponse handleCall(final EndpointCallContext ctx,
			final R requestEntity)
		throws EndpointCallErrorException {

		// make sure there is not record id in the call URL
		if (this.getAddressedRecordId(ctx) != null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED, null,
					"Existing record id is in the URL.");

		// persist the record
		this.endpointHandler.create(ctx, requestEntity);

		// register persistent resource collection modification
		ctx.registerPersistentResourceModification(this.prsrcClass);

		// get referred dependent resource collections versions
		final PersistentResourceVersionInfo colsVerInfo =
			this.getDependentResourcesVersioningInfo(ctx, null);

		// return the record in the response
		return new CreatedResponse(requestEntity,
				ctx.getRequestURI() + "/"
						+ this.idPropHandler.getValue(requestEntity),
				this.getResourceETag(ctx, requestEntity, colsVerInfo),
				this.getResourceLastModificationTimestamp(requestEntity,
						colsVerInfo));
	}
}
