package org.bsworks.x2.app;

import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.responses.NoContentResponse;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;


/**
 * Default implementation of a persistent resource endpoint call handler for
 * HTTP DELETE requests. The handler assumes that the last URI parameter is a
 * record id. It attempts then to delete the identified persistent resource
 * record. If the identified record does not exist, the handler sends HTTP error
 * 404 (Not Found) back to the client. If the record id is not in the URL, the
 * handler sends HTTP error 405 (Method Not Allowed) back to the client. If
 * persistent resource record was successfully deleted, HTTP 204 (No Content)
 * response is sent back.
 *
 * <p>The handler implementation supports conditional HTTP requests and may
 * returns an HTTP error 412 (Precondition Failed) response.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultDeletePersistentResourceEndpointCallHandler<R>
	extends AbstractPersistentResourceEndpointCallHandler<R, Void> {

	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	public DefaultDeletePersistentResourceEndpointCallHandler(
			final PersistentResourceEndpointHandler<R> endpointHandler) {
		super(endpointHandler);
	}


	/**
	 * Returns {@code null}.
	 */
	@Override
	public final Class<Void> getRequestEntityClass() {

		return null;
	}

	/**
	 * Returns {@code null}.
	 */
	@Override
	public final Class<?>[] getRequestEntityValidationGroups() {

		return null;
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
			final Void requestEntity)
		throws EndpointCallErrorException {

		// get requested record id
		final Object recId = this.getAddressedRecordId(ctx);
		if (recId == null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED, null,
					"No record id in the URL.");

		// get the record filter
		final FilterSpec<R> recFilter =
			this.endpointHandler.getRecordFilter(ctx, recId);

		// check if conditional request
		if (this.isConditionalRequest(ctx)) {

			// get referred dependent resource collections versions
			final PersistentResourceVersionInfo colsVerInfo =
				this.getDependentResourcesVersioningInfo(ctx, null);

			// get record versioning meta-properties
			final R recVerInfo =
				this.getRecordVersioningMetaProperties(ctx, recFilter);

			// check conditional request conditions
			this.processConditionalRequest(ctx,
					this.getResourceETag(ctx, recVerInfo, colsVerInfo),
					this.getResourceLastModificationTimestamp(recVerInfo,
							colsVerInfo),
					(recVerInfo == null));

			// resource not found if no record
			if (recVerInfo == null)
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_NOT_FOUND, null,
						"No resource record with this id.");
		}

		// delete the record
		final Set<Class<?>> affectedResources =
			this.endpointHandler.delete(ctx, recId, recFilter);

		// check that the record existed
		if (affectedResources.isEmpty())
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_NOT_FOUND, null,
					"No resource record with this id.");

		// update the affected persistent resource collection versions
		ctx.registerPersistentResourceModification(affectedResources);

		// done
		return new NoContentResponse(null, null);
	}
}
