package org.bsworks.x2.app;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.FilterSpecBuilder;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.OrderSpecBuilder;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.PropertiesFetchSpecBuilder;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.SortDirection;
import org.bsworks.x2.responses.NotModifiedResponse;
import org.bsworks.x2.responses.OKResponse;
import org.bsworks.x2.services.persistence.PersistentResourceFetchResult;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.util.StringUtils;


/**
 * Default implementation of a persistent resource endpoint call handler for
 * HTTP GET requests. The handler supports two types of calls: a get single
 * record call, identified by presence of the last URI parameter, which is the
 * requested record id, and records collection search call, identified by the
 * absence of the record id in the request URI.
 *
 * <p>The call is configured using optional request parameters. The following
 * parameters can be used:
 *
 * <dl>
 * <dt>{@value #INCLUDE_PROPS_FETCH_PARAM}</dt><dd>Comma-separated list of
 * persistent resource property paths to include in the result (see
 * {@link PropertiesFetchSpec}). If specified, only these properties are
 * fetched. If the list is prefixed with a plus sign ("+"), all properties that
 * are normally fetched by default are fetched plus whatever properties
 * mentioned in the list (makes sense to use to add properties that are not
 * fetched by default to the result). In this mode, the parameter may be
 * combined with {@value #EXCLUDE_PROPS_FETCH_PARAM} parameter.</dd>
 * <dt>{@value #EXCLUDE_PROPS_FETCH_PARAM}</dt><dd>Comma-separated list of
 * persistent resource property paths to exclude from the result (see
 * {@link PropertiesFetchSpec}). It is illegal to combine
 * {@value #EXCLUDE_PROPS_FETCH_PARAM} with {@value #INCLUDE_PROPS_FETCH_PARAM}
 * unless the latter's value is prefixed with "+" (see above).</dd>
 * <dt>{@value #FILTER_PARAM_RE}</dt><dd>Specifies a filter condition (see
 * {@link FilterSpec}). A condition is a sequence of three parts: the persistent
 * resource property path(s), the conditional operation and a value. The
 * following conditional operations are supported:
 * <dl>
 * <dt>:</dt><dd>Equals.</dd>
 * <dt>{@literal <}</dt><dd>Less than or equal.</dd>
 * <dt>{@literal >}</dt><dd>Greater than or equal.</dd>
 * <dt>~</dt><dd>Contains substring that matches case-insensitive regular
 * expression.</dd>
 * <dt>*</dt><dd>Contains case-insensitive substring.</dd>
 * <dt>^</dt><dd>Starts with case-insensitive prefix.</dd>
 * <dt>|</dt><dd>Equals to any of the values from the provided pipe-separated
 * list.</dd>
 * </dl>
 * Each conditional operation can be prefixed with "!" to negate it.
 * <p>A condition may consist of a property path (or paths), optionally followed
 * by a "!", and no conditional operation with values. In that case, the
 * {@link FilterConditionType#NOT_EMPTY} or {@link FilterConditionType#EMPTY} is
 * used to test the condition.
 * <p>Multiple property paths may be specified separated with commas. In that
 * case, the filter conditions are created for each of the property paths and
 * the resulting list of conditions is combined using logical disjunction
 * (Boolean "OR").
 * <p>Multiple parameters matching {@value #FILTER_PARAM_RE} may be specified
 * for the same request to define a filter with multiple conditions. Conditions
 * with the same parameter name are combined into groups. Conditions within a
 * group are combined using logical conjunction or disjunction based on the
 * value of the {@value #FILTER_COMB_PARAM} parameter. Then, if multiple groups
 * are present, the groups are combined using the logical operator complementary
 * to the {@value #FILTER_COMB_PARAM}.</dd>
 * <dt>{@value #FILTER_COMB_PARAM}</dt><dd>Logical operator used to combine
 * conditions in a single filter conditions group. May be "and", which is the
 * default, or "or" for a disjunction.</dd>
 * <dt>{@value #ORDER_PARAM}</dt><dd>Comma-separated list of persistent resource
 * property paths used to order the result (see {@link OrderSpec}). Each
 * property path in the list can be suffixed with ":asc" or ":desc" to specify
 * the order direction. If not specified, ":asc" is assumed. In between the
 * property name and the optional ":asc" or ":desc" a value transformation
 * function can be specified, including ":len" for the string value length,
 * ":sub:<i>from</i>[:<i>len</i>]" for substring (from is zero-based), and
 * ":lpad:<i>width</i>[:<i>char</i>]" for left padding.</dd>
 * <dt>{@value #SPLIT_PARAM}</dt><dd>Defines result list segmentation (see
 * {@link OrderSpec#addSegment}). The syntax of the parameter value is the same
 * as for the filter condition parameter. Multiple {@value #SPLIT_PARAM}
 * parameters can be specified for a request to create sub-segmentation. Splits
 * are always added before any order specifications provided by the
 * {@link #ORDER_PARAM} parameter.</dd>
 * <dt>{@value #RANGE_PARAM}</dt><dd>Collection fetch range specification (see
 * {@link RangeSpec}), which a pair of comma-separated integer numbers. The
 * first number is for the first record index, zero-based. The second number is
 * the maximum number of records to return.</dd>
 * <dt>{@value #REFS_FETCH_PARAM}</dt><dd>Comma-separated list of persistent
 * resource reference property paths to fetch along with the records (see
 * {@link RefsFetchSpec}).</dd>
 * </dl>
 *
 * <p>For a single record request, only {@value #INCLUDE_PROPS_FETCH_PARAM},
 * {@value #EXCLUDE_PROPS_FETCH_PARAM} and {@value #REFS_FETCH_PARAM} parameters
 * are used.
 *
 * <p>For a collection search call, the response is encapsulated in a
 * {@link PersistentResourceFetchResult} object. For a single record request,
 * the {@link PersistentResourceFetchResult} wrapper object is returned only if
 * the request has {@value #REFS_FETCH_PARAM} parameter. Otherwise, the fetched
 * record itself is returned.
 *
 * <p>Multiple persistent property paths can be included in a single condition
 * separated with pipes. That results in multiple conditions for each property
 * combined using logical disjunction (Boolean "OR").
 *
 * <p>The handler implementation supports conditional HTTP requests and
 * generates "ETag" and "Last-Modified" HTTP response headers based on
 * persistent resource record version and last modification timestamp
 * meta-properties and uses application's
 * {@link PersistentResourceVersioningService} to incorporate participating
 * persistent resource collections in the generated values.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultGetPersistentResourceEndpointCallHandler<R>
	extends AbstractPersistentResourceEndpointCallHandler<R, Void> {

	/**
	 * Name of request parameter used to specify properties to include in
	 * {@link PropertiesFetchSpec}.
	 */
	public static final String INCLUDE_PROPS_FETCH_PARAM = "i";

	/**
	 * Name of request parameter used to specify properties to exclude from
	 * {@link PropertiesFetchSpec}.
	 */
	public static final String EXCLUDE_PROPS_FETCH_PARAM = "x";

	/**
	 * Regular expression used to match names of request parameters used to
	 * specify {@link FilterSpec}.
	 */
	public static final String FILTER_PARAM_RE = "f\\d*";

	/**
	 * Name of request parameter used to specify the mode of combining filter
	 * conditions in a group.
	 */
	public static final String FILTER_COMB_PARAM = "fj";

	/**
	 * Name of request parameter used to specify {@link OrderSpec}.
	 */
	public static final String ORDER_PARAM = "o";

	/**
	 * Name of request parameter used to specify segments in the
	 * {@link OrderSpec}.
	 */
	public static final String SPLIT_PARAM = "s";

	/**
	 * Name of request parameter used to specify {@link RangeSpec}.
	 */
	public static final String RANGE_PARAM = "r";

	/**
	 * Name of request parameter used to specify {@link RefsFetchSpec}.
	 */
	public static final String REFS_FETCH_PARAM = "e";


	/**
	 * Pattern for filter condition parameter names.
	 */
	private static final Pattern FILTER_PARAM_PATTERN =
		Pattern.compile(FILTER_PARAM_RE);

	/**
	 * Pattern for filter condition parameters.
	 */
	private static final Pattern FILTER_COND_PATTERN = Pattern.compile(
			// 1. property path(s)
			"([a-z]\\w*(?:\\.[a-z]\\w*)*(?:/id|/key)?"
				+ "(?:,[a-z]\\w*(?:\\.[a-z]\\w*)*(?:/id|/key)?)*)"
			+ "(?:"
				+ "(!)?"     // 2. operation negation
				+ "(?:"
					+ ":(.+)"    // 3. equality
					+ "|<(.+)"   // 4. range high limit (equal or less)
					+ "|>(.+)"   // 5. range low limit (equal or greater)
					+ "|~(.+)"   // 6. pattern
					+ "|\\*(.+)" // 7. substring
					+ "|\\^(.+)" // 8. prefix
					+ "|\\|(.+)" // 9. alternatives
				+ ")?"
			+ ")", Pattern.CASE_INSENSITIVE);

	/**
	 * Pattern for order specification element.
	 */
	private static final Pattern ORDER_SPEC_PATTERN = Pattern.compile(
			"([a-z]\\w*(?:\\.[a-z]\\w*)*(?:/id)?)"  // 1. property path
			+ "(?:"
				+ "(:len)"                          // 2. length
				+ "|(:sub:(\\d+)(?::(\\d+))?)"      // 3. sub, 4. from, 5. len
				+ "|(:lpad:(\\d+)(?::([^\\s,:]))?)" // 6. lpad, 7. wdth, 8. char
			+ ")?"
			+ "(?::(asc|desc))?",                   // 9. sort direction
			Pattern.CASE_INSENSITIVE);


	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	public DefaultGetPersistentResourceEndpointCallHandler(
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
	 * Returns {@code true}.
	 */
	@Override
	public final boolean isReadOnly() {

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public EndpointCallResponse handleCall(final EndpointCallContext ctx,
			final Void requestEntity)
		throws EndpointCallErrorException {

		// get fetch configuration from the request parameters
		PropertiesFetchSpecBuilder<R> propsFetch = null;
		FilterSpecBuilder<R> filter = null;
		OrderSpecBuilder<R> order = null;
		RangeSpec range = null;
		for (final String paramName : ctx.getRequestParamNames()) {
			;//...
		}

		// get fetch configuration using deprecated method
		if (propsFetch == null)
			propsFetch = this.getDeprecatedPropertiesFetchSpec(ctx);
		if (filter == null)
			filter = this.getDeprecatedFilterSpec(ctx);
		if (order == null)
			order = this.getDeprecatedOrderSpec(ctx);
		if (range == null)
			range = this.getDeprecatedRangeSpec(ctx);

		// get requested record id
		final Object recId = this.getAddressedRecordId(ctx);

		// handle different types of calls
		if (recId == null)
			return this.handleSearchCall(ctx, propsFetch, filter, order, range);
		if ((propsFetch != null)
				&& !propsFetch.getFetchedRefProperties().isEmpty())
			return this.handleGetWithRefsCall(ctx, recId, propsFetch);
		return this.handleSimpleGetCall(ctx, recId, propsFetch);
	}

	/**
	 * Handle simple single record get call.
	 *
	 * @param ctx Call context.
	 * @param recId Record id.
	 * @param propsFetch Properties fetch specification, or {@code null}.
	 *
	 * @return The response.
	 *
	 * @throws EndpointCallErrorException If an error happens in the endpoint
	 * handler.
	 */
	protected EndpointCallResponse handleSimpleGetCall(
			final EndpointCallContext ctx, final Object recId,
			final PropertiesFetchSpecBuilder<R> propsFetch)
		throws EndpointCallErrorException {

		// get the record filter
		final FilterSpecBuilder<R> recFilter =
			this.endpointHandler.getRecordFilter(ctx, recId);

		// get referred dependent resource collections versions
		final Set<Class<?>> prsrcClasses = new HashSet<>();
		this.addReferredResources(ctx, propsFetch, prsrcClasses);
		final PersistentResourceVersionInfo colsVerInfo;
		if (!prsrcClasses.isEmpty())
			colsVerInfo = ctx
				.getRuntimeContext()
				.getPersistentResourceVersioningService()
				.getCollectionsVersionInfo(
						ctx.getPersistenceTransaction(),
						prsrcClasses);
		else
			colsVerInfo = null;

		// get record versioning meta-properties
		final R recVerInfo =
			this.getRecordVersioningMetaProperties(ctx, recFilter);

		// get resource ETag and last modification timestamp
		final String eTag =
			this.getResourceETag(ctx, recVerInfo, colsVerInfo);
		final Date lastModTS =
			this.getResourceLastModificationTimestamp(recVerInfo, colsVerInfo);

		// check if conditional request and is not modified
		if (!this.processConditionalRequest(ctx, eTag, lastModTS,
				(recVerInfo == null)))
			return new NotModifiedResponse(eTag, lastModTS);

		// resource not found if no record
		if (recVerInfo == null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_NOT_FOUND, null,
					"No resource record with this id.");

		// get the full record
		final R rec =
			this.endpointHandler.get(ctx, recId, recFilter, propsFetch, false);

		// return the record in the response
		return new OKResponse(rec, eTag, lastModTS);
	}

	/**
	 * Handle single record get with references fetch call.
	 *
	 * @param ctx Call context.
	 * @param recId Record id.
	 * @param propsFetch Properties fetch specification, or {@code null}.
	 *
	 * @return The response.
	 *
	 * @throws EndpointCallErrorException If an error happens in the endpoint
	 * handler.
	 */
	protected EndpointCallResponse handleGetWithRefsCall(
			final EndpointCallContext ctx, final Object recId,
			final PropertiesFetchSpecBuilder<R> propsFetch)
		throws EndpointCallErrorException {

		// get the record filter
		final FilterSpecBuilder<R> recFilter =
			this.endpointHandler.getRecordFilter(ctx, recId);

		// get referred dependent and fetched resource collections versions
		final Set<Class<?>> prsrcClasses = new HashSet<>();
		this.addReferredResources(ctx, propsFetch, prsrcClasses);
		final PersistentResourceVersionInfo colsVerInfo = ctx
				.getRuntimeContext()
				.getPersistentResourceVersioningService()
				.getCollectionsVersionInfo(
						ctx.getPersistenceTransaction(),
						prsrcClasses);

		// get record versioning meta-properties
		final R recVerInfo =
			this.getRecordVersioningMetaProperties(ctx, recFilter);

		// get resource ETag and last modification timestamp
		final String eTag =
			this.getResourceETag(ctx, recVerInfo, colsVerInfo);
		final Date lastModTS =
			this.getResourceLastModificationTimestamp(recVerInfo, colsVerInfo);

		// check if conditional request and is not modified
		if (!this.processConditionalRequest(ctx, eTag, lastModTS,
				(recVerInfo == null)))
			return new NotModifiedResponse(eTag, lastModTS);

		// resource not found if no record
		if (recVerInfo == null)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_NOT_FOUND, null,
					"No resource record with this id.");

		// get the record
		final PersistentResourceFetchResult<R> res =
			this.endpointHandler.search(ctx, propsFetch, recFilter, null,
					new RangeSpec(0, 1), false);

		// return the record in the response
		return new OKResponse(res, eTag, lastModTS);
	}

	/**
	 * Handle collection search call.
	 *
	 * @param ctx Call context.
	 * @param propsFetch Properties fetch specification, or {@code null}.
	 * @param filter Filter specification, or {@code null}.
	 * @param order Order specification, or {@code null}.
	 * @param range Range specification, or {@code null}.
	 *
	 * @return The response.
	 *
	 * @throws EndpointCallErrorException If an error happens in the endpoint
	 * handler.
	 */
	protected EndpointCallResponse handleSearchCall(
			final EndpointCallContext ctx,
			final PropertiesFetchSpecBuilder<R> propsFetch,
			final FilterSpecBuilder<R> filter, final OrderSpecBuilder<R> order,
			final RangeSpec range)
		throws EndpointCallErrorException {

		// get all involved resource collections versions
		final Set<Class<?>> prsrcClasses = new HashSet<>();
		prsrcClasses.add(this.prsrcClass);
		this.addReferredResources(ctx, propsFetch, prsrcClasses);
		if (filter != null)
			prsrcClasses.addAll(filter.getParticipatingPersistentResources());
		if (order != null)
			prsrcClasses.addAll(order.getParticipatingPersistentResources());
		final PersistentResourceVersionInfo colsVerInfo = ctx
				.getRuntimeContext()
				.getPersistentResourceVersioningService()
				.getCollectionsVersionInfo(
						ctx.getPersistenceTransaction(),
						prsrcClasses);

		// get resource ETag and last modification timestamp
		final String eTag =
			this.getResourceETag(ctx, null, colsVerInfo);
		final Date lastModTS =
			this.getResourceLastModificationTimestamp(null, colsVerInfo);

		// check if conditional request and is not modified
		if (!this.processConditionalRequest(ctx, eTag, lastModTS, false))
			return new NotModifiedResponse(eTag, lastModTS);

		// get matching records
		final PersistentResourceFetchResult<R> res =
			this.endpointHandler.search(ctx, propsFetch, filter, order, range,
					true);

		// return the records in the response
		return new OKResponse(res, eTag, lastModTS);
	}

	/**
	 * Add all dependent resource classes, fetched reference classes and their
	 * dependent resource classes, other persistent resources that own requested
	 * nested object properties and all resource classes used for calculation
	 * of requested aggregate properties to the specified set.
	 *
	 * @param ctx Call context.
	 * @param propsFetch Properties fetch specification, or {@code null} if
	 * none.
	 * @param prsrcClasses Set of persistent resource classes, to which to add
	 * the resource classes.
	 */
	private void addReferredResources(final EndpointCallContext ctx,
			final PropertiesFetchSpec<R> propsFetch,
			final Set<Class<?>> prsrcClasses) {

		// add requested dependent resources
		for (final DependentRefPropertyHandler ph :
				this.prsrcHandler.getDependentRefProperties()) {
			if (((propsFetch != null) && propsFetch.isIncluded(ph.getName()))
				|| ((propsFetch == null) && ph.isFetchedByDefault()))
				prsrcClasses.add(ph.getReferredResourceClass());
		}

		// add resources owning nested object properties
		for (final ObjectPropertyHandler ph :
				this.prsrcHandler.getObjectProperties()) {
			if (((propsFetch != null) && propsFetch.isIncluded(ph.getName()))
				|| ((propsFetch == null) && ph.isFetchedByDefault())) {
				if (ph.isBorrowed())
					prsrcClasses.add(ph.getOwningPersistentResourceClass());
			}
		}

		// add requested aggregate properties
		for (final AggregatePropertyHandler ph :
				this.prsrcHandler.getAggregateProperties()) {
			if ((propsFetch != null) && propsFetch.isIncluded(ph.getName()))
				prsrcClasses.addAll(ph.getUsedPersistentResourceClasses());
		}

		// done if no references are requested to be fetched
		if (propsFetch == null)
			return;

		// add fetched references
		final Resources resources = ctx.getRuntimeContext().getResources();
		for (final Map.Entry<String, Class<?>> entry :
				propsFetch.getFetchedRefProperties().entrySet()) {
			final String refPropPath = entry.getKey();

			// skip if explicitly excluded by the properties fetch specification
			if (!propsFetch.isIncluded(refPropPath))
				continue;

			// get referred resource class
			final Class<?> refTargetClass = entry.getValue();

			// skip if already processed
			if (!prsrcClasses.add(refTargetClass))
				continue;

			// add requested dependent resources of the referred resource
			final PersistentResourceHandler<?> refPrsrcHandler =
				resources.getPersistentResourceHandler(refTargetClass);
			for (final DependentRefPropertyHandler ph :
					refPrsrcHandler.getDependentRefProperties()) {
				if (propsFetch.isIncluded(refPropPath + "." + ph.getName()))
					prsrcClasses.add(ph.getReferredResourceClass());
			}

			// add requested aggregate properties of the referred resource
			for (final AggregatePropertyHandler ph :
					refPrsrcHandler.getAggregateProperties()) {
				if (propsFetch.isIncluded(refPropPath + "." + ph.getName()))
					prsrcClasses.addAll(ph.getUsedPersistentResourceClasses());
			}
		}
	}

	/**
	 * Get properties fetch specification from the request parameters using
	 * deprecated method.
	 *
	 * @param ctx Call context.
	 *
	 * @return Properties fetch specification, or {@code null} if none.
	 *
	 * @throws EndpointCallErrorException If request parameters are invalid.
	 */
	private PropertiesFetchSpecBuilder<R> getDeprecatedPropertiesFetchSpec(
			final EndpointCallContext ctx)
		throws EndpointCallErrorException {

		final String includePropsFetchParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(INCLUDE_PROPS_FETCH_PARAM));
		final String excludePropsFetchParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(EXCLUDE_PROPS_FETCH_PARAM));
		final String refsFetchParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(REFS_FETCH_PARAM));

		if ((includePropsFetchParam == null)
				&& (excludePropsFetchParam == null)
				&& (refsFetchParam == null))
			return null;

		final boolean includeWithPlus = ((includePropsFetchParam != null)
				&& includePropsFetchParam.startsWith("+"));

		if (includeWithPlus
				&& (includePropsFetchParam != null) // redundant, for Eclipse
				&& (includePropsFetchParam.length() <= 1))
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid \"" + INCLUDE_PROPS_FETCH_PARAM
					+ "\" request parameter.");

		if ((includePropsFetchParam != null)
				&& (excludePropsFetchParam != null) && !includeWithPlus)
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Conflicting \"" + INCLUDE_PROPS_FETCH_PARAM + "\" and  \""
					+ EXCLUDE_PROPS_FETCH_PARAM + "\" request parameters.");

		final PropertiesFetchSpecBuilder<R> propsFetch =
			ctx.getPropertiesFetchSpec(this.prsrcClass);
		try {
			if ((includePropsFetchParam != null) && !includeWithPlus) {
				for (final String propPath : includePropsFetchParam.split(","))
					propsFetch.include(propPath);
			} else {
				propsFetch.includeByDefault();
				if (includePropsFetchParam != null)
					for (final String propPath :
						includePropsFetchParam.substring(1).split(","))
					propsFetch.include(propPath);
				if (excludePropsFetchParam != null)
					for (final String propPath :
							excludePropsFetchParam.split(","))
						propsFetch.exclude(propPath);
			}
		} catch (final InvalidSpecificationException e) {
			if (this.log.isDebugEnabled())
				this.log.debug("invalid parameter", e);
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid properties fetch specification parameter.");
		}

		for (final String propPath : refsFetchParam.split(",")) {
			try {
				propsFetch.fetch(propPath);
			} catch (final InvalidSpecificationException e) {
				if (this.log.isDebugEnabled())
					this.log.debug("invalid parameter", e);
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_BAD_REQUEST, null,
						"Invalid references fetch specification parameter.");
			}
		}

		return propsFetch;
	}

	/**
	 * Get filter specification from the request parameters using deprecated
	 * method.
	 *
	 * @param ctx Call context.
	 *
	 * @return Filter specification, or {@code null} if none.
	 *
	 * @throws EndpointCallErrorException If request parameters are invalid.
	 */
	private FilterSpecBuilder<R> getDeprecatedFilterSpec(
			final EndpointCallContext ctx)
		throws EndpointCallErrorException {

		final Collection<String> filterParamNames =
			ctx.getRequestParamNames(FILTER_PARAM_PATTERN);
		if (filterParamNames.isEmpty())
			return null;

		final boolean useDisjunction =
			"or".equalsIgnoreCase(ctx.getRequestParam(FILTER_COMB_PARAM));


		final FilterSpecBuilder<R> filter = ctx.getFilterSpec(this.prsrcClass);

		final Matcher m = FILTER_COND_PATTERN.matcher("");

		if (filterParamNames.size() == 1) {
			this.addFilterConditions(ctx,
					(useDisjunction ? filter.addDisjunction() : filter),
					filterParamNames.iterator().next(), m);
		} else { // multiple groups
			final FilterSpecBuilder<R> j =
				(useDisjunction ? filter : filter.addDisjunction());
			for (final String filterParamName : filterParamNames) {
				this.addFilterConditions(ctx,
						(useDisjunction ?
								j.addDisjunction() : j.addConjunction()),
						filterParamName, m);
			}
		}

		return filter;
	}

	/**
	 * Add filter conditions to the specified filter from the request parameters
	 * for a single conditions group.
	 *
	 * @param ctx Call context.
	 * @param filter Filter specification, to which to add the conditions.
	 * @param filterParamName Request parameter name for the group.
	 * @param m Matcher for filter condition pattern.
	 *
	 * @throws EndpointCallErrorException If request parameters are invalid.
	 */
	private void addFilterConditions(final EndpointCallContext ctx,
			final FilterSpecBuilder<R> filter, final String filterParamName,
			final Matcher m)
		throws EndpointCallErrorException {

		for (final String condExpr : ctx.getRequestParamValues(filterParamName))
			this.addFilterCondition(filter, filterParamName, condExpr, m);
	}

	/**
	 * Add filter condition to the specified filter.
	 *
	 * @param filter The filter specification, to which to add the condition.
	 * @param paramName Name of the request parameter that provides the
	 * condition.
	 * @param condExpr Condition expression (the parameter value).
	 * @param m Matcher for filter condition pattern.
	 *
	 * @throws EndpointCallErrorException If condition expression is invalid.
	 */
	private void addFilterCondition(final FilterSpecBuilder<R> filter,
			final String paramName, final String condExpr, final Matcher m)
		throws EndpointCallErrorException {

		if (!m.reset(condExpr).matches())
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid condition expression in parameter \"" + paramName
						+ "\".");

		final FilterConditionType condType;
		final List<String> operandsList;
		int g = 2;
		if (m.group(++g) != null) {
			condType = FilterConditionType.EQ;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.LE;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.GE;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.MATCH;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.SUBSTRING;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.PREFIX;
			operandsList = Collections.singletonList(m.group(g));
		} else if (m.group(++g) != null) {
			condType = FilterConditionType.EQ;
			operandsList = Arrays.asList(m.group(g).split("\\|"));
		} else {
			condType = FilterConditionType.NOT_EMPTY;
			operandsList = Collections.emptyList();
		}

		final Object[] operands =
				operandsList.toArray(new Object[operandsList.size()]);
		final boolean negated = (m.group(2) != null);
		try {
			final String[] propPaths = m.group(1).split(",");
			if (propPaths.length > 1) {
				final FilterSpecBuilder<R> junc = filter.addDisjunction();
				for (final String propPath : propPaths)
					junc.addCondition(propPath, condType, negated,
							operands);
			} else {
				filter.addCondition(propPaths[0], condType, negated,
						operands);
			}
		} catch (final InvalidSpecificationException e) {
			if (this.log.isDebugEnabled())
				this.log.debug("invalid parameter", e);
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid condition expression in parameter \"" + paramName
						+ "\": " + e.getMessage());
		}
	}

	/**
	 * Get order specification from the request parameters using deprecated
	 * method.
	 *
	 * @param ctx Call context.
	 *
	 * @return Order specification, or {@code null} if none.
	 *
	 * @throws EndpointCallErrorException If request parameters are invalid.
	 */
	private OrderSpecBuilder<R> getDeprecatedOrderSpec(
			final EndpointCallContext ctx)
		throws EndpointCallErrorException {

		OrderSpecBuilder<R> order = null;

		final String[] segmentParams = ctx.getRequestParamValues(SPLIT_PARAM);
		if ((segmentParams != null) && (segmentParams.length > 0)) {

			order = ctx.getOrderSpec(this.prsrcClass);

			final Matcher m = FILTER_COND_PATTERN.matcher("");

			for (final String condExpr : segmentParams) {
				final FilterSpecBuilder<R> split =
					ctx.getFilterSpec(this.prsrcClass);
				this.addFilterCondition(split, SPLIT_PARAM, condExpr, m);
				order.addSegment(split);
			}
		}

		final String orderParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(ORDER_PARAM));
		if (orderParam != null) {

			if (order == null)
				order = ctx.getOrderSpec(this.prsrcClass);

			final Matcher m = ORDER_SPEC_PATTERN.matcher("");

			for (final String orderElParam : orderParam.split(",")) {

				if (!m.reset(orderElParam).matches())
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST, null,
							"Invalid order specification parameter.");

				final String propPath = m.group(1);

				final PropertyValueFunction func;
				final Object[] funcParams;
				if (m.group(2) != null) {
					func = PropertyValueFunction.LENGTH;
					funcParams = null;
				} else if (m.group(3) != null) {
					func = PropertyValueFunction.SUBSTRING;
					final int from = Integer.parseInt(m.group(4));
					final int len =
						(m.group(5) != null ? Integer.parseInt(m.group(5)) : 0);
					funcParams = new Object[] {
							Integer.valueOf(from),
							Integer.valueOf(len)
					};
				} else if (m.group(6) != null) {
					func = PropertyValueFunction.LPAD;
					final int width = Integer.parseInt(m.group(7));
					if (width > 255)
						throw new EndpointCallErrorException(
								HttpServletResponse.SC_BAD_REQUEST, null,
								"Invalid order specification parameter:"
										+ " padding is too large.");
					final String paddingCharStr = m.group(8);
					final char paddingChar =
						(paddingCharStr != null ?
								paddingCharStr.charAt(0) : ' ');
					funcParams = new Object[] {
							Integer.valueOf(width),
							Character.valueOf(paddingChar)
					};
				} else {
					func = PropertyValueFunction.PLAIN;
					funcParams = null;
				}

				final SortDirection dir =
					("desc".equalsIgnoreCase(m.group(9)) ? SortDirection.DESC :
						SortDirection.ASC);

				try {
					order.add(dir, propPath, func, funcParams);
				} catch (final InvalidSpecificationException e) {
					if (this.log.isDebugEnabled())
						this.log.debug("invalid parameter", e);
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST, null,
							"Invalid order specification parameter: "
									+ e.getMessage());
				}
			}
		}

		return order;
	}

	/**
	 * Get range specification from the request parameters using deprecated
	 * method.
	 *
	 * @param ctx Call context.
	 *
	 * @return Range specification, or {@code null} if none.
	 *
	 * @throws EndpointCallErrorException If request parameters are invalid.
	 */
	private RangeSpec getDeprecatedRangeSpec(final EndpointCallContext ctx)
		throws EndpointCallErrorException {

		final String rangeParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(RANGE_PARAM));
		if (rangeParam == null)
			return null;

		final RangeSpec range;
		final int commaInd = rangeParam.indexOf(',');
		try {
			range = new RangeSpec(
					Integer.parseInt(rangeParam.substring(0, commaInd)),
					Integer.parseInt(rangeParam.substring(commaInd + 1)));
		} catch (final InvalidSpecificationException | NumberFormatException
				| IndexOutOfBoundsException e) {
			if (this.log.isDebugEnabled())
				this.log.debug("invalid parameter", e);
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid range specification parameter.");
		}

		return range;
	}
}
