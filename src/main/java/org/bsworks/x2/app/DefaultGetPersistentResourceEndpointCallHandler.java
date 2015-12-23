package org.bsworks.x2.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallResponse;
import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpecBuilder;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.ObjectPropertyHandler;
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
 * HTTP GET requests.
 *
 * <p>The handler implementation supports conditional HTTP requests and
 * generates "ETag" and "Last-Modified" HTTP response headers based on
 * persistent resource record version and last modification timestamp
 * meta-properties and uses application's
 * {@link PersistentResourceVersioningService} to incorporate participating
 * persistent resource collections in the generated values.
 *
 * <p>Depending on whether a resource record id is present as the last URI
 * parameter, the handler performs one of the two distinct operations: get the
 * specified by id resource record, or perform the resource records collection
 * search and get matching records. In the first case, the response is the
 * record (unless referred resource records fetch is requested, see below), or
 * an HTTP 404 (Not Found) error response if no record with the specified id
 * exists. In the collection search call case, the response is a special object
 * represented by an instance of {@link PersistentResourceFetchResult}, that
 * wraps the collection of the matched records (which may be empty if none
 * matched) along with any requested referred resource records and the total
 * number of the matched records for a ranged request (see below).
 *
 * <h3>Search Filter Specification</h3>
 *
 * <p>The filter for a collection search is specified using one or more HTTP
 * request parameters. Each parameter represents a condition in the filter
 * logical expression. Like a logical expression that may contain
 * sub-expressions in parenthesis, filter conditions can be combined into
 * groups. The groups and the conditions within them can form a logical
 * conjunction ("AND") or a logical disjunction ("OR"). A group is identified by
 * a prefix and its position in the expression hierarchy is encoded in the
 * prefix format that uses dot-separated notation to express nesting level of
 * the group.
 *
 * <p>The prefix for the root condition group of the main filter is
 * {@value #MAIN_FILTER_KEY}. All conditions associated with this prefix belong
 * to the top level of the filter logic expression. If another expression needs
 * to be an element in the top level expression, its conditions must have prefix
 * that is the parent prefix plus an arbitrary word appended to it after a dot
 * (alphanumeric plus underscore). Nested expressions can be created by adding
 * more elements to the prefix all separated by a dot.
 *
 * <p>The group prefix is the first part of every filter condition HTTP request
 * parameter name and is separated from the rest of the condition definition
 * with a dollar sign. By default, conditions in a give group are combined using
 * logical "AND". To make it logical "OR", an HTTP request parameter consisting
 * only of the group prefix plus the dollar sign and value "or" can be included.
 *
 * <p>So, for example, if we have an expression:
 *
 * <p>{@code A & B & (C | D) & (E & (F | G))}
 *
 * <p>the group prefixes in the HTTP request query string could be:
 *
 * <p>{@code f$A&f$B&f.a$=or&f.a$C&f.a$D&f.b$E&f.b.0$=or&f.b.0$F&f.b.0$G}
 *
 * <p>Each HTTP request parameter representing a condition is defined as
 * follows:
 *
 * <p>{@literal <group prefix>$<property path>[,<property path>...][:<value function>][:<condition type>][!]=[<test value>]}
 *
 * <p>The group prefix has been already discussed above. Following the dollar
 * sign after the group prefix is a comma-separated list of persistent resource
 * paths (using dot notation). The values of the specified properties are tested
 * by the condition. If multiple properties are specified, the results of
 * testing each property are combined using logical "OR".
 *
 * <p>Optionally, after a colon, the property value transformation function and
 * its parameters can be specified. The following functions are supported:
 *
 * <dl>
 * <dt>len<dd>String value length.
 * <dt>lc<dd>Lower-case string value.
 * <dt>{@literal sub:<from>[:<len>]}<dd>String value's substring starting
 * from the specified character (zero-based index) with the specified maximum
 * length. If length is not specified, the rest of the string is used.
 * <dt>{@literal lpad:<width>[:<char>]}<dd>String value padded on the left
 * to the specified minimum length using the specified padding character. If
 * character is not specified, space is used.
 * </dl>
 *
 * <p>The condition type, that follows the property path(s) and the optional
 * value transformation function in the condition HTTP parameter name, specifies
 * what aspect of the property value is tested. It can be one of the following:
 *
 * <dl>
 * <dt>min<dd>The parameter value is the minimum allowed value
 * (see {@link FilterConditionType#GE}).
 * <dt>max<dd>The parameter value is the maximum allowed value
 * (see {@link FilterConditionType#LE}).
 * <dt>pat<dd>The parameter value is a regular expression for a substring in the
 * property value. The matching performed is case-insensitive
 * (see {@link FilterConditionType#MATCH}).
 * <dt>sub<dd>The parameter value is a case-insensitive substring in the
 * property value (see {@link FilterConditionType#SUBSTRING}).
 * <dt>pre<dd>The parameter value is a case-insensitive prefix of the property
 * value (see {@link FilterConditionType#PREFIX}).
 * <dt>alt<dd>The parameter value is a pipe-separated list of allowed property
 * values (see {@link FilterConditionType#EQ}).
 * </dl>
 *
 * <p>If no condition type is specified, the parameter value is the matching
 * value for the property.
 *
 * <p>To inverse the condition, a exclamation mark is added at the end of the
 * HTTP parameter name.
 *
 * <p>When no condition type is specified (the equality condition) and the HTTP
 * parameter has no value (empty string value), the condition test if the
 * property is not empty  (see {@link FilterConditionType#NOT_EMPTY}).
 *
 * <h3>Properties Fetch Specification</h3>
 *
 * <p>The collection search call may contain a {@link #PROPS_FETCH_PARAM} HTTP
 * request parameter that specifies what properties of the persistent resource
 * records are included in the result and what properties are excluded. It is
 * also used to specify what referred resource records should be included in the
 * result. If the resource contains properties that are references to records of
 * another resource, the referred resource records fetch function allows
 * receiving the referred record in the same response in a transactional way.
 *
 * <p>The HTTP request parameter value is a comma-separated list of elements.
 * Each element can be:
 *
 * <ul>
 * <li>Property path to include the property.
 * <li>Property path prefixed with a dash (minus sign) to exclude the property.
 * <li>Reference property path ending in ".*" to include the property and fetch
 * the referred resource record.
 * <li>Reference property path ending in ".*" and prefixed with a dash to
 * exclude all properties of the referred resource record by default unless
 * explicitly included.
 * <li>An asterisk ("*") to include all properties by default (properties that
 * are fetched by default according to the resource definition).
 * <li>An aggregate property path followed by a slash and a filter prefix to
 * include the aggregate property and use the specified filter in its value
 * calculation.
 * </ul>
 *
 * <p>When a nested property is included using a dot notation property path, all
 * the parent properties are automatically included as well. If any of the
 * parent properties are references, the referred resource records are also
 * automatically fetched and included.
 *
 * <p>Unless an "*" is included in the properties list, if the HTTP request
 * parameter is present, all properties are initially excluded. Absence of the
 * HTTP request parameter is equivalent to having it with a single value of "*".
 *
 * <p>An aggregate property can be included with a filter. The filter is defined
 * in the request the same way as the main filter, but instead of the
 * {@value #MAIN_FILTER_KEY}, the specified after the slash filter key is used
 * to identify the filter's root group.
 *
 * <p>Besides the resource collection search calls, the
 * {@value #PROPS_FETCH_PARAM} HTTP request parameter can also be added to a
 * single record request as well. In that case, it is used to specify what
 * properties to include in the request. Note, that if the specification implies
 * fetching any referred resource records, the response is no longer the simple
 * resource record, but a {@link PersistentResourceFetchResult}, the same way as
 * if it were a collection search call, with the single record in the records
 * array.
 *
 * <h3>Result Order Specification</h3>
 *
 * <p>An {@value #ORDER_PARAM} HTTP request parameter is used to specify
 * ordering of the records in the resource collection search request result. The
 * value of the parameter is a comma-separated list of elements, each of which
 * can be:
 *
 * <ul>
 * <li>A property path to sort by the property value. The path can be followed
 * by a colon and a value transformation function, the same way as in the filter
 * condition definition described above.
 * <li>A dollar sign an a filter key to split the result around the logical
 * expression specified by the filter (result set segmentation). The filter is
 * defined the same way as the main filter with the specified filter key instead
 * of the {@value #MAIN_FILTER_KEY}. In ascending order, the records that <em>do
 * not</em> match the filter go first followed by the records that do match it.
 * </ul>
 *
 * <p>Each element can be optionally followed ":asc" or ":desc" to specify the
 * ascending or descending order. If nothing is given, ":asc" is assumed.
 *
 * <h3>Range Specification</h3>
 *
 * <p>The resource collection search result can be paginated by specifying a
 * range. The range is specified using an {@value #RANGE_PARAM} HTTP request
 * parameter. The value is two numbers separated with a comma: the first number
 * is the first matched record to return (starting with zero) and the second
 * number is the maximum number of records to return (the page size).
 *
 * <p>If range is specified, the result include the total number of matched
 * records. If range is not specified, the total number is not included (-1 in
 * the result) because it is equal to the number of the returned records.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultGetPersistentResourceEndpointCallHandler<R>
	extends AbstractPersistentResourceEndpointCallHandler<R, Void> {

	/**
	 * Property path regular expression.
	 */
	private static final String PROP_PATH_RE = "[a-z]\\w*(?:\\.[a-z]\\w*)*";

	/**
	 * Additional filter key regular expression.
	 */
	private static final String FILTER_KEY_RE = "[a-z][a-z_0-9]*";

	/**
	 * Property value function transformation specification regular expression.
	 */
	private static final String VALUE_FUNC_RE =
			"(:len)"                             // +0. length
			+ "|(:lc)"                           // +1. lower case
			+ "|(:sub:(\\d+)(?::(\\d+))?)"       // +2. substring, from, length
			+ "|(:lpad:(\\d+)(?::([^\\s,:]))?)"; // +5. left pad, width, char

	/**
	 * Number of groups in {@link #VALUE_FUNC_RE}.
	 */
	private static final int VALUE_FUNC_RE_NUM_GROUPS = 8;

	/**
	 * Name of request parameter used to specify what resource properties to
	 * include/exclude in the fetch.
	 */
	public static final String PROPS_FETCH_PARAM = "p";

	/**
	 * Pattern used to parse properties fetch specification parameter.
	 */
	private static final Pattern PROPS_FETCH_PARAM_PATTERN = Pattern.compile(
			"(?:^|\\G(?<!^),)"
			+ "(\\*|-?"                             // 1. full element
				+ "(" + PROP_PATH_RE + ")"          // 2. property path
				+ "(?:"
					+ "\\.\\*"
					+ "|/(" + FILTER_KEY_RE + ")"   // 3. filter key
				+ ")?"
			+ ")");

	/**
	 * Name of request parameter used to specify the search request result
	 * sorting.
	 */
	public static final String ORDER_PARAM = "o";

	/**
	 * Pattern used to parse order specification parameter.
	 */
	private static final Pattern ORDER_PARAM_PATTERN = Pattern.compile(
			"(?:^|\\G(?<!^),)"
			+ "(?:"
				+ "\\$(" + FILTER_KEY_RE + ")"      // 1. segment filter key
				+ "|(" + PROP_PATH_RE + "(?:/id)?)" // 2. property path
					+ "(?:" + VALUE_FUNC_RE + ")?"  // 3-10. value function
			+ ")"
			+ "(?::(asc|desc))?");                  // 11. sort direction

	/**
	 * Main filter key.
	 */
	public static final String MAIN_FILTER_KEY = "f";

	/**
	 * Pattern used to parse filter specification element parameter name.
	 */
	private static final Pattern FILTER_PARAM_NAME_PATTERN = Pattern.compile(
			"(" + FILTER_KEY_RE + "(?:\\.[a-z_0-9]+)*)\\$" // 1. group key
			+ "(?:"
				+ "("                                      // 2. property paths
					+ PROP_PATH_RE + "(?:/id|/key)?"
					+ "(?:," + PROP_PATH_RE + "(?:/id|/key)?)*"
				+ ")"
				+ "(?: " + VALUE_FUNC_RE + ")?"            // 3-10. value func
				+ "(?::("                                  // 11. test:
					+ "min"                                // minimum
					+ "|max"                               // maximum
					+ "|pat"                               // RE pattern match
					+ "|sub"                               // contains substring
					+ "|pre"                               // starts with
					+ "|alt"                               // one of
				+ "))?"
				+ "(!)?"                                   // 12. negation
			+ ")?");

	/**
	 * Name of request parameter used to specify the search request result
	 * range.
	 */
	public static final String RANGE_PARAM = "r";

	/**
	 * Name of request parameter used for the deprecated way of specifying
	 * property inclusion rules.
	 */
	@Deprecated
	private static final String INCLUDE_PROPS_FETCH_PARAM = "i";

	/**
	 * Name of request parameter used for the deprecated way of specifying
	 * property exclusion rules.
	 */
	@Deprecated
	private static final String EXCLUDE_PROPS_FETCH_PARAM = "x";

	/**
	 * Regular expression used to match names of request parameters used the
	 * deprecated way of specifying the filter.
	 */
	@Deprecated
	private static final String FILTER_PARAM_RE = "f\\d*";

	/**
	 * Pattern for deprecated filter condition parameter names.
	 */
	@Deprecated
	private static final Pattern FILTER_PARAM_PATTERN =
		Pattern.compile(FILTER_PARAM_RE);

	/**
	 * Name of request parameter used to specify the mode of combining filter
	 * conditions in a group in the deprecated way of specifying the filter.
	 */
	@Deprecated
	private static final String FILTER_COMB_PARAM = "fj";

	/**
	 * Pattern for deprecated filter condition parameters.
	 */
	@Deprecated
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
	 * Name of request parameter used for the deprecated way of specifying
	 * ordering segments.
	 */
	@Deprecated
	private static final String SPLIT_PARAM = "s";

	/**
	 * Name of request parameter used for the deprecated way of specifying
	 * fetched referred resources.
	 */
	@Deprecated
	private static final String REFS_FETCH_PARAM = "e";


	/**
	 * Enable deprecated protocol.
	 */
	private final boolean ENABLE_DEPRECATED;


	/**
	 * Create new handler.
	 *
	 * @param endpointHandler Persistent resource endpoint handler.
	 */
	public DefaultGetPersistentResourceEndpointCallHandler(
			final PersistentResourceEndpointHandler<R> endpointHandler) {
		super(endpointHandler);

		this.ENABLE_DEPRECATED = false;
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

		// create collector for additional filters
		final Map<String, FilterSpecBuilder<R>> addlFilters = new HashMap<>();

		// get properties fetch specification
		final String propsFetchParam =
			StringUtils.nullIfEmpty(ctx.getRequestParam(PROPS_FETCH_PARAM));
		final PropertiesFetchSpecBuilder<R> propsFetch;
		if (propsFetchParam != null) {
			propsFetch = ctx.getPropertiesFetchSpec(this.prsrcClass);
			try {
				this.parsePropertiesFetchParam(ctx, propsFetch, propsFetchParam,
						addlFilters);
			} catch (final InvalidSpecificationException e) {
				if (this.log.isDebugEnabled())
					this.log.debug("invalid properties fetch specification"
							+ " request parameter", e);
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_BAD_REQUEST, null,
						"Invalid \"" + PROPS_FETCH_PARAM
						+ "\" request parameter.");
			}
		} else if (this.ENABLE_DEPRECATED) {
			propsFetch = this.getDeprecatedPropertiesFetchSpec(ctx);
			if (propsFetch != null)
				this.log.warn(
						"deprecated properties fetch specification is used");
		} else {
			propsFetch = null;
		}

		// get requested record id
		final Object recId = this.getAddressedRecordId(ctx);

		// record request?
		if (recId != null) {

			// parse any additional filters
			this.parseAdditionalFilters(ctx, addlFilters);

			// referred resource records requested?
			if ((propsFetch != null)
					&& !propsFetch.getFetchedRefProperties().isEmpty())
				return this.handleGetWithRefsCall(ctx, recId, propsFetch);

			// simple record request
			return this.handleSimpleGetCall(ctx, recId, propsFetch);
		}

		// search request:

		// get order specification
		final String orderParam =
			StringUtils.nullIfEmpty(ctx.getRequestParam(ORDER_PARAM));
		final String[] deprecatedSplitParams = (this.ENABLE_DEPRECATED ?
				ctx.getRequestParamValues(SPLIT_PARAM) : null);
		final OrderSpecBuilder<R> order;
		if ((orderParam != null)
				|| ((deprecatedSplitParams != null)
						&& (deprecatedSplitParams.length > 0))) {
			order = ctx.getOrderSpec(this.prsrcClass);
			if ((deprecatedSplitParams != null)
					&& (deprecatedSplitParams.length > 0)) {
				this.parseDeprecatedSplitParam(ctx, order,
						deprecatedSplitParams);
				this.log.warn("deprecated result set split parameter is used");
			}
			if (orderParam != null) {
				try {
					this.parseOrderParam(ctx, order, orderParam, addlFilters);
				} catch (final InvalidSpecificationException e) {
					if (this.log.isDebugEnabled())
						this.log.debug("invalid order specification request"
								+ " parameter", e);
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST, null,
							"Invalid \"" + ORDER_PARAM
							+ "\" request parameter.");
				}
			}
		} else {
			order = null;
		}

		// parse any additional filters
		this.parseAdditionalFilters(ctx, addlFilters);

		// get main filter
		FilterSpecBuilder<R> filter = ctx.getFilterSpec(this.prsrcClass);
		try {
			this.parseFilterParams(ctx, filter, MAIN_FILTER_KEY);
			if (filter.isEmpty())
				filter = null;
		} catch (final InvalidSpecificationException e) {
			if (this.log.isDebugEnabled())
				this.log.debug("invalid main filter specification request"
						+ " parameters", e);
			throw new EndpointCallErrorException(
					HttpServletResponse.SC_BAD_REQUEST, null,
					"Invalid filter specification request parameters.");
		}

		// see if there is a deprecated filter specification
		if (this.ENABLE_DEPRECATED && (filter == null)) {
			filter = this.getDeprecatedFilterSpec(ctx);
			if (filter != null)
				this.log.warn("deprecated filter specification is used");
		}

		// get range specification
		final String rangeParam = StringUtils.nullIfEmpty(
				ctx.getRequestParam(RANGE_PARAM));
		final RangeSpec range;
		if (rangeParam != null) {
			final int commaInd = rangeParam.indexOf(',');
			try {
				range = new RangeSpec(
						Integer.parseInt(rangeParam.substring(0, commaInd)),
						Integer.parseInt(rangeParam.substring(commaInd + 1)));
			} catch (final InvalidSpecificationException | NumberFormatException
					| IndexOutOfBoundsException e) {
				if (this.log.isDebugEnabled())
					this.log.debug("invalid range specification parameter", e);
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_BAD_REQUEST, null,
						"Invalid \"" + RANGE_PARAM + "\" request parameter.");
			}
		} else {
			range = null;
		}

		// handle the search request
		return this.handleSearchCall(ctx, propsFetch, filter, order, range);
	}

	/**
	 * Parse all additional filters in the request.
	 *
	 * @param ctx Call context.
	 * @param addlFilters Map with additional filter builders by filter keys.
	 *
	 * @throws EndpointCallErrorException If any of the additional filter
	 * specification parameters are invalid.
	 */
	private void parseAdditionalFilters(
			final EndpointCallContext ctx,
			final Map<String, FilterSpecBuilder<R>> addlFilters)
		throws EndpointCallErrorException {

		for (final Map.Entry<String, FilterSpecBuilder<R>> entry
				: addlFilters.entrySet()) {
			try {
				this.parseFilterParams(ctx, entry.getValue(), entry.getKey());
			} catch (final InvalidSpecificationException e) {
				if (this.log.isDebugEnabled())
					this.log.debug("invalid additional filter \""
							+ entry.getKey() + "\" specification request"
							+ " parameters", e);
				throw new EndpointCallErrorException(
						HttpServletResponse.SC_BAD_REQUEST, null,
						"Invalid additional filter specification request"
						+ " parameters.");
			}
		}
	}

	/**
	 * Parse properties fetch parameter value.
	 *
	 * @param ctx Call context.
	 * @param propsFetch Properties fetch specification builder.
	 * @param paramValue The parameter value.
	 * @param addlFilters Map, to which to add aggregate property filters if
	 * any.
	 */
	private void parsePropertiesFetchParam(
			final EndpointCallContext ctx,
			final PropertiesFetchSpecBuilder<R> propsFetch,
			final String paramValue,
			final Map<String, FilterSpecBuilder<R>> addlFilters) {

		// extract elements
		int lastMatchEnd = -1;
		for (final Matcher m = PROPS_FETCH_PARAM_PATTERN.matcher(paramValue);
				m.find(); lastMatchEnd = m.end()) {
			final String fullMatch = m.group(1);
			final String propPath = m.group(2);
			final String filterKey = m.group(3);
			if (fullMatch.equals("*")) {
				propsFetch.includeByDefault();
			} else if (fullMatch.startsWith("-")) {
				if (filterKey != null)
					throw new InvalidSpecificationException(
							"Exclusion rule for filtered aggregate property.");
				if (fullMatch.endsWith(".*"))
					propsFetch.excludeProperties(propPath);
				else
					propsFetch.exclude(propPath);
			} else {
				if (filterKey != null) {
					final String aggCollectionPath;
					try {
						aggCollectionPath =
							((AggregatePropertyHandler) this.prsrcHandler
								.getPersistentPropertyChain(propPath).getLast())
								.getAggregatedCollectionPropertyPath();
					} catch (final ClassCastException e) {
						throw new InvalidSpecificationException(
								"Filtered property \"" + propPath
								+ "\" is not an aggregate property.", e);
					}
					FilterSpecBuilder<R> filter = addlFilters.get(filterKey);
					if (filter == null)
						addlFilters.put(filterKey,
								filter = ctx
									.getFilterSpec(this.prsrcClass)
									.setBasePath(aggCollectionPath));
					else if (!filter.getBasePath().equals(aggCollectionPath))
						throw new InvalidSpecificationException("Filter \""
								+ filterKey + "\" is shared by aggregate"
								+ " properties that aggregate different"
								+ " collections.");
					propsFetch.includeFilteredAggregate(propPath, filter);
				} else if (fullMatch.endsWith(".*")) {
					propsFetch.fetch(propPath);
				} else {
					propsFetch.include(propPath);
				}
			}
		}
		if (lastMatchEnd != paramValue.length())
			throw new InvalidSpecificationException(
					"Specification parsing error.");
	}

	/**
	 * Parse order parameter value.
	 *
	 * @param ctx Call context.
	 * @param order Order specification builder.
	 * @param paramValue The parameter value.
	 * @param addlFilters Map, to which to add order segmentation filters if
	 * any.
	 */
	private void parseOrderParam(
			final EndpointCallContext ctx,
			final OrderSpecBuilder<R> order,
			final String paramValue,
			final Map<String, FilterSpecBuilder<R>> addlFilters) {

		// extract elements
		int lastMatchEnd = -1;
		for (final Matcher m = ORDER_PARAM_PATTERN.matcher(paramValue);
				m.find(); lastMatchEnd = m.end()) {

			// get order direction
			final SortDirection dir = ("desc".equals(
						m.group(3 + VALUE_FUNC_RE_NUM_GROUPS)) ?
					SortDirection.DESC : SortDirection.ASC);

			// test if segmentation element
			final String splitFilterKey = m.group(1);
			if (splitFilterKey != null) {

				// get segment split
				FilterSpecBuilder<R> split = addlFilters.get(splitFilterKey);
				if (split == null)
					addlFilters.put(splitFilterKey,
							split = ctx.getFilterSpec(this.prsrcClass));
				else if (!split.getBasePath().isEmpty())
					throw new InvalidSpecificationException(
							"Result segmentation uses aggregate property"
							+ " filter.");

				// add segment
				order.addSegment(dir, split);

			} else { // property element

				// get property path
				final String propPath = m.group(2);

				// parse value transformation function if any
				final List<Object> funcParams = new ArrayList<>();
				final PropertyValueFunction func =
					this.parseValueFunction(m, 3, funcParams);

				// add order element
				order.add(dir, propPath, func,
						(funcParams.isEmpty() ? null :
							funcParams.toArray(new Object[funcParams.size()])));
			}
		}
		if (lastMatchEnd != paramValue.length())
			throw new InvalidSpecificationException(
					"Specification parsing error.");
	}

	/**
	 * Parse parameters for a particular filter key and build the filter.
	 *
	 * @param ctx Call context.
	 * @param filter Filter builder.
	 * @param filterKey Filter key.
	 */
	private void parseFilterParams(
			final EndpointCallContext ctx,
			final FilterSpecBuilder<R> filter,
			final String filterKey) {

		// get relevant parameters
		final SortedMap<String, String[]> params =
			ctx.getRequestParamsTree().subMap(filterKey + "$", filterKey + "/");

		// find and process filter specification elements
		final Matcher m = FILTER_PARAM_NAME_PATTERN.matcher("");
		FilterSpecBuilder<R> curGroup = filter;
		String curGroupKey = filterKey;
		final List<String> curGroupKeyParts = new ArrayList<>();
		this.splitFilterGroupKey(curGroupKey, curGroupKeyParts);
		final List<String> groupKeyParts = new ArrayList<>();
		for (final Map.Entry<String, String[]> entry : params.entrySet()) {

			// test if parameter name matches filter definition element pattern
			if (!m.reset(entry.getKey()).matches())
				continue;

			// get conditions group key
			final String groupKey = m.group(1);

			// if new group key, find/create the conditions group
			if (!groupKey.equals(curGroupKey)) {
				this.splitFilterGroupKey(groupKey, groupKeyParts);
				final int curLen = curGroupKeyParts.size();
				final int newLen = groupKeyParts.size();
				final int minLen = (curLen < newLen ? curLen : newLen);
				int i = 0;
				while ((i < minLen)
						&& curGroupKeyParts.get(i).equals(groupKeyParts.get(i)))
					i++;
				for (int j = curLen - 1; j >= i; j--) {
					curGroupKeyParts.remove(j);
					curGroup = curGroup.getParent();
				}
				for (int j = i; j < newLen; j++) {
					curGroupKeyParts.add(groupKeyParts.get(j));
					curGroup = curGroup.addConjunction();
				}
				curGroupKey = groupKey;
			}

			// get parameter value
			final String[] entryValue = entry.getValue();
			final String value = (
					(entryValue != null) && (entryValue.length > 0) ?
							StringUtils.nullIfEmpty(entryValue[0]) : null);

			// get property paths from the parameter name
			final String propPathsString = m.group(2);

			// check if group options parameter
			if (propPathsString == null) {
				if ("or".equals(value))
					curGroup.makeDisjunction();
				continue;
			}

			// build the condition:

			// parse value transformation function if any
			final List<Object> funcParamsList = new ArrayList<>();
			final PropertyValueFunction func =
				this.parseValueFunction(m, 3, funcParamsList);
			final Object[] funcParams =
				funcParamsList.toArray(new Object[funcParamsList.size()]);

			// condition type and operands
			final FilterConditionType type;
			final Object[] operands;
			switch (StringUtils.defaultIfEmpty(m.group(11), "")) {
			case "min":
				if (value == null)
					throw new InvalidSpecificationException(
							"Minimum condition requires value.");
				type = FilterConditionType.GE;
				operands = new Object[] { value };
				break;
			case "max":
				if (value == null)
					throw new InvalidSpecificationException(
							"Maximum condition requires value.");
				type = FilterConditionType.LE;
				operands = new Object[] { value };
				break;
			case "pat":
				if (value == null)
					throw new InvalidSpecificationException(
							"Pattern condition requires value.");
				type = FilterConditionType.MATCH;
				operands = new Object[] { value };
				break;
			case "sub":
				if (value == null)
					throw new InvalidSpecificationException(
							"Substring condition requires value.");
				type = FilterConditionType.SUBSTRING;
				operands = new Object[] { value };
				break;
			case "pre":
				if (value == null)
					throw new InvalidSpecificationException(
							"Prefix condition requires value.");
				type = FilterConditionType.PREFIX;
				operands = new Object[] { value };
				break;
			case "alt":
				if (value == null)
					throw new InvalidSpecificationException(
							"Alternatives condition requires value.");
				type = FilterConditionType.EQ;
				operands = value.split("\\|");
				break;
			default:
				if (value != null) {
					type = FilterConditionType.EQ;
					operands = new Object[] { value };
				} else {
					type = FilterConditionType.EMPTY;
					operands = new Object[] {};
				}
			}

			// negated condition?
			final boolean negated = (m.group(12) != null);

			// add condition to the current group for each property
			final String[] propPaths = propPathsString.split(",");
			if (propPaths.length > 1) {
				final FilterSpecBuilder<R> g = curGroup.addDisjunction();
				for (final String propPath : propPaths)
					g.addCondition(propPath,
							func, funcParams, type, negated, operands);
			} else {
				curGroup.addCondition(propPaths[0],
						func, funcParams, type, negated, operands);
			}
		}
	}

	/**
	 * Split specified filter group key into its parts.
	 *
	 * @param groupKey The group key.
	 * @param groupKeyParts List, to which to add the parts. The list is cleared
	 * by this method before adding the parts.
	 */
	private void splitFilterGroupKey(final String groupKey,
			final List<String> groupKeyParts) {

		groupKeyParts.clear();

		int dotInd, lastDotInd = -1;
		while ((dotInd = groupKey.indexOf('.', lastDotInd + 1)) > 0) {
			groupKeyParts.add(groupKey.substring(lastDotInd + 1, dotInd));
			lastDotInd = dotInd;
		}
		groupKeyParts.add(groupKey.substring(lastDotInd + 1));
	}

	/**
	 * Parse property value transformation function specification.
	 *
	 * @param m Matcher containing the specification in its matched groups.
	 * @param groupOffset First group offset.
	 * @param funcParams List, to which to add function parameters if any.
	 *
	 * @return The function.
	 */
	private PropertyValueFunction parseValueFunction(
			final Matcher m,
			final int groupOffset,
			final List<Object> funcParams) {

		if (m.group(groupOffset) != null) {
			return PropertyValueFunction.LENGTH;
		}

		if (m.group(groupOffset + 1) != null) {
			return PropertyValueFunction.LOWERCASE;
		}

		if (m.group(groupOffset + 2) != null) {
			funcParams.add(Integer.valueOf(m.group(groupOffset + 3)));
			final String lenParam = m.group(groupOffset + 4);
			funcParams.add(lenParam != null ?
					Integer.valueOf(lenParam) : Integer.valueOf(0));
			return PropertyValueFunction.SUBSTRING;
		}

		if (m.group(groupOffset + 5) != null) {
			final Integer width = Integer.valueOf(m.group(groupOffset + 6));
			if (width.intValue() > 255)
				throw new InvalidSpecificationException(
						"Padding width value is too large.");
			funcParams.add(width);
			final String paddingCharParam = m.group(groupOffset + 7);
			funcParams.add(paddingCharParam != null ?
					Character.valueOf(paddingCharParam.charAt(0)) :
						Character.valueOf(' '));
			return PropertyValueFunction.LPAD;
		}

		return PropertyValueFunction.PLAIN;
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
	@Deprecated
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

		if (refsFetchParam != null) {
			for (final String propPath : refsFetchParam.split(",")) {
				try {
					propsFetch.fetch(propPath);
				} catch (final InvalidSpecificationException e) {
					if (this.log.isDebugEnabled())
						this.log.debug("invalid parameter", e);
					throw new EndpointCallErrorException(
							HttpServletResponse.SC_BAD_REQUEST, null,
							"Invalid references fetch specification"
							+ " parameter.");
				}
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
					junc.addCondition(propPath, PropertyValueFunction.PLAIN,
							null, condType, negated, operands);
			} else {
				filter.addCondition(propPaths[0], PropertyValueFunction.PLAIN,
						null, condType, negated, operands);
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
	 * Parse deprecated order segmentation parameter.
	 *
	 * @param ctx Call context.
	 * @param order Order specification builder, to which to add parsed
	 * segments.
	 * @param paramValues Deprecated parameter values.
	 *
	 * @throws EndpointCallErrorException If request parameter values are
	 * invalid.
	 */
	@Deprecated
	private void parseDeprecatedSplitParam(
			final EndpointCallContext ctx,
			final OrderSpecBuilder<R> order,
			final String[] paramValues)
		throws EndpointCallErrorException {

		final Matcher m = FILTER_COND_PATTERN.matcher("");
		for (final String condExpr : paramValues) {
			final FilterSpecBuilder<R> split =
				ctx.getFilterSpec(this.prsrcClass);
			this.addFilterCondition(split, SPLIT_PARAM, condExpr, m);
			order.addSegment(SortDirection.ASC, split);
		}
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
}
