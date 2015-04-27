package org.bsworks.x2.app;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.resource.DependentResourcePropertyHandler;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.services.persistence.LockType;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;


/**
 * Abstract parent for persistent resource endpoint call handlers.
 *
 * @param <R> Handled persistent resource type.
 * @param <E> Request entity type.
 *
 * @author Lev Himmelfarb
 */
public abstract class AbstractPersistentResourceEndpointCallHandler<R, E>
	implements EndpointCallHandler<E> {

	/**
	 * Pattern for extracting values from ETags list.
	 */
	private static final Pattern ETAG_VAL_PATTERN =
		Pattern.compile("\"([^\"]+)\"");

	/**
	 * List of conditional HTTP request headers.
	 */
	private static final String[] CONDITIONAL_REQUEST_HEADERS = new String[] {
		"If-None-Match",
		"If-Match",
		"If-Modified-Since",
		"If-Unmodified-Since"
	};


	/**
	 * The log.
	 */
	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Persistent resource endpoint handler.
	 */
	protected final PersistentResourceEndpointHandler<R> endpointHandler;

	/**
	 * Persistent resource handler.
	 */
	protected final PersistentResourceHandler<R> prsrcHandler;

	/**
	 * Persistent resource class.
	 */
	protected final Class<R> prsrcClass;

	/**
	 * Persistent resource record id property handler.
	 */
	protected final IdPropertyHandler idPropHandler;

	/**
	 * Persistent resource record version property handler, or {@code null} if
	 * none.
	 */
	protected final MetaPropertyHandler versionPropHandler;

	/**
	 * Persistent resource record last modification timestamp property handler,
	 * or {@code null} if none.
	 */
	protected final MetaPropertyHandler lastModTSPropHandler;


	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	protected AbstractPersistentResourceEndpointCallHandler(
			final PersistentResourceEndpointHandler<R> endpointHandler) {

		this.endpointHandler = endpointHandler;
		this.prsrcHandler = this.endpointHandler.getPersistentResourceHandler();
		this.prsrcClass = this.prsrcHandler.getResourceClass();
		this.idPropHandler = this.prsrcHandler.getIdProperty();
		this.versionPropHandler = this.prsrcHandler.getMetaProperty(
				MetaPropertyType.VERSION);
		this.lastModTSPropHandler = this.prsrcHandler.getMetaProperty(
				MetaPropertyType.MODIFICATION_TIMESTAMP);
	}


	/**
	 * Default implementation simply calls
	 * {@link PersistentResourceHandler#isAllowed(org.bsworks.x2.resource.ResourcePropertyAccess, Actor)}.
	 */
	@Override
	public boolean isAllowed(final HttpMethod requestMethod,
			final String requestURI, final List<String> uriParams,
			final Actor actor) {

		return this.endpointHandler.isAllowed(requestMethod, requestURI,
				uriParams, actor);
	}


	/**
	 * Get record id from the request URI assuming that it is the last URI
	 * parameter.
	 *
	 * @param ctx Call context.
	 *
	 * @return Record id, or {@code null} if none.
	 *
	 * @throws EndpointCallErrorException If the URI is invalid.
	 */
	protected final Object getAddressedRecordId(final EndpointCallContext ctx)
		throws EndpointCallErrorException {

		final String recIdStr = ctx.getRequestURIParam(-1);
		if (recIdStr == null)
			return null;

		try {
			return this.idPropHandler.getValueHandler().valueOf(recIdStr);
		} catch (final InvalidResourceDataException e) {
			if (this.log.isDebugEnabled())
				this.log.debug("invalid parameter", e);
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid record id value.");
		}
	}

	/**
	 * Get resource record versioning meta-properties (record version number and
	 * last modification timestamp). As a side-effect, the method also locks the
	 * record in shared mode.
	 *
	 * @param ctx Call context.
	 * @param recFilter Record filter.
	 *
	 * @return Record with versioning meta-properties set, or {@code null} if
	 * does not exist.
	 */
	protected final R getRecordVersioningMetaProperties(
			final EndpointCallContext ctx, final FilterSpec<R> recFilter) {

		final PropertiesFetchSpec<R> propsFetch =
			ctx.getPropertiesFetchSpec(this.prsrcClass);
		if (this.versionPropHandler != null)
			propsFetch.include(this.versionPropHandler.getName());
		if (this.lastModTSPropHandler != null)
			propsFetch.include(this.lastModTSPropHandler.getName());

		return ctx
				.getPersistenceTransaction()
				.createPersistentResourceFetch(this.prsrcClass)
				.setFilter(recFilter)
				.setRange(new RangeSpec(0, 1), null)
				.setPropertiesFetch(propsFetch)
				.lockResult(LockType.SHARED) // prevent record modification
				.getFirstResult();
	}

	/**
	 * Get combined versioning information for dependent persistent resource
	 * collections referred by the handler's persistent resource.
	 *
	 * @param ctx Call context.
	 * @param propsFetch Properties fetch specification, or {@code null} if
	 * none.
	 *
	 * @return Versioning information, or {@code null} if no dependent resource
	 * reference properties included.
	 */
	protected final PersistentResourceVersionInfo
	getDependentResourcesVersioningInfo(final EndpointCallContext ctx,
			final PropertiesFetchSpec<R> propsFetch) {

		if (this.prsrcHandler.getDependentResourceProperties().isEmpty())
			return null;

		final Set<Class<?>> prsrcClasses = new HashSet<>();
		for (final DependentResourcePropertyHandler ph :
				this.prsrcHandler.getDependentResourceProperties()) {
			if (((propsFetch != null) && propsFetch.isIncluded(ph.getName()))
				|| ((propsFetch == null) && ph.isFetchedByDefault()))
				prsrcClasses.add(ph.getReferredResourceClass());
		}
		if (prsrcClasses.isEmpty())
			return null;

		return ctx
				.getRuntimeContext()
				.getPersistentResourceVersioningService()
				.getCollectionsVersionInfo(
						ctx.getPersistenceTransaction(),
						prsrcClasses);
	}

	/**
	 * Calculate resource current "ETag" value.
	 *
	 * @param ctx Call context.
	 * @param rec Existing record, or {@code null} if addressed record does not
	 * exist or resource is not a particular record.
	 * @param colsVerInfo Persistent resource collections versioning
	 * information, or {@code null} if resource does not involve any persistent
	 * resource collections as a whole.
	 *
	 * @return Current resource "ETag" value, or {@code null} if unavailable.
	 * Note, that the returned value is not quoted.
	 */
	protected final String getResourceETag(final EndpointCallContext ctx,
			final R rec, final PersistentResourceVersionInfo colsVerInfo) {

		if (((rec == null) || (this.versionPropHandler == null))
				&& (colsVerInfo == null))
			return null;

		final StringBuilder eTagBuf = new StringBuilder(64);
		eTagBuf.append(ctx.getRuntimeContext().getApplicationVersion());
		if (rec != null)
			eTagBuf.append("-").append(this.versionPropHandler.getValue(rec));
		if (colsVerInfo != null)
			eTagBuf.append("-").append(colsVerInfo.getVersion());

		return eTagBuf.toString();
	}

	/**
	 * Calculate resource last modification timestamp.
	 *
	 * @param rec Existing record, or {@code null} if addressed record does not
	 * exist or resource is not a particular record.
	 * @param colsVerInfo Persistent resource collections versioning
	 * information, or {@code null} if resource does not involve any persistent
	 * resource collections as a whole.
	 *
	 * @return Resource last modification timestamp, or {@code null} if
	 * unavailable.
	 */
	protected final Date getResourceLastModificationTimestamp(final R rec,
			final PersistentResourceVersionInfo colsVerInfo) {

		if (((rec == null) || (this.lastModTSPropHandler == null))
				&& (colsVerInfo == null))
			return null;

		final Date recLastModTS = (rec == null ? null :
			(Date) this.lastModTSPropHandler.getValue(rec));
		final Date colsLastModTS = (colsVerInfo == null ? null :
			colsVerInfo.getLastModificationTimestamp());

		if (recLastModTS == null)
			return colsLastModTS;

		if (colsLastModTS == null)
			return recLastModTS;

		if (recLastModTS.after(colsLastModTS))
			return recLastModTS;

		return colsLastModTS;
	}

	/**
	 * Tell if conditional HTTP request.
	 *
	 * @param ctx Call context.
	 *
	 * @return {@code true} if conditional request.
	 */
	protected final boolean isConditionalRequest(
			final EndpointCallContext ctx) {

		for (final String header : CONDITIONAL_REQUEST_HEADERS) {
			if (ctx.getRequestHeader(header) != null)
				return true;
		}

		return false;
	}

	/**
	 * Process conditional request.
	 *
	 * @param ctx Call context.
	 * @param eTag Resource current "ETag" value, or {@code null} if
	 * unavailable.
	 * @param lastModTS Resource last modification timestamp, or {@code null} if
	 * unavailable.
	 * @param requestedRecDoesNotExist {@code true} if particular record was
	 * addressed by the request, but the identified record does not exist.
	 *
	 * @return {@code true} to proceed with request processing, {@code false} if
	 * the resource is not modified.
	 *
	 * @throws EndpointCallErrorException If precondition failed.
	 */
	protected final boolean processConditionalRequest(
			final EndpointCallContext ctx, final String eTag,
			final Date lastModTS, final boolean requestedRecDoesNotExist)
		throws EndpointCallErrorException {

		// process "If-Match" and "If-Unmodified-Since" conditions
		final String ifMatchHeader = ctx.getRequestHeader("If-Match");
		final Date ifUnmodifiedSinceHeader =
			ctx.getRequestDateHeader("If-Unmodified-Since");
		if (ifMatchHeader != null) {

			if (!this.evaluateETagMatchCondition(ifMatchHeader,
					requestedRecDoesNotExist, eTag))
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_PRECONDITION_FAILED, null,
						"No matching resource at the specified URI.");

		} else if (ifUnmodifiedSinceHeader != null) {

			if (this.evaluateModifiedSinceCondition(ifUnmodifiedSinceHeader,
					lastModTS))
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_PRECONDITION_FAILED, null,
						"The resource has since been modified.");
		}

		// process "If-None-Match" and "If-Modified-Since" conditions
		final String ifNoneMatchHeader = ctx.getRequestHeader("If-None-Match");
		final Date ifModifiedSinceHeader =
			ctx.getRequestDateHeader("If-Modified-Since");
		if (ifNoneMatchHeader != null) {

			if (this.evaluateETagMatchCondition(ifNoneMatchHeader,
					requestedRecDoesNotExist, eTag)) {
				if (ctx.getRequestMethod() == HttpMethod.GET)
					return false; // unmodified
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_PRECONDITION_FAILED, null,
						"There is a matching resource at the specified URI.");
			}

		} else if ((ctx.getRequestMethod() == HttpMethod.GET)
				&& (ifModifiedSinceHeader != null)) {

			if (!this.evaluateModifiedSinceCondition(ifModifiedSinceHeader,
					lastModTS))
				return false; // unmodified
		}

		// proceed with request processing
		return true;
	}

	/**
	 * Evaluate ETag match condition.
	 *
	 * @param conditionHeader "If-Match" or "If-None-Match" header value.
	 * @param requestedRecDoesNotExist {@code true} if particular record was
	 * addressed by the request, but the identified record does not exist.
	 * @param eTag Addressed resource current "ETag" value, or {@code null} if
	 * unavailable.
	 *
	 * @return Result of condition evaluation.
	 */
	private boolean evaluateETagMatchCondition(final String conditionHeader,
			final boolean requestedRecDoesNotExist, final String eTag) {

		if (conditionHeader.equals("*"))
			return !requestedRecDoesNotExist;

		if (eTag == null)
			return false;

		final Matcher m = ETAG_VAL_PATTERN.matcher(conditionHeader);
		while (m.find())
			if (m.group(1).equals(eTag))
				return true;

		return false;
	}

	/**
	 * Evaluate last modification timestamp condition.
	 *
	 * @param sinceHeader "If-Modified-Since" or "If-Unmodified-Since" header
	 * value.
	 * @param lastModTS Resource last modification timestamp, or {@code null} if
	 * unavailable, in which case {@code true} if always returned, which reports
	 * the resource as modified.
	 *
	 * @return Result of condition evaluation.
	 */
	private boolean evaluateModifiedSinceCondition(final Date sinceHeader,
			final Date lastModTS) {

		if (lastModTS == null)
			return true;

		return ((lastModTS.getTime() - sinceHeader.getTime()) >= 1000);
	}
}
