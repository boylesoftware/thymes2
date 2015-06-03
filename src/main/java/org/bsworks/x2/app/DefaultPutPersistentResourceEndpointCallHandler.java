package org.bsworks.x2.app;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.IdHandling;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.responses.CreatedResponse;
import org.bsworks.x2.responses.NoContentResponse;
import org.bsworks.x2.responses.OKResponse;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;


/**
 * Default implementation of a persistent resource endpoint call handler for
 * HTTP PUT requests. The handler assumes that the last URI parameter is a
 * record id and the request body contains the new record data. It attempts then
 * to update the identified persistent resource record with the provided data.
 * If the identified record does not exist, the handler sends HTTP error 404
 * (Not Found) back to the client. If the record id is not in the URL, the
 * handler sends HTTP error 405 (Method Not Allowed) back to the client.
 *
 * <p>If persistent resource record was successfully updated, HTTP 200 (OK)
 * response is sent back with the updated record as the response entity. If the
 * updated record is not needed by the caller, to save on the network traffic
 * a {@value #NOCONTENT_PARAM} request parameter can be added, in which case
 * HTTP 204 (No Content) response is sent back.
 *
 * <p>The handler also verifies that the record id in the submitted data matches
 * the record id specified in the URL. If it does not match, an HTTP error 400
 * (Bad Request) is sent back in the response. If the persistent resource has
 * record version meta-property, the handler verifies that the version number in
 * the submitted data matches the version number of the existing record. If they
 * don't match, an HTTP error 409 (Conflict) is sent back to the client.
 *
 * <p>The handler implementation supports conditional HTTP requests and may
 * returns an HTTP error 412 (Precondition Failed) response.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultPutPersistentResourceEndpointCallHandler<R>
	extends AbstractPersistentResourceEndpointCallHandler<R, R> {

	/**
	 * Name of request parameter, which, if "true", makes the handler send back
	 * HTTP 204 (No Content) response upon successful record update instead of
	 * default HTTP 200 (OK) response.
	 */
	public static final String NOCONTENT_PARAM = "nocontent";


	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	public DefaultPutPersistentResourceEndpointCallHandler(
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
	 * {@link EndpointCallHandler#UPDATE_VAIDATION_GROUPS}.
	 */
	@Override
	public Class<?>[] getRequestEntityValidationGroups() {

		return UPDATE_VAIDATION_GROUPS;
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

		// get requested record id
		final Object recId = this.getAddressedRecordId(ctx);
		if (recId == null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_METHOD_NOT_ALLOWED, null,
					"No record id in the URL.");

		// verify the record id in the submitted record data
		if (!recId.equals(this.idPropHandler.getValue(requestEntity)))
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Record id in the submitted data does not match record id"
							+ " in the URL.");

		// get referred dependent resource collections versions
		final PersistentResourceVersionInfo colsVerInfo =
			this.getDependentResourcesVersioningInfo(ctx);

		// load and lock existing record
		final R rec = this.endpointHandler.get(ctx, recId,
				this.endpointHandler.getRecordFilter(ctx, recId), null, true);

		// check conditional request conditions
		this.processConditionalRequest(ctx,
				this.getResourceETag(ctx, rec, colsVerInfo),
				this.getResourceLastModificationTimestamp(rec, colsVerInfo),
				(rec == null));

		// check if need to create new record
		if (rec == null) {

			// make sure not an auto-generated id
			if (this.idPropHandler.getHandling() == IdHandling.AUTO_GENERATED)
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_NOT_FOUND, null,
						"No resource record with this id.");

			// make sure the actor is allowed to persist the resource
			if (!this.prsrcHandler.isAllowed(ResourcePropertyAccess.PERSIST,
						ctx.getActor()))
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_FORBIDDEN, null,
						"Not allowed to create new records.");

			// persist the record
			this.endpointHandler.create(ctx, requestEntity);

			// register persistent resource collection modification
			ctx.registerPersistentResourceModification(this.prsrcClass);

			// return the record in the response
			return new CreatedResponse(requestEntity,
					null,
					this.getResourceETag(ctx, requestEntity, colsVerInfo),
					this.getResourceLastModificationTimestamp(requestEntity,
							colsVerInfo));
		}

		// verify the record version
		if (this.versionPropHandler != null) {
			if (!this.versionPropHandler.getValue(rec).equals(
					this.versionPropHandler.getValue(requestEntity)))
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_CONFLICT, null,
						"Record version in the submitted data does not match"
								+ " version of the existing record.",
						new ExistingPersistentResourceRecord(rec));
		}

		// update the record
		final Set<String> updatedProps =
			this.endpointHandler.update(ctx, rec, requestEntity);

		// update the affected persistent resource collection versions
		if (!updatedProps.isEmpty()) {
			final Set<Class<?>> prsrcClasses = new HashSet<>();
			int numUpdatedDepRefs = 0;
			for (final DependentRefPropertyHandler ph :
					this.prsrcHandler.getDependentRefProperties()) {
				if (updatedProps.contains(ph.getName())) {
					numUpdatedDepRefs++;
					prsrcClasses.add(ph.getReferredResourceClass());
				}
			}
			if (numUpdatedDepRefs < updatedProps.size())
				prsrcClasses.add(this.prsrcClass);
			ctx.registerPersistentResourceModification(prsrcClasses);
		}

		// re-get dependent resource collections versioning info
		final PersistentResourceVersionInfo newColsVerInfo =
				this.getDependentResourcesVersioningInfo(ctx);

		// see if no content response is requested
		if (Boolean.parseBoolean(ctx.getRequestParam(NOCONTENT_PARAM)))
			return new NoContentResponse(
					this.getResourceETag(ctx, rec, newColsVerInfo),
					this.getResourceLastModificationTimestamp(rec,
							newColsVerInfo));

		// return response with the updated record
		return new OKResponse(rec,
				this.getResourceETag(ctx, rec, newColsVerInfo),
				this.getResourceLastModificationTimestamp(rec, newColsVerInfo));
	}
}
