package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterCondition;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.PropertiesFetchSpecBuilder;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertiesContainer;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyPersistence;
import org.bsworks.x2.resource.ResourcePropertyValueHandler;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.SimplePropertyHandler;
import org.bsworks.x2.resource.TypePropertyHandler;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.util.CollectionUtils;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Query builder.
 *
 * @author Lev Himmelfarb
 */
class QueryBuilder {

	/**
	 * Name of the record id column in the temporary anchor table.
	 */
	static final String ANCHOR_ID_COLNAME = "id";


	/**
	 * Query builder context.
	 */
	private static final class QueryBuilderContext {

		/**
		 * Parent context, or {@code null} for the seed.
		 */
		final QueryBuilderContext parentCtx;

		/**
		 * Top persistent resource handler being fetched.
		 */
		final PersistentResourceHandler<?> prsrcHandler;

		/**
		 * Application resources manager.
		 */
		final Resources resources;

		/**
		 * SQL dialect.
		 */
		final SQLDialect dialect;

		/**
		 * Query parameter value handlers factory.
		 */
		final ParameterValuesFactoryImpl paramsFactory;

		/**
		 * Data consumer.
		 */
		final Actor actor;

		/**
		 * Properties fetch specification.
		 */
		private final PropertiesFetchSpec<?> propsFetch;

		/**
		 * Filter specification.
		 */
		final FilterSpec<?> filter;

		/**
		 * Order specification.
		 */
		final OrderSpec<?> order;

		/**
		 * Parent property path (my be empty but not {@code null}).
		 */
		final String parentPropPath;

		/**
		 * Parent property chain (may be empty).
		 */
		final List<ResourcePropertyHandler> parentPropChain;

		/**
		 * Query root table name.
		 */
		final String rootTableName;

		/**
		 * Query root table alias.
		 */
		final String rootTableAlias;

		/**
		 * Parent anchor column expression.
		 */
		final String parentAnchorColExpr;

		/**
		 * Full parent join expression.
		 */
		final String parentJoinExpr;

		/**
		 * Parent join attachment expression.
		 */
		final String parentJoinAttachmentExpr;

		/**
		 * Attachment expressions of all branches merged to the context.
		 */
		final StringBuilder mergedJoinAttachmentExprs = new StringBuilder(128);

		/**
		 * Column labels prefix.
		 */
		final String colLabelPrefix;

		/**
		 * Record id column name. May be parent record id column name if the
		 * query root object does not have its own id. May be {@code null} for
		 * the seed context.
		 */
		final String rootIdColName;

		/**
		 * Record id column expression. In contrast to {@link #rootIdColName},
		 * {@code null} if the query root object does not have its own id.
		 */
		final String rootIdColExpr;

		/**
		 * Tells if outer joins must be forced.
		 */
		private final boolean forceOuter;

		/**
		 * Select list for single-valued properties.
		 */
		private final StringBuilder singlesSelectList = new StringBuilder(512);

		/**
		 * Select list for collection properties.
		 */
		private final StringBuilder collectionsSelectList =
			new StringBuilder(512);

		/**
		 * Property paths that are included in the select list and need a
		 * single-valued join.
		 */
		final Set<String> selectSingleJoins = new HashSet<>();

		/**
		 * Join expressions for all single-valued properties.
		 */
		final SortedMap<String, String> allSingleJoins = new TreeMap<>();

		/**
		 * Collection joins.
		 */
		final StringBuilder collectionJoins = new StringBuilder(128);

		/**
		 * Group by list.
		 */
		private String groupByList = "";

		/**
		 * Aggregation "WHERE" clause, if any.
		 */
		private WhereClause aggregationWhereClause = null;

		/**
		 * Order by list.
		 */
		private final StringBuilder orderByList = new StringBuilder(64);

		/**
		 * Single-valued property descriptors by property paths.
		 */
		final Map<String, SingleValuedQueryProperty> singlePropExprs =
			new HashMap<>();

		/**
		 * Collection property stumps by property paths.
		 */
		final Map<String, CollectionQueryProperty> collectionProps =
			new HashMap<>();

		/**
		 * Branches (before re-branching).
		 */
		final Collection<QueryBranch> branches = new ArrayList<>();

		/**
		 * Next column label prefix character to use.
		 */
		private char nextColLabelPrefixChar = 'a';

		/**
		 * Nested property column label prefix.
		 */
		String propColLabelPrefix = null;

		/**
		 * Nested property table alias.
		 */
		String propTableAlias;

		/**
		 * Nested property chain.
		 */
		final List<ResourcePropertyHandler> propChain;


		// polymorphic nested object support

		/**
		 * Polymorphic object value type column expression.
		 */
		String valueTypeColExpr;

		/**
		 * Polymorphic object value type test expressions.
		 */
		final List<String> valueTypeTests = new ArrayList<>();

		/**
		 * Joins needed for the polymorphic object value type tests.
		 */
		final StringBuilder valueTypeTestJoins = new StringBuilder();


		// aggregation support

		/**
		 * Context level in the aggregation branch.
		 */
		private int aggregationBranchLevel;

		/**
		 * Aggregated collection property path.
		 */
		private String aggregatedCollectionPropPath;

		/**
		 * Aggregation key property name, or {@code null} if single-valued
		 * aggregate property.
		 */
		private String aggregationKeyPropName;

		/**
		 * Aggregated property handlers.
		 */
		private List<AggregatePropertyHandler> aggregatedPropHandlers;

		/**
		 * Properties fetch specification.
		 */
		private PropertiesFetchSpecBuilder<?> aggregationPropsFetch;

		/**
		 * Aggregation value expressions by property names.
		 */
		private final Map<String, String> aggregationValueExprs;

		/**
		 * Aggregation filter, if any.
		 */
		private FilterSpec<? extends Object> aggregationFilter;


		/**
		 * Create "seed" context, which a context for building a new branched
		 * query system.
		 *
		 * @param resources Application resources manager.
		 * @param dialect SQL dialect.
		 * @param paramsFactory Query parameter value handlers factory.
		 * @param actor Data consumer.
		 * @param prsrcHandler Persistent resource handler.
		 * @param propsFetch Properties fetch specification, or {@code null}.
		 * @param filter Filter specification, or {@code null}.
		 * @param order Order specification, or {@code null}.
		 */
		QueryBuilderContext(final Resources resources, final SQLDialect dialect,
				final ParameterValuesFactoryImpl paramsFactory,
				final Actor actor,
				final PersistentResourceHandler<?> prsrcHandler,
				final PropertiesFetchSpec<?> propsFetch,
				final FilterSpec<?> filter, final OrderSpec<?> order) {

			this.parentCtx = null;
			this.prsrcHandler = prsrcHandler;
			this.resources = resources;
			this.dialect = dialect;
			this.paramsFactory = paramsFactory;
			this.actor = actor;
			this.propsFetch = propsFetch;
			this.filter = filter;
			this.order = order;
			this.parentPropPath = null;
			this.parentPropChain =
				Collections.<ResourcePropertyHandler>emptyList();
			this.rootTableName = null;
			this.rootTableAlias = null;
			this.parentAnchorColExpr = null;
			this.parentJoinExpr = null;
			this.parentJoinAttachmentExpr = null;
			this.colLabelPrefix = null;
			this.rootIdColName = null;
			this.rootIdColExpr = null;
			this.forceOuter = false;

			this.propChain = new ArrayList<>(1);

			this.aggregationBranchLevel = -1;
			this.aggregationValueExprs = new HashMap<>();
		}

		/**
		 * Create child context.
		 *
		 * @param parentCtx Parent context.
		 * @param parentPropPath Parent property path.
		 * @param rootTableName Query root table name.
		 * @param rootTableAlias Query root table alias.
		 * @param parentAnchorColExpr Parent anchor column expression.
		 * @param parentJoinExpr Full parent join expression.
		 * @param parentJoinAttacmentExpr Parent join attachment expression.
		 * @param colLabelPrefix Column labels prefix.
		 * @param rootIdColName Record id column name. May be parent record id
		 * column name if the query root object does not have its own id.
		 * @param rootIdColExpr Record id column expression. In contrast to
		 * {@link #rootIdColName}, {@code null} if the query root object does
		 * not have its own id.
		 * @param forceOuter {@code true} if outer joins must be forced.
		 */
		QueryBuilderContext(final QueryBuilderContext parentCtx,
				final String parentPropPath, final String rootTableName,
				final String rootTableAlias, final String parentAnchorColExpr,
				final String parentJoinExpr,
				final String parentJoinAttacmentExpr,
				final String colLabelPrefix, final String rootIdColName,
				final String rootIdColExpr, final boolean forceOuter) {

			this.parentCtx = parentCtx;
			this.prsrcHandler = parentCtx.prsrcHandler;
			this.resources = parentCtx.resources;
			this.dialect = parentCtx.dialect;
			this.paramsFactory = parentCtx.paramsFactory;
			this.actor = parentCtx.actor;
			this.propsFetch = parentCtx.propsFetch;
			this.filter = parentCtx.filter;
			this.order = parentCtx.order;
			this.parentPropPath = parentPropPath;
			this.parentPropChain = parentCtx.propChain;
			this.rootTableName = rootTableName;
			this.rootTableAlias = rootTableAlias;
			this.parentAnchorColExpr = parentAnchorColExpr;
			this.parentJoinExpr = parentJoinExpr;
			this.parentJoinAttachmentExpr = parentJoinAttacmentExpr;
			this.colLabelPrefix = colLabelPrefix;
			this.rootIdColName = rootIdColName;
			this.rootIdColExpr = rootIdColExpr;
			this.forceOuter = forceOuter;

			this.propChain =
				new ArrayList<>(this.parentPropChain.size() + 1);
			this.propChain.addAll(this.parentPropChain);

			this.aggregationBranchLevel =
				(parentCtx.aggregationBranchLevel < 0 ? -1 :
					parentCtx.aggregationBranchLevel + 1);
			this.aggregatedCollectionPropPath =
				parentCtx.aggregatedCollectionPropPath;
			this.aggregationKeyPropName = parentCtx.aggregationKeyPropName;
			this.aggregationPropsFetch = parentCtx.aggregationPropsFetch;
			this.aggregationValueExprs = parentCtx.aggregationValueExprs;
		}


		/**
		 * Tell if outer joins must be forced.
		 *
		 * @return {@code true} if only outer joins should be used.
		 */
		boolean isForceOuter() {

			if (this.aggregationBranchLevel >= 0)
				return true;

			return this.forceOuter;
		}

		/**
		 * Add single-valued property column expression to the select list.
		 *
		 * @param propName Property name.
		 * @param valExpr Value expression.
		 * @param pureValExpr Core value expression.
		 */
		void appendSelectList(final String propName, final String valExpr,
				final String pureValExpr) {

			if (this.aggregationBranchLevel > 0) {

				if (this.parentPropPath.equals(
						this.aggregatedCollectionPropPath)) {
					this.aggregationValueExprs.put(propName, valExpr);
					if (propName.equals(this.aggregationKeyPropName))
						this.aggregationValueExprs.put(propName + "/key",
								pureValExpr);
				} else if (this.parentPropPath.startsWith(
						this.aggregatedCollectionPropPath + ".")) {
					this.aggregationValueExprs.put(
							this.parentPropPath.substring(
								this.aggregatedCollectionPropPath.length() + 1)
							+ "." + propName, valExpr);
				}

				return;
			}

			this.appendSelectList(
					valExpr + " AS " + this.dialect.quoteColumnLabel(
							this.colLabelPrefix + propName),
					false);
		}

		/**
		 * Add column expression to the select list.
		 *
		 * @param colExpr Column expression to add.
		 * @param collection {@code true} to add to collection properties select
		 * list, {@code false} to add to single-valued properties select list.
		 */
		void appendSelectList(final String colExpr, final boolean collection) {

			if (this.aggregationBranchLevel > 0)
				return;

			final StringBuilder selectList =
				(collection ? this.collectionsSelectList :
					this.singlesSelectList);

			if (selectList.length() > 0)
				selectList.append(", ");
			selectList.append(colExpr);
		}

		/**
		 * Create group by list.
		 *
		 * @return Group by list.
		 */
		String createGroupByList() {

			final List<String> colExprs = new ArrayList<>();
			for (QueryBuilderContext c = this; c != null; c = c.parentCtx) {
				if (c.rootIdColExpr != null)
					colExprs.add(c.rootTableAlias + "." + c.rootIdColName);
			}
			final StringBuilder groupByList = new StringBuilder(64);
			for (int i = colExprs.size() - 1; i >= 0; i--) {
				if (groupByList.length() > 0)
					groupByList.append(", ");
				groupByList.append(colExprs.get(i));
			}

			if (this.aggregationKeyPropName != null) {
				if (groupByList.length() > 0)
					groupByList.append(", ");
				groupByList.append(this.aggregationValueExprs.get(
						this.aggregationKeyPropName + "/key"));
			}

			return groupByList.toString();
		}

		/**
		 * Create aggregation "WHERE" clause.
		 *
		 * @return Aggregation "WHERE" clause, or {@code null} if no aggregation
		 * filter.
		 */
		WhereClause createAggregationWhereClause() {

			if (this.aggregationFilter == null)
				return null;

			;System.out.println(">>> BUILDING AGGREGATION WHERE CLAUSE:"
					+ "\n - singlePropExprs: " + this.singlePropExprs
					+ "\n - collectionProps: " + this.collectionProps
					+ "\n - allSingleJoins: " + this.allSingleJoins
					+ "\n - aggregationValueExprs: " + this.aggregationValueExprs);
			/*final Map<String, JDBCParameterValue> params = new HashMap<>();
			final WhereClause res = new WhereClause(this.resources,
					this.dialect, this.paramsFactory,
					this.aggregationFilter, "g", this.singlePropExprs,
					this.collectionProps, this.allSingleJoins, params);
			;System.out.println(">>> BUILT WHERE CLAUSE: " + res);*/

			;return null;
		}

		/**
		 * Set the group by list.
		 *
		 * @param groupByList Group by list to set.
		 */
		void setGroupByList(final String groupByList) {

			this.groupByList = groupByList;
		}

		/**
		 * Set aggregation "WHERE" clause.
		 *
		 * @param aggregationWhereClause The clause to set.
		 */
		void setAggregationWhereClause(
				final WhereClause aggregationWhereClause) {

			this.aggregationWhereClause = aggregationWhereClause;
		}

		/**
		 * Add column expression to the order by list end.
		 *
		 * @param colExpr Column expression to add.
		 */
		void appendOrderByList(final String colExpr) {

			if (this.aggregationBranchLevel >= 0)
				return;

			if (this.orderByList.length() > 0)
				this.orderByList.append(", ");
			this.orderByList.append(colExpr);
		}

		/**
		 * Add column expression to the order by list front.
		 *
		 * @param colExpr Column expression to add.
		 */
		void prependOrderByList(final String colExpr) {

			if (this.aggregationBranchLevel >= 0)
				return;

			if (this.orderByList.length() > 0)
				this.orderByList.insert(0, colExpr + ", ");
			else
				this.orderByList.append(colExpr);
		}

		/**
		 * Build and return complete select list of the query.
		 *
		 * @return The select list, may be empty.
		 */
		String getSelectList() {

			final StringBuilder selectList = new StringBuilder(512);

			selectList.append(this.singlesSelectList);

			if (this.collectionsSelectList.length() > 0) {
				if (selectList.length() > 0)
					selectList.append(", ");
				selectList.append(this.collectionsSelectList);
			}

			return selectList.toString();
		}

		/**
		 * Get group by list of the query.
		 *
		 * @return The group by list, may be empty.
		 */
		String getGroupByList() {

			return this.groupByList;
		}

		/**
		 * Get aggregation "WHERE" clause for the query.
		 *
		 * @return The aggregation "WHERE" clause, or {@code null} if none.
		 */
		WhereClause getAggregationWhereClause() {

			return this.aggregationWhereClause;
		}

		/**
		 * Build and return order by list of the query.
		 *
		 * @return The order by list, may be empty.
		 */
		String getOrderByList() {

			return this.orderByList.toString();
		}

		/**
		 * Prepare context for a nested property.
		 *
		 * @param propHandler Nested property handler.
		 */
		void prepareNestedProperty(final ResourcePropertyHandler propHandler) {

			if (this.propColLabelPrefix == null)
				this.propChain.add(propHandler);
			else
				this.propChain.set(this.propChain.size() - 1, propHandler);

			this.propColLabelPrefix =
				this.colLabelPrefix + (this.nextColLabelPrefixChar++) + "$";

			this.propTableAlias =
				"z" + this.propColLabelPrefix.replace("$", "");
		}

		/**
		 * Get full property path relative to this context.
		 *
		 * @param propName Property name.
		 *
		 * @return Property path.
		 */
		String getPropertyPath(final String propName) {

			final boolean noParent = ((this.parentPropPath == null)
					|| this.parentPropPath.isEmpty());

			return (noParent ? "" : this.parentPropPath + ".") + propName;
		}

		/**
		 * Get properties fetch specification to use.
		 *
		 * @return Properties fetch specification.
		 */
		private PropertiesFetchSpec<?> getPropertiesFetchSpec() {

			return (this.aggregationBranchLevel < 0 ? this.propsFetch :
				this.aggregationPropsFetch);
		}

		/**
		 * Tell if the specified reference property is requested to be fetched.
		 *
		 * @param propPath Reference property path.
		 *
		 * @return {@code true} if requested to be fetched.
		 */
		boolean isFetchRequested(final String propPath) {

			final PropertiesFetchSpec<?> propsFetch =
				this.getPropertiesFetchSpec();

			return ((propsFetch != null)
					&& propsFetch.isFetchRequested(propPath));
		}

		/**
		 * Tell if the specified property needs to be selected.
		 *
		 * @param propPath Property path.
		 * @param propHandler Property handler.
		 *
		 * @return {@code true} if selected.
		 */
		boolean isSelected(final String propPath,
				final ResourcePropertyHandler propHandler) {

			final PropertiesFetchSpec<?> propsFetch =
				this.getPropertiesFetchSpec();

			return (((propsFetch != null)
						&& propsFetch.isIncluded(propPath))
					|| ((propsFetch == null)
							&& propHandler.isFetchedByDefault()));
		}

		/**
		 * Get aggregate property filter.
		 *
		 * @param propPath Aggregate property path.
		 *
		 * @return The filter, or {@code null} if none.
		 */
		FilterSpec<? extends Object> getAggregateFilter(final String propPath) {

			final PropertiesFetchSpec<?> propsFetch =
				this.getPropertiesFetchSpec();

			if (propsFetch == null)
				return null;

			return propsFetch.getAggregateFilter(propPath);
		}

		/**
		 * Enter aggregation mode.
		 *
		 * @param aggregatedCollectionPropPath Aggregated collection property
		 * path.
		 * @param aggregationKeyPropName Aggregation key property name, or
		 * {@code null} if single-valued aggregate property.
		 * @param aggregatedPropHandlers Aggregated property handlers.
		 * @param aggFilter Aggregated collection filter, or {@code null} if
		 * none.
		 */
		void setAggregationMode(
				final String aggregatedCollectionPropPath,
				final String aggregationKeyPropName,
				final List<AggregatePropertyHandler> aggregatedPropHandlers,
				final FilterSpec<? extends Object> aggFilter) {

			this.aggregationBranchLevel = 0;

			this.aggregatedPropHandlers = aggregatedPropHandlers;
			this.aggregationKeyPropName = aggregationKeyPropName;

			// create complete aggregated collection property path
			final String propPathsPrefix =
				(this.parentPropPath.isEmpty() ? "" :
					this.parentPropPath + ".");
			this.aggregatedCollectionPropPath =
				propPathsPrefix + aggregatedCollectionPropPath;

			// create aggregation properties and references fetches
			this.aggregationPropsFetch =
				this.resources.getPropertiesFetchSpec(
						this.prsrcHandler.getResourceClass());
			for (final AggregatePropertyHandler ph : aggregatedPropHandlers) {

				// add aggregated properties to the fetch
				for (final String propPath : ph.getAggregatedPropertyPaths())
					this.aggregationPropsFetch.include(
							this.aggregatedCollectionPropPath + "." + propPath);

				// add intermediate references to the fetch
				// INCLUDED AUTOMATICALLY
				/*final String refPath = ph.getLastIntermediateRefPath();
				if (refPath != null)
					this.aggregationPropsFetch.fetch(propPathsPrefix + refPath);*/
			}

			// save aggregation filter and include all used properties
			this.aggregationFilter = aggFilter;
			if (this.aggregationFilter != null)
				this.includeUsedFilterProps(
						this.aggregationFilter,
						this.aggregationPropsFetch,
						this.aggregatedCollectionPropPath + ".");
		}

		private void includeUsedFilterProps(
				final FilterSpec<? extends Object> filter,
				final PropertiesFetchSpecBuilder<?> propsFetch,
				final String propsPrefix) {

			for (FilterCondition c : filter.getConditions())
				propsFetch.include(propsPrefix + c.getPropertyPath());
			for (FilterSpec<? extends Object> junc : filter.getJunctions())
				this.includeUsedFilterProps(junc, propsFetch, propsPrefix);
		}

		/**
		 * Clear aggregation mode.
		 */
		void clearAggregationMode() {

			this.aggregationBranchLevel = -1;

			this.aggregatedCollectionPropPath = null;
			this.aggregationKeyPropName = null;
			this.aggregatedPropHandlers = null;
			this.aggregationPropsFetch = null;

			this.aggregationValueExprs.clear();

			this.aggregationFilter = null;
		}

		/**
		 * Tell if the context is a root context of the aggregation branch being
		 * built.
		 *
		 * @return {@code true} if aggregation branch root context.
		 */
		boolean isAggregationModeRoot() {

			return (this.aggregationBranchLevel == 0);
		}

		/**
		 * Build and get select list for the aggregate columns. Assumed to be
		 * called on the zero-level aggregation mode context.
		 *
		 * @return Aggregates select list.
		 */
		String getAggregatesSelectList() {

			// build select list
			final StringBuffer selectList = new StringBuffer(64);
			for (final AggregatePropertyHandler ph :
					this.aggregatedPropHandlers) {

				if (selectList.length() > 0)
					selectList.append(", ");

				final String valueColLabel;
				if (this.aggregationKeyPropName != null) {

					this.prepareNestedProperty(ph);

					valueColLabel = this.propColLabelPrefix + ph.getName();

					selectList
					.append(this.aggregationValueExprs.get(
							this.aggregationKeyPropName))
					.append(" AS ")
					.append(this.dialect.quoteColumnLabel(
							this.colLabelPrefix + ph.getName()))
					.append(", ");
				} else {
					valueColLabel = this.colLabelPrefix + ph.getName();
				}

				final String ending;
				switch (ph.getFunction()) {
				case COUNT:
					selectList.append("COUNT(");
					ending = ")";
					break;
				case COUNT_DISTINCT:
					selectList.append("COUNT(DISTINCT ");
					ending = ")";
					break;
				case SUM:
					selectList.append("COALESCE(SUM(");
					ending = "), 0)";
					break;
				case AVG:
					selectList.append("COALESCE(AVG(");
					ending = "), 0)";
					break;
				case MAX:
					selectList.append("MAX(");
					ending = ")";
					break;
				case MIN:
					selectList.append("MIN(");
					ending = ")";
					break;
				default: // may not happen
					throw new AssertionError("Invalid aggregation function.");
				}

				final Matcher m = ph.getValuePropertiesMatcher();
				if (m != null) {
					while (m.find())
						m.appendReplacement(selectList,
								this.aggregationValueExprs.get(m.group()));
					m.appendTail(selectList);
				} else {
					selectList.append(this.aggregationValueExprs.get(
							ph.getAggregatedCollectionHandler().getIdProperty()
								.getName()));
				}

				selectList.append(ending).append(" AS ")
					.append(this.dialect.quoteColumnLabel(valueColLabel));
			}

			return selectList.toString();
		}
	}


	/**
	 * Application resources manager.
	 */
	private final Resources resources;

	/**
	 * SQL dialect.
	 */
	private final SQLDialect dialect;

	/**
	 * Query parameter value handlers factory.
	 */
	private final ParameterValuesFactoryImpl paramsFactory;

	/**
	 * Filter specification.
	 */
	private final FilterSpec<?> filter;

	/**
	 * Order specification.
	 */
	private final OrderSpec<?> order;

	/**
	 * Root table name.
	 */
	private final String rootTableName;

	/**
	 * Alias of the root table.
	 */
	private final String rootTableAlias;

	/**
	 * Name of the record id column in the root table.
	 */
	private final String rootIdColName;

	/**
	 * Body of the "SELECT" clause for the properties.
	 */
	private final String selectList;

	/**
	 * Property paths that are included in the select list and need a
	 * single-valued join.
	 */
	private final Set<String> selectSingleJoins;

	/**
	 * Join expressions for all single-valued properties.
	 */
	private final SortedMap<String, String> allSingleJoins;

	/**
	 * Tail of the body of the "FROM" clause containing the collection joins.
	 */
	private final String collectionJoins;

	/**
	 * Body of the "GROUP BY" clause.
	 */
	private final String groupByList;

	/**
	 * Aggregation "WHERE" clause, if any.
	 */
	private final WhereClause aggregationWhereClause;

	/**
	 * Body of the "ORDER BY" clause containing collection parent ids.
	 */
	private final String orderByList;

	/**
	 * Single-valued property descriptors by property paths.
	 */
	private final Map<String, SingleValuedQueryProperty> singlePropExprs;

	/**
	 * Collection property stumps by property paths.
	 */
	private final Map<String, CollectionQueryProperty> collectionProps;

	/**
	 * Branches.
	 */
	private final Collection<QueryBranch> branches;


	/**
	 * Create new query builder.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param filter Filter specification.
	 * @param order Order specification.
	 * @param rootTableName Root table name.
	 * @param rootTableAlias Alias of the root table.
	 * @param rootIdColName Name of the record id column in the root table.
	 * @param selectList Body of the "SELECT" clause for the properties.
	 * @param selectSingleJoins Property paths that are included in the select
	 * list and need a single-valued join.
	 * @param allSingleJoins Join expressions for all single-valued properties.
	 * @param collectionJoins Tail of the body of the "FROM" clause containing
	 * the collection joins.
	 * @param groupByList Body of the "GROUP BY" clause.
	 * @param aggregationWhereClause Aggregation "WHERE" clause, if any.
	 * @param orderByList Body of the "ORDER BY" clause containing collection
	 * parent ids.
	 * @param singlePropExprs Single-valued property SQL value expressions by
	 * property paths.
	 * @param collectionProps Collection property stumps by property paths.
	 * @param branches Branches.
	 */
	private QueryBuilder(final Resources resources, final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final FilterSpec<?> filter, final OrderSpec<?> order,
			final String rootTableName, final String rootTableAlias,
			final String rootIdColName, final String selectList,
			final Set<String> selectSingleJoins,
			final SortedMap<String, String> allSingleJoins,
			final String collectionJoins, final String groupByList,
			final WhereClause aggregationWhereClause,
			final String orderByList,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final Map<String, CollectionQueryProperty> collectionProps,
			final Collection<QueryBranch> branches) {

		this.resources = resources;
		this.dialect = dialect;
		this.paramsFactory = paramsFactory;
		this.filter = filter;
		this.order = order;
		this.rootTableName = rootTableName;
		this.rootTableAlias = rootTableAlias;
		this.rootIdColName = rootIdColName;
		this.selectList = selectList;
		this.selectSingleJoins = selectSingleJoins;
		this.allSingleJoins = allSingleJoins;
		this.collectionJoins = collectionJoins;
		this.groupByList = groupByList;
		this.aggregationWhereClause = aggregationWhereClause;
		this.orderByList = orderByList;
		this.singlePropExprs = singlePropExprs;
		this.collectionProps = collectionProps;
		this.branches = branches;
	}


	/**
	 * Create query builder for the top persistent resource.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param actor Data consumer.
	 * @param prsrcHandler Persistent resource handler.
	 * @param propsFetch Properties fetch specification, or {@code null}.
	 * @param filter Filter specification, or {@code null}.
	 * @param order Order specification, or {@code null}.
	 *
	 * @return The query builder.
	 */
	static QueryBuilder createQueryBuilder(final Resources resources,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory, final Actor actor,
			final PersistentResourceHandler<?> prsrcHandler,
			final PropertiesFetchSpec<?> propsFetch, final FilterSpec<?> filter,
			final OrderSpec<?> order) {

		final QueryBranch b = createBranch(
				new QueryBuilderContext(
						resources,
						dialect,
						paramsFactory,
						actor,
						prsrcHandler,
						propsFetch,
						filter,
						order),
				"",
				false,
				null,
				null,
				null,
				null,
				null,
				"",
				prsrcHandler,
				prsrcHandler.getPersistentCollectionName(),
				"t",
				prsrcHandler.getIdProperty().getPersistence().getFieldName());

		final Log log = LogFactory.getLog(QueryBuilder.class);
		if (log.isDebugEnabled()) {
			final StringBuilder buf = new StringBuilder(512);
			appendQueryBuilderDesc(buf, b.getQueryBuilder());
			log.debug(buf.toString());
		}

		return b.getQueryBuilder();
	}

	/**
	 * Append query builder textual description to the buffer. Used for debug
	 * logging.
	 *
	 * @param buf Buffer, to which to append the description.
	 * @param qb The query builder.
	 */
	private static void appendQueryBuilderDesc(final StringBuilder buf,
			final QueryBuilder qb) {

		buf.append("# SINGLE PROPS:");
		final SortedMap<String, SingleValuedQueryProperty> singlePropExprs =
				new TreeMap<>(qb.singlePropExprs);
		for (final Map.Entry<String, SingleValuedQueryProperty> entry :
				singlePropExprs.entrySet())
			buf.append("\n  - ").append(entry.getKey()).append(": ")
				.append(entry.getValue().getValueExpression());

		buf.append("\n# COLLECTION STUMPS:");
		for (final Map.Entry<String, CollectionQueryProperty> entry :
				qb.collectionProps.entrySet()) {
			final CollectionQueryProperty stump = entry.getValue();
			buf.append("\n  - ").append(entry.getKey())
				.append(": propName=" + stump.getPropHandler().getName()
						+ ", colTblAlias=" + stump.getCollectionTableAlias()
						+ ", colTblName=" + stump.getCollectionTableName()
						+ ", colTblJoinCond="
							+ stump.getCollectionTableJoinCondition()
						+ ", keyExpr=" + stump.getKeyExpression()
						+ ", valueExpr=" + stump.getValueExpression());
		}

		buf.append("\n# ALL JOINS:");
		for (final Map.Entry<String, String> entry :
				qb.allSingleJoins.entrySet())
			buf.append("\n  - ").append(entry.getKey()).append(": ")
				.append(entry.getValue());

		buf.append("\n# SELECT SINGLE JOINS: ").append(qb.selectSingleJoins);

		buf.append("\n# COLLECTION JOINS: ").append(qb.collectionJoins);

		if (!qb.branches.isEmpty()) {
			buf.append("\n# BRANCHES:");
			int n = 0;
			for (final QueryBranch branch : qb.branches) {
				buf.append("\n  - BRANCH #").append(n++).append(":")
					.append("\n      - propPath: ")
					.append(branch.getNodePropertyPath())
					.append("\n      - anchorColExpr: ")
					.append(branch.getAnchorColumnExpression())
					.append("\n      - joinExpr: ")
					.append(branch.getJoinExpression());
				buf.append("\n  - BRANCH QUERY BUILDER:\n");
				appendQueryBuilderDesc(buf, branch.getQueryBuilder());
			}
		}
	}

	/**
	 * Create branch for a properties container object.
	 *
	 * @param parentCtx Parent context. The context property chain must be set
	 * to the branch property, or be empty if the top-level resource branch.
	 * @param parentPropPath Path to the property that is the container object,
	 * or empty string for the top-level resource.
	 * @param optional {@code true} if branch's objects are optional.
	 * @param anchorColExpr Anchor column expression, or {@code null} for the
	 * top-level resource.
	 * @param joinCondition Branch join condition, or {@code null} if the
	 * container is embedded in its parent table or it is the top-level
	 * resource.
	 * @param linkTableName Optional link table name for many-to-many
	 * relationship, or {@code null} for direct join.
	 * @param linkTableAlias Link table alias.
	 * @param linkJoinCondition Link table join condition.
	 * @param colLabelPrefix Prefix for the container properties column labels.
	 * @param container The container.
	 * @param tableName Table used to store the container properties.
	 * @param tableAlias Alias of the container table.
	 * @param idColName Record id column name. May be parent record id column
	 * name if the branch root object does not have its own record id.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createBranch(
			final QueryBuilderContext parentCtx,
			final String parentPropPath,
			final boolean optional,
			final String anchorColExpr,
			final String joinCondition,
			final String linkTableName,
			final String linkTableAlias,
			final String linkJoinCondition,
			final String colLabelPrefix,
			final ResourcePropertiesContainer container,
			final String tableName,
			final String tableAlias,
			final String idColName) {

		// get the id property handler
		final IdPropertyHandler idPropHandler = container.getIdProperty();

		// determine if outer joins must be forced
		final boolean forceOuter = (parentCtx.isForceOuter() || optional);

		// create parent join expression
		final String joinExpr;
		if (joinCondition != null) {
			if (linkTableName != null) {
				joinExpr =
					(forceOuter ?
							" LEFT OUTER JOIN " : " INNER JOIN ")
						+ linkTableName + " AS " + linkTableAlias
						+ " ON " + linkJoinCondition
						+ (forceOuter ?
								" LEFT OUTER JOIN " : " INNER JOIN ")
							+ tableName + " AS " + tableAlias
							+ " ON " + joinCondition;
			} else {
				joinExpr =
					(forceOuter ?
							" LEFT OUTER JOIN " : " INNER JOIN ")
						+ tableName + " AS " + tableAlias
						+ " ON " + joinCondition;
			}
		} else {
			joinExpr = "";
		}

		// create join attachment expression
		final String joinAttachmentExpr;
		if (!joinExpr.isEmpty() && (container.getIdProperty() != null)) {
			joinAttachmentExpr = tableAlias + "." + idColName;
		} else {
			joinAttachmentExpr = "";
		}

		// get id column value expression
		final String idColValExpr = (idPropHandler == null ? null :
			tableAlias + "." + idPropHandler.getPersistence().getFieldName());

		// create query builder context
		final QueryBuilderContext ctx = new QueryBuilderContext(parentCtx,
				parentPropPath, tableName, tableAlias, anchorColExpr,
				joinExpr, joinAttachmentExpr, colLabelPrefix, idColName,
				(idPropHandler == null ? null :
					idColValExpr + " AS " + parentCtx.dialect.quoteColumnLabel(
							colLabelPrefix + idPropHandler.getName())),
				forceOuter);

		// determine if polymorphic object
		final boolean polymorphic =
			((container instanceof ObjectPropertyHandler)
					&& (((ObjectPropertyHandler) container).getTypeProperty()
							!= null));

		// add record id to the select list and the properties
		if ((idPropHandler != null) && !polymorphic) {
			ctx.appendSelectList(idPropHandler.getName(), idColValExpr,
					idColValExpr);
			ctx.singlePropExprs.put(
					ctx.getPropertyPath(idPropHandler.getName()),
					new SingleValuedQueryProperty(
							ctx.rootTableAlias + "."
								+ idPropHandler.getPersistence().getFieldName(),
							idPropHandler.getValueHandler()
								.getPersistentValueType()));
		}

		// see if persistent resource
		final PersistentResourceHandler<?> prsrcHandler =
			(container instanceof PersistentResourceHandler ?
					(PersistentResourceHandler<?>) container : null);

		// add persistent resource meta-properties
		if (prsrcHandler != null) {
			for (final MetaPropertyType metaType : MetaPropertyType.values()) {
				final MetaPropertyHandler metaHandler =
					prsrcHandler.getMetaProperty(metaType);
				if (metaHandler == null)
					continue;
				final String propPath =
					ctx.getPropertyPath(metaHandler.getName());
				final String metaValExpr = ctx.rootTableAlias + "."
						+ metaHandler.getPersistence().getFieldName();
				ctx.singlePropExprs.put(propPath,
						new SingleValuedQueryProperty(metaValExpr,
								metaHandler.getValueHandler()
									.getPersistentValueType()));
				if (ctx.isSelected(propPath, metaHandler))
					ctx.appendSelectList(
							metaValExpr + " AS " + ctx.dialect.quoteColumnLabel(
									colLabelPrefix + metaHandler.getName()),
							false);
			}
		}

		// add simple properties
		for (final SimplePropertyHandler propHandler :
				container.getSimpleProperties())
			addSimpleProperty(ctx, propHandler);

		// add nested object properties
		for (final ObjectPropertyHandler propHandler :
				container.getObjectProperties())
			addObjectProperty(ctx, propHandler);

		// add reference properties
		for (final RefPropertyHandler propHandler :
				container.getRefProperties())
			addRefProperty(ctx, propHandler);

		// add properties specific to persistent resources
		if (prsrcHandler != null) {

			// add dependent resource reference properties
			for (final DependentRefPropertyHandler propHandler :
					prsrcHandler.getDependentRefProperties())
				addDependentRefProperty(ctx, propHandler);

			// collect all requested aggregate properties and group them
			final Map<String, List<AggregatePropertyHandler>>
			aggregatePropHandlers = new HashMap<>();
			int branchKeyDisc = 0;
			final Map<FilterSpec<? extends Object>, Integer> aggFilterDiscs =
				new HashMap<>();
			final Map<String, FilterSpec<? extends Object>> aggFilters =
				new HashMap<>();
			for (final AggregatePropertyHandler propHandler :
					prsrcHandler.getAggregateProperties()) {
				final String propPath =
					ctx.getPropertyPath(propHandler.getName());
				if (!ctx.isSelected(propPath, propHandler))
					continue;
				List<AggregatePropertyHandler> phList = null;
				final FilterSpec<? extends Object> aggFilter =
					ctx.getAggregateFilter(propPath);
				if (propHandler.getKeyPropertyName() != null) {
					final String branchKey =
						propHandler.getAggregatedCollectionPropertyPath()
						+ "/M" + (branchKeyDisc++);
					aggregatePropHandlers.put(branchKey,
							phList = new ArrayList<>());
				} else if (aggFilter != null) {
					Integer disc = aggFilterDiscs.get(aggFilter);
					if (disc == null)
						aggFilterDiscs.put(aggFilter,
								disc = Integer.valueOf(branchKeyDisc++));
					final String branchKey =
						propHandler.getAggregatedCollectionPropertyPath()
						+ "/F" + disc;
					aggFilters.put(branchKey, aggFilter);
					phList = aggregatePropHandlers.get(branchKey);
					if (phList == null) {
						phList = new ArrayList<>();
						aggregatePropHandlers.put(branchKey, phList);
					}
				} else {
					final Deque<? extends ResourcePropertyHandler> propChain =
						prsrcHandler.getPersistentPropertyChain(propHandler
								.getDeepAggregatedResourcePropertyPath());
					final StringBuilder subPath = new StringBuilder(
						propHandler.getDeepAggregatedResourcePropertyPath());
					for (final Iterator<? extends ResourcePropertyHandler> i =
						propChain.descendingIterator(); i.hasNext();) {
						final ResourcePropertyHandler ph = i.next();
						phList = aggregatePropHandlers.get(subPath.toString());
						if (phList != null)
							break;
						if (!ph.isSingleValued())
							break;
						if (i.hasNext())
							subPath.setLength(subPath.length()
									- ph.getName().length() - 1);
					}
					if (phList == null) {
						phList = new ArrayList<>();
						subPath.setLength(0);
						subPath.append(propHandler
								.getDeepAggregatedResourcePropertyPath());
						for (final Iterator<? extends ResourcePropertyHandler>
						i =
							propChain.descendingIterator(); i.hasNext();) {
							final ResourcePropertyHandler ph = i.next();
							final String key = subPath.toString();
							if (aggregatePropHandlers.containsKey(key))
								break;
							aggregatePropHandlers.put(key, phList);
							if (i.hasNext())
								subPath.setLength(subPath.length()
										- ph.getName().length() - 1);
						}
					}
				}
				phList.add(propHandler);
			}

			// add aggregate properties if any
			for (final Map.Entry<String, List<AggregatePropertyHandler>> entry :
					aggregatePropHandlers.entrySet())
				addAggregateProperties(ctx, prsrcHandler, entry.getValue(),
						aggFilters.get(entry.getKey()));
		}

		// order by record id if has collections
		if (ctx.collectionJoins.length() > 0)
			ctx.prependOrderByList(
					ctx.rootTableAlias + "." + ctx.rootIdColName);

		// create the branch object
		final QueryBuilder resQB = new QueryBuilder(
				ctx.resources,
				ctx.dialect,
				ctx.paramsFactory,
				ctx.filter,
				ctx.order,
				ctx.rootTableName,
				ctx.rootTableAlias,
				ctx.rootIdColName,
				ctx.getSelectList(),
				ctx.selectSingleJoins,
				ctx.allSingleJoins,
				ctx.collectionJoins.toString(),
				ctx.getGroupByList(),
				ctx.getAggregationWhereClause(),
				ctx.getOrderByList(),
				ctx.singlePropExprs,
				ctx.collectionProps,
				ctx.branches);
		final QueryBranch resBranch = new QueryBranch(resQB,
				ctx.parentAnchorColExpr, joinExpr, joinAttachmentExpr,
				ctx.mergedJoinAttachmentExprs.toString(), parentPropPath,
				parentCtx.propChain);

		// return the resulting branch
		return resBranch;
	}

	/**
	 * Create branch object for simple property stored in its own table.
	 *
	 * @param ctx The query builder context.
	 * @param propPath Property path.
	 * @param propTableName Property table name.
	 * @param propIdColName Name of the id column in the property table, or
	 * {@code null} if none.
	 * @param propValExpr Property value SQL expression.
	 * @param optional {@code true} if the values are optional.
	 * @param propAnchorColExpr Anchor column expression for the property in the
	 * parent.
	 * @param propJoinCondition Property table join condition.
	 * @param propAttachmentExpr Property table join attachment expression.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createBranch(final QueryBuilderContext ctx,
			final String propPath, final String propTableName,
			final String propIdColName, final String propValExpr,
			final boolean optional, final String propAnchorColExpr,
			final String propJoinCondition, final String propAttachmentExpr) {

		final String propJoinExpr =
				(optional || ctx.isForceOuter() ?
						" LEFT OUTER JOIN " : " INNER JOIN ")
					+ propTableName + " AS " + ctx.propTableAlias
					+ " ON " + propJoinCondition;

		return new QueryBranch(
				new QueryBuilder(
						ctx.resources,
						ctx.dialect,
						ctx.paramsFactory,
						ctx.filter,
						ctx.order,
						propTableName,
						ctx.propTableAlias,
						propIdColName,
						propValExpr,
						Collections.<String>emptySet(),
						CollectionUtils.<String, String>emptySortedMap(),
						"",
						"",
						null,
						"",
						Collections.<String, SingleValuedQueryProperty>
						emptyMap(),
						Collections.<String, CollectionQueryProperty>emptyMap(),
						Collections.<QueryBranch>emptyList()),
				propAnchorColExpr,
				propJoinExpr,
				propAttachmentExpr,
				null,
				propPath,
				ctx.propChain);
	}

	/**
	 * Add simple property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 */
	private static void addSimpleProperty(final QueryBuilderContext ctx,
			final SimplePropertyHandler propHandler) {

		// get property persistence
		final ResourcePropertyPersistence propPersistence =
			getPropertyPersistence(ctx, propHandler);
		if (propPersistence == null)
			return;

		// single of collection?
		if (propHandler.isSingleValued())
			addSingleSimpleProperty(ctx, propHandler, propPersistence);
		else // collection
			addBranch(ctx, createCollectionSimplePropertyBranch(ctx,
					propHandler, propPersistence),
					ctx.isSelected(ctx.getPropertyPath(propHandler.getName()),
							propHandler));
	}

	/**
	 * Add single-valued simple property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 */
	private static void addSingleSimpleProperty(final QueryBuilderContext ctx,
			final SimplePropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence) {

		// property value expression
		final String propValExpr = ctx.rootTableAlias + "."
				+ propPersistence.getFieldName();

		// add property to the single properties
		final String propPath = ctx.getPropertyPath(propHandler.getName());
		ctx.singlePropExprs.put(propPath,
				new SingleValuedQueryProperty(propValExpr,
						propHandler.getValueHandler()
							.getPersistentValueType()));

		// check if selected
		if (!ctx.isSelected(propPath, propHandler))
			return;

		// add property to the select list
		ctx.appendSelectList(propHandler.getName(), propValExpr, propValExpr);
	}

	/**
	 * Create collection simple property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createCollectionSimplePropertyBranch(
			final QueryBuilderContext ctx,
			final SimplePropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence) {

		// check that it's not embedded
		final String propTableName = propPersistence.getCollectionName();
		if (propTableName == null)
			throw new PersistenceException(
					"Embedded collections are not supported.");

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propValueExpr = ctx.propTableAlias + "."
				+ propPersistence.getFieldName();

		// property key expression
		final String propKeyExpr = getMapKeyColumnExpression(ctx.dialect,
				propHandler, propPersistence, ctx.propTableAlias);

		// add collection property stump
		final String propPath = ctx.getPropertyPath(propHandler.getName());
		final String propAttachmentExpr =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName();
		final String propJoinCondition = propAttachmentExpr
				+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler, propTableName,
						ctx.propTableAlias, propJoinCondition, propKeyExpr,
						propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr =
			(propKeyExpr != null ? propKeyExpr :
					"CASE WHEN " + propValueExpr + " IS NOT NULL THEN 1 END")
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// create property branch
		return createBranch(ctx, propPath, propTableName, null,
				propValueExpr + " AS " + ctx.dialect.quoteColumnLabel(
						ctx.propColLabelPrefix + "_value"),
				propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, propAttachmentExpr);
	}

	/**
	 * Add nested object property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 */
	private static void addObjectProperty(final QueryBuilderContext ctx,
			final ObjectPropertyHandler propHandler) {

		// get property persistence
		final ResourcePropertyPersistence propPersistence =
			getPropertyPersistence(ctx, propHandler);
		if (propPersistence == null)
			return;

		// construct property path
		final String propPath = ctx.getPropertyPath(propHandler.getName());

		// check if polymorphic
		final boolean polymorphic = (propHandler.getTypeProperty() != null);

		// prepare context to gather info about the concrete types
		if (polymorphic) {
			ctx.valueTypeColExpr = null;
			ctx.valueTypeTests.clear();
			ctx.valueTypeTestJoins.setLength(0);
		}

		// create property branch for embedded, single or collection
		final String propTableName = propPersistence.getCollectionName();
		if (propTableName == null)
			addBranch(ctx, createEmbeddedObjectPropertyBranch(ctx,
					propHandler, propPersistence, propPath),
					ctx.isSelected(propPath, propHandler));
		else if (propHandler.isSingleValued()) // single-valued
			addBranch(ctx, createSingleObjectPropertyBranch(ctx,
					propHandler, propPersistence, propPath, propTableName),
					ctx.isSelected(propPath, propHandler));
		else // not embedded collection
			addBranch(ctx, createCollectionObjectPropertyBranch(ctx,
					propHandler, propPersistence, propPath, propTableName),
					ctx.isSelected(propPath, propHandler));

		// add polymorphic type property
		if (polymorphic && propHandler.isSingleValued()) {
			final String typeExpr;
			final TypePropertyHandler typeHandler =
				propHandler.getTypeProperty();
			final ResourcePropertyPersistence typePersistence =
				typeHandler.getPersistence();
			if (typePersistence != null) {
				typeExpr = ctx.valueTypeColExpr;
			} else { // no persistent type column

				// build type value expression
				final StringBuilder typeExprBuf = new StringBuilder(256);
				typeExprBuf.append("(CASE");
				for (final String testExpr : ctx.valueTypeTests)
					typeExprBuf.append(testExpr);
				typeExprBuf.append(" END)");
				typeExpr = typeExprBuf.toString();

				// add concrete type joins
				ctx.allSingleJoins.put(propPath + "." + typeHandler.getName(),
						ctx.valueTypeTestJoins.toString());
			}
			ctx.singlePropExprs.put(propPath + "." + typeHandler.getName(),
					new SingleValuedQueryProperty(typeExpr,
							typeHandler.getValueHandler()
								.getPersistentValueType()));
		}
	}

	/**
	 * Create embedded nested object property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createEmbeddedObjectPropertyBranch(
			final QueryBuilderContext ctx,
			final ObjectPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath) {

		// check that it's not a collection
		if (!propHandler.isSingleValued())
			throw new PersistenceException(
					"Embedded collections are not supported.");

		// column label prefix
		ctx.prepareNestedProperty(propHandler);

		// value type column expression
		final TypePropertyHandler typeHandler = propHandler.getTypeProperty();
		if ((typeHandler != null) && (typeHandler.getPersistence() != null))
			ctx.valueTypeColExpr = ctx.rootTableAlias + "."
					+ typeHandler.getPersistence().getFieldName();

		// create anchor column expression
		final String propAnchorColExpr;
		if (propHandler.isConcreteType()) {
			final String testExpr;
			if (ctx.valueTypeColExpr != null)
				testExpr = ctx.valueTypeColExpr;
			else
				testExpr = ctx.rootTableAlias + "." + ctx.rootIdColName;
			propAnchorColExpr =
				"CASE " + testExpr
					+ " WHEN '" + propHandler.getName() + "' THEN '"
					+ propHandler.getName() + "' END AS "
					+ ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		} else {
			propAnchorColExpr =
				ctx.rootTableAlias + "." + ctx.rootIdColName + " AS "
					+ ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		}

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, null, null, null, null,
				ctx.propColLabelPrefix, propHandler, ctx.rootTableName,
				ctx.rootTableAlias, ctx.rootIdColName);
	}

	/**
	 * Create single-valued not embedded nested object property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param propTableName Nested object table name.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createSingleObjectPropertyBranch(
			final QueryBuilderContext ctx,
			final ObjectPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final String propTableName) {

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// value type column expression
		final TypePropertyHandler typeHandler = propHandler.getTypeProperty();
		if ((typeHandler != null) && (typeHandler.getPersistence() != null))
			ctx.valueTypeColExpr = ctx.propTableAlias + "."
					+ typeHandler.getPersistence().getFieldName();

		// create join condition
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;

		// create anchor column expression
		final String propAnchorColExpr;
		if (propHandler.isConcreteType()) {
			final String valueTypeTest =
				" WHEN " + ctx.propTableAlias + "."
					+ propPersistence.getParentIdFieldName()
					+ " IS NOT NULL THEN '" + propHandler.getName() + "'";
			ctx.parentCtx.valueTypeTests.add(valueTypeTest);
			ctx.parentCtx.valueTypeTestJoins.append(" LEFT OUTER JOIN ")
				.append(propTableName).append(" AS ").append(ctx.propTableAlias)
				.append(" ON ").append(propJoinCondition);
			propAnchorColExpr =
				"CASE" + valueTypeTest + " END AS "
					+ ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		} else {
			propAnchorColExpr =
				ctx.propTableAlias + "."
					+ propPersistence.getParentIdFieldName() + " AS "
					+ ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		}

		// record id column
		final String propIdColName;
		final IdPropertyHandler propIdHandler = propHandler.getIdProperty();
		if (propIdHandler != null)
			propIdColName = propIdHandler.getPersistence().getFieldName();
		else
			propIdColName = propPersistence.getParentIdFieldName();

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, propHandler, propTableName,
				ctx.propTableAlias, propIdColName);
	}

	/**
	 * Create collection nested object property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param propTableName Nested object table name.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createCollectionObjectPropertyBranch(
			final QueryBuilderContext ctx,
			final ObjectPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final String propTableName) {

		// get the object id
		final IdPropertyHandler propIdHandler = propHandler.getIdProperty();
		if (propIdHandler == null)
			throw new PersistenceException("Nested object in a collection must"
					+ " have an id property.");

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// value type column expression
		final TypePropertyHandler typeHandler = propHandler.getTypeProperty();
		if ((typeHandler != null) && (typeHandler.getPersistence() != null))
			ctx.valueTypeColExpr = ctx.propTableAlias + "."
					+ typeHandler.getPersistence().getFieldName();

		// property value expression
		final String propIdColName =
				propIdHandler.getPersistence().getFieldName();
		final String propValueExpr = ctx.propTableAlias + "." + propIdColName;

		// property key expression
		final String propKeyExpr = getMapKeyColumnExpression(ctx.dialect,
				propHandler, propPersistence, ctx.propTableAlias);

		// add collection property stump
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler, propTableName,
						ctx.propTableAlias, propJoinCondition, propKeyExpr,
						propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr;
		if (propHandler.isConcreteType()) {
			final String valueTypeTest =
				" WHEN " + propValueExpr + " IS NOT NULL THEN '"
						+ propHandler.getName() + "'";
			ctx.parentCtx.valueTypeTests.add(valueTypeTest);
			ctx.parentCtx.valueTypeTestJoins.append(" LEFT OUTER JOIN ")
				.append(propTableName).append(" AS ").append(ctx.propTableAlias)
				.append(" ON ").append(propJoinCondition);
			propAnchorColExpr =
				"CASE" + valueTypeTest + " END AS "
					+ ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		} else {
			propAnchorColExpr =
				(propKeyExpr != null ? propKeyExpr : propValueExpr)
					+ " AS " + ctx.dialect.quoteColumnLabel(
							ctx.colLabelPrefix + propHandler.getName());
		}

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, propHandler, propTableName,
				ctx.propTableAlias, propIdColName);
	}

	/**
	 * Add reference property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 */
	private static void addRefProperty(final QueryBuilderContext ctx,
			final RefPropertyHandler propHandler) {

		// get property persistence
		final ResourcePropertyPersistence propPersistence =
			getPropertyPersistence(ctx, propHandler);
		if (propPersistence == null)
			return;

		// construct property path
		final String propPath = ctx.getPropertyPath(propHandler.getName());

		// get reference target
		final Class<?> refTargetClass = propHandler.getReferredResourceClass();

		// check if fetch requested
		if (ctx.isFetchRequested(propPath)) {

			// single of collection?
			if (propHandler.isSingleValued())
				addBranch(ctx, createSingleFetchedRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), ctx.isSelected(propPath, propHandler));
			else // collection
				addBranch(ctx, createCollectionFetchedRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), ctx.isSelected(propPath, propHandler));

		} else { // no fetch requested

			// single of collection?
			if (propHandler.isSingleValued())
				addSingleRefProperty(ctx, propHandler, propPersistence,
						propPath, refTargetClass);
			else // collection
				addBranch(ctx, createCollectionRefPropertyBranch(ctx,
						propHandler, propPersistence, refTargetClass),
						ctx.isSelected(propPath, propHandler));
		}
	}

	/**
	 * Add single-valued not fetched reference property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 */
	private static void addSingleRefProperty(final QueryBuilderContext ctx,
			final RefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler and table
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);
		final String propTableName =
			refTargetHandler.getPersistentCollectionName();

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propValExpr =
			ctx.rootTableAlias + "." + propPersistence.getFieldName();

		// add property to the single properties
		ctx.singlePropExprs.put(propPath,
				new SingleValuedQueryProperty(propValExpr,
						propHandler.getValueHandler()
							.getPersistentValueType()));

		// create column value expression
		final String propColValExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
					propValExpr);

		// add to select list if selected
		if (ctx.isSelected(propPath, propHandler))
			ctx.appendSelectList(propHandler.getName(), propColValExpr,
					propValExpr);

		// check if used
		if (!isUsed(ctx, propPath))
			return;

		// create join expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propJoinCondition =
			ctx.propTableAlias + "." + propIdColName
			+ " = " + ctx.rootTableAlias + "." + propPersistence.getFieldName();

		// create property branch
		addBranch(ctx, createBranch(ctx, propPath, propPersistence.isOptional(),
				propColValExpr + " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName()),
				propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, refTargetHandler, propTableName,
				ctx.propTableAlias, propIdColName), false);
	}

	/**
	 * Create collection not fetched reference property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createCollectionRefPropertyBranch(
			final QueryBuilderContext ctx, final RefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final Class<?> refTargetClass) {

		// check that it's not embedded
		final String propTableName = propPersistence.getCollectionName();
		if (propTableName == null)
			throw new PersistenceException(
					"Embedded collections are not supported.");

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propValueExpr = ctx.propTableAlias + "."
				+ propPersistence.getFieldName();

		// property key expression
		final String propKeyExpr = getMapKeyColumnExpression(ctx.dialect,
				propHandler, propPersistence, ctx.propTableAlias);

		// add collection property stump
		final String propPath = ctx.getPropertyPath(propHandler.getName());
		final String propAttachmentExpr =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName();
		final String propJoinCondition =
			propAttachmentExpr
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler, propTableName,
						ctx.propTableAlias, propJoinCondition, propKeyExpr,
						propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr =
				(propKeyExpr != null ? propKeyExpr : propValueExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// create property branch
		return createBranch(ctx, propPath, propTableName, null,
				ctx.dialect.nullableConcat(
						refTargetClass.getSimpleName() + "#", propValueExpr)
					+ " AS " + ctx.dialect.quoteColumnLabel(
							ctx.propColLabelPrefix + "_value"),
				propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, propAttachmentExpr);
	}

	/**
	 * Create single-valued fetched reference property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createSingleFetchedRefPropertyBranch(
			final QueryBuilderContext ctx, final RefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler and table
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);
		final String propTableName =
			refTargetHandler.getPersistentCollectionName();

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propValExpr =
			ctx.rootTableAlias + "." + propPersistence.getFieldName();

		// add property to the single properties
		ctx.singlePropExprs.put(propPath,
				new SingleValuedQueryProperty(propValExpr,
						propHandler.getValueHandler()
							.getPersistentValueType()));

		// create anchor column expression
		final String propAnchorColExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
					propValExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName() + ":");

		// create join expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propJoinCondition =
			ctx.propTableAlias + "." + propIdColName
			+ " = " + ctx.rootTableAlias + "." + propPersistence.getFieldName();

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, refTargetHandler, propTableName,
				ctx.propTableAlias, propIdColName);
	}

	/**
	 * Create collection fetched reference property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createCollectionFetchedRefPropertyBranch(
			final QueryBuilderContext ctx, final RefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// check that it's not embedded
		final String linkTableName = propPersistence.getCollectionName();
		if (linkTableName == null)
			throw new PersistenceException(
					"Embedded collections are not supported.");

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// get target resource handler and table
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);
		final String refTargetTableName =
			refTargetHandler.getPersistentCollectionName();
		final String linkTableAlias = ctx.propTableAlias + "0";

		// property value expression
		final String propValueExpr =
			linkTableAlias + "." + propPersistence.getFieldName();

		// property key expression
		final String propKeyExpr = getMapKeyColumnExpression(ctx.dialect,
				propHandler, propPersistence, linkTableAlias);

		// add collection property stump
		final String linkJoinCondition =
			linkTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler, linkTableName,
						linkTableAlias, linkJoinCondition, propKeyExpr,
						propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr =
				(propKeyExpr != null ? propKeyExpr : propValueExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName())
				+ ", " + ctx.dialect.nullableConcat(
						refTargetClass.getSimpleName() + "#", propValueExpr)
					+ " AS " + ctx.dialect.quoteColumnLabel(
							ctx.propColLabelPrefix + "_value:");

		// create join expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propJoinCondition =
			ctx.propTableAlias + "." + propIdColName
			+ " = " + propValueExpr;

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, linkTableName,
				linkTableAlias, linkJoinCondition, ctx.propColLabelPrefix,
				refTargetHandler, refTargetTableName, ctx.propTableAlias,
				propIdColName);
	}

	/**
	 * Add dependent resource reference property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 */
	private static void addDependentRefProperty(final QueryBuilderContext ctx,
			final DependentRefPropertyHandler propHandler) {

		// get property persistence
		final ResourcePropertyPersistence propPersistence =
			getPropertyPersistence(ctx, propHandler);

		// construct property path
		final String propPath = ctx.getPropertyPath(propHandler.getName());

		// get reference target
		final Class<?> refTargetClass = propHandler.getReferredResourceClass();

		// check if fetch requested
		if (ctx.isFetchRequested(propPath)) {

			// single of collection?
			if (propHandler.isSingleValued())
				addBranch(ctx, createSingleFetchedDependentRefPropertyBranch(
						ctx, propHandler, propPersistence, propPath,
						refTargetClass), ctx.isSelected(propPath, propHandler));
			else // collection
				addBranch(ctx,
						createCollectionFetchedDependentRefPropertyBranch(ctx,
								propHandler, propPersistence, propPath,
								refTargetClass), ctx.isSelected(propPath,
										propHandler));

		} else { // no fetch requested

			// single of collection?
			if (propHandler.isSingleValued())
				addSingleDependentRefProperty(ctx, propHandler, propPersistence,
						propPath, refTargetClass);
			else // collection
				addBranch(ctx, createCollectionDependentRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), ctx.isSelected(propPath, propHandler));
		}
	}

	/**
	 * Add single-valued not fetched dependent resource reference property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 */
	private static void addSingleDependentRefProperty(
			final QueryBuilderContext ctx,
			final DependentRefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propValExpr = ctx.propTableAlias + "." + propIdColName;

		// add property to the single properties
		ctx.singlePropExprs.put(propPath,
				new SingleValuedQueryProperty(propValExpr,
						propHandler.getValueHandler()
							.getPersistentValueType()));

		// create column value expression
		final String propColValExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
				propValExpr);

		// create join expression
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;

		// add to select list if selected
		if (ctx.isSelected(propPath, propHandler)) {

			// add the join (will be created by the branch)
			ctx.selectSingleJoins.add(propPath);

			// add property to the select list
			ctx.appendSelectList(propHandler.getName(), propColValExpr,
					propValExpr);
		}

		// create branch
		final QueryBranch branch = createBranch(ctx, propPath,
				propPersistence.isOptional(),
				propColValExpr + " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName()),
				propJoinCondition, null, null, null, ctx.propColLabelPrefix,
				refTargetHandler, propPersistence.getCollectionName(),
				ctx.propTableAlias, propIdColName);

		// check if used
		if (!isUsed(ctx, propPath)) {

			// save the join
			ctx.allSingleJoins.put(propPath, branch.getJoinExpression());

			// no need for the branch
			return;
		}

		// add the branch if used
		addBranch(ctx, branch, false);
	}

	/**
	 * Create collection not fetched dependent resource reference property
	 * branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createCollectionDependentRefPropertyBranch(
			final QueryBuilderContext ctx,
			final DependentRefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propValueExpr = ctx.propTableAlias + "."
				+ propIdColName;

		// add collection property stump
		final String propTableName = propPersistence.getCollectionName();
		final String propAttachmentExpr =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName();
		final String propJoinCondition = propAttachmentExpr
				+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler,
						propTableName, ctx.propTableAlias,
						propJoinCondition, null, propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr =
			propValueExpr
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// create property branch
		return createBranch(ctx, propPath, propTableName, propIdColName,
				ctx.dialect.nullableConcat(
						refTargetClass.getSimpleName() + "#", propValueExpr)
					+ " AS " + ctx.dialect.quoteColumnLabel(
							ctx.propColLabelPrefix + "_value"),
				propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, propAttachmentExpr);
	}

	/**
	 * Create single-valued fetched dependent resource reference property
	 * branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createSingleFetchedDependentRefPropertyBranch(
			final QueryBuilderContext ctx,
			final DependentRefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propValExpr = ctx.propTableAlias + "." + propIdColName;

		// add property to the single properties
		ctx.singlePropExprs.put(propPath,
				new SingleValuedQueryProperty(propValExpr,
						propHandler.getValueHandler()
							.getPersistentValueType()));

		// create anchor column expression
		final String propAnchorColExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
					propValExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName() + ":");

		// create join expression
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;

		// create property branch
		final String propTableName = propPersistence.getCollectionName();
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, refTargetHandler, propTableName,
				ctx.propTableAlias, propIdColName);
	}

	/**
	 * Create collection fetched dependent resource reference property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch
	createCollectionFetchedDependentRefPropertyBranch(
			final QueryBuilderContext ctx,
			final DependentRefPropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propValueExpr = ctx.propTableAlias + "." + propIdColName;

		// add collection property stump
		final String propTableName = propPersistence.getCollectionName();
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
			+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler, propTableName,
						ctx.propTableAlias, propJoinCondition, null,
						propValueExpr));

		// create anchor column expression
		final String propAnchorColExpr =
			propValueExpr
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName())
				+ ", " + ctx.dialect.nullableConcat(
						refTargetClass.getSimpleName() + "#", propValueExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.propColLabelPrefix + "_value:");

		// create property branch
		return createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
				ctx.propColLabelPrefix, refTargetHandler, propTableName,
				ctx.propTableAlias, propIdColName);
	}

	/**
	 * Add aggregate properties that use same aggregation collection and
	 * grouping.
	 *
	 * @param ctx Query builder context.
	 * @param prsrcHandler Handler of the persistent resource containing the
	 * aggregates.
	 * @param propHandlers Aggregate property handlers.
	 * @param aggFilter Aggregated collection filter, or {@code null} if none.
	 */
	private static void addAggregateProperties(
			final QueryBuilderContext ctx,
			final PersistentResourceHandler<?> prsrcHandler,
			final List<AggregatePropertyHandler> propHandlers,
			final FilterSpec<? extends Object> aggFilter) {

		// get collection property path
		final String collectionPropPath =
			propHandlers.get(0).getAggregatedCollectionPropertyPath();

		// switch context into the aggregation mode
		ctx.setAggregationMode(collectionPropPath,
				propHandlers.get(0).getKeyPropertyName(), propHandlers,
				aggFilter);

		// get aggregated collection property chain
		final Deque<? extends ResourcePropertyHandler> collectionPropChain =
			prsrcHandler.getPersistentPropertyChain(collectionPropPath);

		// get branch root property handler
		final ResourcePropertyHandler rootPropHandler =
			collectionPropChain.getFirst();

		// create branch depending on the root property kind
		if (rootPropHandler instanceof ObjectPropertyHandler)
			addObjectProperty(ctx, (ObjectPropertyHandler) rootPropHandler);
		else if (rootPropHandler instanceof RefPropertyHandler)
			addRefProperty(ctx, (RefPropertyHandler) rootPropHandler);
		else if (rootPropHandler instanceof DependentRefPropertyHandler)
			addDependentRefProperty(ctx,
					(DependentRefPropertyHandler) rootPropHandler);
		else // cannot happen
			throw new AssertionError("Unexpected aggregate property collection"
					+ " chain root property type "
					+ rootPropHandler.getClass().getName() + ".");

		// restore normal context mode
		ctx.clearAggregationMode();
	}

	/**
	 * Get property persistence descriptor.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 *
	 * @return Property persistence descriptor, or {@code null} if the property
	 * is transient of loading it is not allowed to the actor.
	 */
	private static ResourcePropertyPersistence getPropertyPersistence(
			final QueryBuilderContext ctx,
			final ResourcePropertyHandler propHandler) {

		// check if persistent
		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		if (propPersistence == null)
			return null;

		// check if allowed to load
		if (!propHandler.isAllowed(ResourcePropertyAccess.LOAD, ctx.actor))
			return null;

		// got a persistent property to fetch
		return propPersistence;
	}

	/**
	 * Get map key column expression.
	 *
	 * @param dialect SQL dialect.
	 * @param propHandler Map property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propTableAlias Alias of the table containing the keys.
	 *
	 * @return Key column expression, or {@code null} if not a map.
	 */
	private static String getMapKeyColumnExpression(final SQLDialect dialect,
			final ResourcePropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propTableAlias) {

		final String keyColName = propPersistence.getKeyFieldName();

		if (keyColName == null)
			return null;

		final ResourcePropertyValueHandler keyHandler =
			propHandler.getKeyValueHandler();

		if (keyHandler.isRef())
			return dialect.nullableConcat(
					keyHandler.getRefTargetClass().getSimpleName() + "#",
					propTableAlias + "." + keyColName);

		return propTableAlias + "." + keyColName;
	}

	/**
	 * Tell if the specified property is used by the filter or the order
	 * specification.
	 *
	 * @param ctx Builder context.
	 * @param propPath Property path.
	 *
	 * @return {@code true} if used.
	 */
	private static boolean isUsed(final QueryBuilderContext ctx,
			final String propPath) {

		return (((ctx.filter != null) && ctx.filter.isUsed(propPath))
				|| ((ctx.order != null) && ctx.order.isUsed(propPath)));
	}

	/**
	 * Add branch to the query builder context by merging it into it, or adding
	 * it as a branch.
	 *
	 * @param ctx Query builder context.
	 * @param branch Branch to add, or {@code null} to do nothing.
	 * @param selected {@code true} if selected.
	 */
	private static void addBranch(final QueryBuilderContext ctx,
			final QueryBranch branch, final boolean selected) {

		// do we have a branch to add?
		if (branch == null)
			return;

		// check if selected and/or used
		final String branchPropPath = branch.getNodePropertyPath();
		final boolean used = isUsed(ctx, branchPropPath);
		if (!selected && !used)
			return;

		// check if adding aggregation branch
		final boolean aggregation = ctx.isAggregationModeRoot();

		// check if collection and is not selected
		final boolean collection = (aggregation || branch.isCollection());
		if (collection && !selected)
			return;

		// decide whether to merge or attach
		if (!collection) {
			final QueryBuilder branchQB = branch.getQueryBuilder();
			if (!branchQB.hasCollections())
				mergeBranch(ctx, branch, collection, selected);
			else if (ctx.collectionJoins.length() > 0)
				attachBranch(ctx, branch, collection, selected);
			else
				mergeBranch(ctx, branch, collection, selected);
		} else { // selected collection
			if (ctx.collectionJoins.length() > 0)
				attachBranch(ctx, branch, collection, selected);
			else
				mergeBranch(ctx, branch, collection, selected);
		}
	}

	/**
	 * Merge branch to the query context.
	 *
	 * @param ctx The context, to which to merge the branch.
	 * @param branch The branch.
	 * @param collection {@code true} if merging in collection mode.
	 * @param selected {@code true} if branch property is selected.
	 */
	private static void mergeBranch(final QueryBuilderContext ctx,
			final QueryBranch branch, final boolean collection,
			final boolean selected) {

		// get branch query builder
		final QueryBuilder branchQB = branch.getQueryBuilder();

		// check if branch has collections
		final boolean hasCollections = branchQB.hasCollections();

		// merge select list if selected
		if (selected) {

			// add anchor column
			final String anchorColExpr = (ctx.isAggregationModeRoot() ?
					ctx.getAggregatesSelectList() :
					branch.getAnchorColumnExpression());
			if (!anchorColExpr.isEmpty())
				ctx.appendSelectList(anchorColExpr,
						collection || hasCollections);

			// add branch select list
			if (!branchQB.selectList.isEmpty())
				ctx.appendSelectList(branchQB.selectList,
						collection || hasCollections);
		}

		// merge branch joins
		if (selected) {

			if (!collection) {
				final String branchJoinExpr = branch.getJoinExpression();
				if (!branchJoinExpr.isEmpty()) {
					ctx.selectSingleJoins.add(branch.getNodePropertyPath());
					ctx.allSingleJoins.put(branch.getNodePropertyPath(),
							branch.getJoinExpression());
				}
				ctx.selectSingleJoins.addAll(branchQB.selectSingleJoins);
				ctx.allSingleJoins.putAll(branchQB.allSingleJoins);
				ctx.collectionJoins.append(branchQB.collectionJoins);
			} else {
				ctx.collectionJoins.append(branch.getJoinExpression());
				for (final Map.Entry<String, String> entry :
						branchQB.allSingleJoins.entrySet()) {
					if (branchQB.selectSingleJoins.contains(entry.getKey()))
						ctx.collectionJoins.append(entry.getValue());
				}
				ctx.collectionJoins.append(branchQB.collectionJoins);
			}

		} else { // not selected, but used

			if (!collection) {
				ctx.allSingleJoins.put(branch.getNodePropertyPath(),
						branch.getJoinExpression());
				ctx.allSingleJoins.putAll(branchQB.allSingleJoins);
			}
		}

		// merge join attachments
		if ((selected || !collection) && !ctx.isAggregationModeRoot()) {
			final String branchJoinAttachmentExpr =
				branch.getJoinAttachmentExpression();
			if (!branchJoinAttachmentExpr.isEmpty()) {
				if (ctx.mergedJoinAttachmentExprs.length() > 0)
					ctx.mergedJoinAttachmentExprs.append(", ");
				ctx.mergedJoinAttachmentExprs.append(branchJoinAttachmentExpr);
			}
			final String branchMergedJoinAttachmentExprs =
				branch.getMergedJoinAttachmentExpressions();
			if ((branchMergedJoinAttachmentExprs != null)
					&& !branchMergedJoinAttachmentExprs.isEmpty()) {
				if (ctx.mergedJoinAttachmentExprs.length() > 0)
					ctx.mergedJoinAttachmentExprs.append(", ");
				ctx.mergedJoinAttachmentExprs.append(
						branchMergedJoinAttachmentExprs);
			}
		}

		// merge properties
		if (!collection) {
			ctx.singlePropExprs.putAll(branchQB.singlePropExprs);
			ctx.collectionProps.putAll(branchQB.collectionProps);
		}

		// merge or add group by list
		if (ctx.isAggregationModeRoot()) {
			final StringBuilder groupByList = new StringBuilder(64);
			groupByList.append(ctx.createGroupByList());
			if (ctx.mergedJoinAttachmentExprs.length() > 0)
				groupByList.append(", ").append(ctx.mergedJoinAttachmentExprs);
			ctx.setGroupByList(groupByList.toString());
			ctx.setAggregationWhereClause(ctx.createAggregationWhereClause());
		} else {
			ctx.setGroupByList(branchQB.groupByList);
			ctx.setAggregationWhereClause(branchQB.aggregationWhereClause);
		}

		// merge order by list
		if (selected && !branchQB.orderByList.isEmpty())
			ctx.appendOrderByList(branchQB.orderByList);

		// add branches
		for (final QueryBranch subBranch : branchQB.branches)
			addBranch(ctx, subBranch, selected);
	}

	/**
	 * Attach branch to the query context.
	 *
	 * @param ctx The context, to which to merge the branch.
	 * @param branch The branch.
	 * @param collection {@code true} if merging in collection mode.
	 * @param selected {@code true} if branch property is selected.
	 */
	private static void attachBranch(final QueryBuilderContext ctx,
			final QueryBranch branch, final boolean collection,
			final boolean selected) {

		// get branch query builder
		final QueryBuilder branchQB = branch.getQueryBuilder();

		// include branch's single joins
		final String branchPropPath = branch.getNodePropertyPath();
		if (!collection) {
			ctx.allSingleJoins.put(branchPropPath, branch.getJoinExpression());
			ctx.allSingleJoins.putAll(branchQB.allSingleJoins);
		}

		// make branch's properties available
		if (!collection) {
			ctx.singlePropExprs.putAll(branchQB.singlePropExprs);
			ctx.collectionProps.putAll(branchQB.collectionProps);
		}

		// re-branch the branch and include in the context branches
		if (selected) {

			// create re-branched joins
			final Set<String> rebranchedSelectSingleJoins = new HashSet<>();
			final SortedMap<String, String> rebranchedAllSingleJoins =
				new TreeMap<>();
			final StringBuilder rebranchedCollectionJoins =
				new StringBuilder(128);
			if (!collection) {
				rebranchedSelectSingleJoins.addAll(branchQB.selectSingleJoins);
				rebranchedAllSingleJoins.putAll(branchQB.allSingleJoins);
				final String branchJoinExpr = branch.getJoinExpression();
				if (!branchJoinExpr.isEmpty()) {
					rebranchedSelectSingleJoins.add(branchPropPath);
					final String existingJoin =
						rebranchedAllSingleJoins.get(branchPropPath);
					if (existingJoin != null)
						rebranchedAllSingleJoins.put(branchPropPath,
								branchJoinExpr + existingJoin);
					else
						rebranchedAllSingleJoins.put(branchPropPath,
								branchJoinExpr);
				}
				rebranchedCollectionJoins.append(branchQB.collectionJoins);
			} else {
				rebranchedCollectionJoins.append(branch.getJoinExpression());
				for (final Map.Entry<String, String> entry :
						branchQB.allSingleJoins.entrySet()) {
					if (branchQB.selectSingleJoins.contains(entry.getKey()))
						rebranchedCollectionJoins.append(entry.getValue());
				}
				rebranchedCollectionJoins.append(branchQB.collectionJoins);
			}

			// create re-branched order by list
			final StringBuilder rebranchedOrderByList = new StringBuilder(128);
			if (collection)
				rebranchedOrderByList.append(ctx.rootTableAlias).append(".")
					.append(ctx.rootIdColName);
			if (!branchQB.orderByList.isEmpty()) {
				if (rebranchedOrderByList.length() > 0)
					rebranchedOrderByList.append(", ");
				rebranchedOrderByList.append(branchQB.orderByList);
			}

			// create re-branched query builder
			final QueryBuilder rebranchedQB = new QueryBuilder(
					ctx.resources,
					ctx.dialect,
					ctx.paramsFactory,
					ctx.filter,
					ctx.order,
					ctx.rootTableName,
					ctx.rootTableAlias,
					ctx.rootIdColName,
					(ctx.rootIdColExpr == null ? "" : ctx.rootIdColExpr + ", ")
						+ (ctx.isAggregationModeRoot() ?
								ctx.getAggregatesSelectList() :
								branch.getAnchorColumnExpression())
						+ (branchQB.selectList.isEmpty() ? "" :
								", " + branchQB.selectList),
					rebranchedSelectSingleJoins,
					rebranchedAllSingleJoins,
					rebranchedCollectionJoins.toString(),
					(!ctx.isAggregationModeRoot() ?
						branchQB.groupByList :
						ctx.createGroupByList()),
					(!ctx.isAggregationModeRoot() ?
						branchQB.aggregationWhereClause :
						ctx.createAggregationWhereClause()),
					rebranchedOrderByList.toString(),
					Collections.<String, SingleValuedQueryProperty>emptyMap(),
					Collections.<String, CollectionQueryProperty>emptyMap(),
					Collections.<QueryBranch>emptyList());

			// create re-branched branch and add it to the context
			ctx.branches.add(new QueryBranch(
					rebranchedQB,
					ctx.parentAnchorColExpr,
					ctx.parentJoinExpr,
					ctx.parentJoinAttachmentExpr,
					ctx.mergedJoinAttachmentExprs.toString(),
					branchPropPath,
					branch.getAttachmentChain()));
		}

		// attach branch branches
		for (final QueryBranch b : branchQB.branches)
			attachBranch(ctx, b, b.isCollection(), selected);
	}


	/**
	 * Tell if the query selects any collections. Result sets of queries without
	 * collections always have the same number of rows as the number of the
	 * resulting records, which is not true for queries with collections, whose
	 * result set lengths vary depending on the number of elements in the nested
	 * collections.
	 *
	 * @return {@code true} if has collections.
	 */
	boolean hasCollections() {

		return !this.collectionJoins.isEmpty();
	}

	/**
	 * Get branches.
	 *
	 * @return The branches. May be empty but never {@code null}.
	 */
	Collection<QueryBranch> getBranches() {

		return this.branches;
	}

	/**
	 * Get query root table name.
	 *
	 * @return The table name.
	 */
	String getRootTableName() {

		return this.rootTableName;
	}

	/**
	 * Get query root table alias.
	 *
	 * @return The table alias.
	 */
	String getRootTableAlias() {

		return this.rootTableAlias;
	}

	/**
	 * Build "WHERE" clause for the query.
	 *
	 * @param params Map, to which to add query parameters.
	 *
	 * @return Object representing the SQL "WHERE" clause.
	 */
	WhereClause buildWhereClause(final Map<String, JDBCParameterValue> params) {

		return new WhereClause(this.resources, this.dialect, this.paramsFactory,
				this.filter, "p", this.singlePropExprs, this.collectionProps,
				this.allSingleJoins, params);
	}

	/**
	 * Build "ORDER BY" clause for the query.
	 *
	 * @param params Map, to which to add query parameters.
	 *
	 * @return Object representing the SQL "ORDER BY" clause.
	 */
	OrderByClause buildOrderByClause(
			final Map<String, JDBCParameterValue> params) {

		return new OrderByClause(this.resources, this.dialect,
				this.paramsFactory, this.order, this.singlePropExprs,
				this.collectionProps, this.allSingleJoins, params);
	}

	/**
	 * Build "SELECT" query that selects the records count. The query does not
	 * include any collection joins nor any non-collection joins that are
	 * optional and are not used in the filter.
	 *
	 * @param whereClause The "WHERE" clause to add, or {@code null} for no
	 * "WHERE" clause in the resulting query.
	 *
	 * @return SQL "SELECT COUNT" query text.
	 */
	String buildCountQuery(final WhereClause whereClause) {

		// create main query body
		final StringBuilder q = new StringBuilder(512);
		q.append("SELECT COUNT(*) FROM ").append(this.rootTableName)
			.append(" AS ").append(this.rootTableAlias);

		// add the filter
		if (whereClause != null) {

			// add the joins
			final Set<String> usedJoins = whereClause.getUsedJoins();
			for (final Map.Entry<String, String> entry :
					this.allSingleJoins.entrySet()) {
				if (usedJoins.contains(entry.getKey()))
					q.append(entry.getValue());
			}

			// add the "WHERE" clause
			q.append(" WHERE ").append(whereClause.getBody());
		}

		// return the query
		return q.toString();
	}

	/**
	 * Build "SELECT" query that selects top-level object record ids. The query
	 * does not include any collection joins nor any non-collection joins that
	 * are optional and are not used in the filter.
	 *
	 * @param whereClause The "WHERE" clause to add, or {@code null} for no
	 * "WHERE" clause in the resulting query.
	 * @param orderByClause The "ORDER BY" clause to add, or {@code null}
	 * for no additional "ORDER BY" in the resulting query.
	 *
	 * @return SQL "SELECT" query text.
	 */
	String buildIdsQuery(final WhereClause whereClause,
			final OrderByClause orderByClause) {

		// create main query body
		final StringBuilder q = new StringBuilder(512);
		q.append("SELECT ").append(this.rootTableAlias).append(".")
			.append(this.rootIdColName).append(" AS ").append(ANCHOR_ID_COLNAME)
			.append(" FROM ").append(this.rootTableName).append(" AS ")
			.append(this.rootTableAlias);

		// add the joins
		final Set<String> usedJoins = new HashSet<>();
		if (whereClause != null)
			usedJoins.addAll(whereClause.getUsedJoins());
		if (orderByClause != null)
			usedJoins.addAll(orderByClause.getUsedJoins());
		if (!usedJoins.isEmpty()) {
			for (final Map.Entry<String, String> entry :
					this.allSingleJoins.entrySet()) {
				if (usedJoins.contains(entry.getKey()))
					q.append(entry.getValue());
			}
		}

		// add the "WHERE" clause
		if (whereClause != null)
			q.append(" WHERE ").append(whereClause.getBody());

		// add the "ORDER BY" clause
		if (orderByClause != null)
			q.append(" ORDER BY ").append(orderByClause.getBody());

		// return the query
		return q.toString();
	}

	/**
	 * Build "SELECT" query that selects the data directly from the resource
	 * table.
	 *
	 * @param whereClause The "WHERE" clause to add, or {@code null} for no
	 * "WHERE" clause in the resulting query.
	 * @param orderByClause The "ORDER BY" clause to add, or {@code null}
	 * for no additional "ORDER BY" in the resulting query. The query may use
	 * "ORDER BY" with nested object ids to organize the result set for parsing.
	 * If additional clause is specified, it is prepended to the ids "ORDER BY".
	 *
	 * @return SQL "SELECT" query text.
	 */
	String buildDirectSelectQuery(final WhereClause whereClause,
			final OrderByClause orderByClause) {

		// create main query body
		final StringBuilder q = new StringBuilder(512);
		q.append("SELECT ").append(this.selectList).append(" FROM ")
			.append(this.rootTableName).append(" AS ")
			.append(this.rootTableAlias);

		// add single joins
		final Set<String> usedJoins = new HashSet<>();
		usedJoins.addAll(this.selectSingleJoins);
		if (whereClause != null)
			usedJoins.addAll(whereClause.getUsedJoins());
		if (orderByClause != null)
			usedJoins.addAll(orderByClause.getUsedJoins());
		if (!usedJoins.isEmpty()) {
			for (final Map.Entry<String, String> entry :
					this.allSingleJoins.entrySet()) {
				if (usedJoins.contains(entry.getKey()))
					q.append(entry.getValue());
			}
		}

		// add collection joins
		q.append(this.collectionJoins);

		// add the "WHERE" clause
		if (whereClause != null)
			q.append(" WHERE ").append(whereClause.getBody());

		// add "GROUP BY" clause
		if (!this.groupByList.isEmpty()) {
			q.append(" GROUP BY ").append(this.groupByList);
			;System.out.println(">>> APPENDING GROUP BY [" + this.groupByList + "], AGG WHERE: " + this.aggregationWhereClause);
		}

		// add the "ORDER BY" clause
		if (orderByClause != null) {
			q.append(" ORDER BY ").append(orderByClause.getBody());
			if (!this.orderByList.isEmpty())
				q.append(", ").append(this.orderByList);
		} else if (!this.orderByList.isEmpty()) {
			q.append(" ORDER BY ").append(this.orderByList);
		}

		// return the query
		return q.toString();
	}

	/**
	 * Build "SELECT" query that selects the data joining the resource table to
	 * an anchor table with resource record ids.
	 *
	 * @param anchorTableName Name of the anchor temporary table, or
	 * {@code null} for no anchor table.
	 * @param orderByClause The "ORDER BY" clause to add, or {@code null}
	 * for no additional "ORDER BY" in the resulting query. The query may use
	 * "ORDER BY" with nested object ids to organize the result set for parsing.
	 * If additional clause is specified, it is prepended to the ids "ORDER BY".
	 *
	 * @return SQL "SELECT" query text.
	 */
	String buildAnchoredSelectQuery(final String anchorTableName,
			final OrderByClause orderByClause) {

		// create main query body
		final StringBuilder q = new StringBuilder(512);
		q.append("SELECT ").append(this.selectList).append(" FROM ")
			.append(anchorTableName).append(" AS a INNER JOIN ")
			.append(this.rootTableName).append(" AS ")
			.append(this.rootTableAlias).append(" ON ")
			.append(this.rootTableAlias).append(".").append(this.rootIdColName)
			.append(" = a.").append(ANCHOR_ID_COLNAME);

		// add single joins
		final Set<String> usedJoins = new HashSet<>();
		usedJoins.addAll(this.selectSingleJoins);
		if (orderByClause != null)
			usedJoins.addAll(orderByClause.getUsedJoins());
		if (!usedJoins.isEmpty()) {
			for (final Map.Entry<String, String> entry :
					this.allSingleJoins.entrySet()) {
				if (usedJoins.contains(entry.getKey()))
					q.append(entry.getValue());
			}
		}

		// add collection joins
		q.append(this.collectionJoins);

		// add "GROUP BY" clause
		if (!this.groupByList.isEmpty()) {
			q.append(" GROUP BY ").append(this.groupByList);
			;System.out.println(">>> APPENDING GROUP BY [" + this.groupByList + "], AGG WHERE: " + this.aggregationWhereClause);
		}

		// add the "ORDER BY" clause
		if (orderByClause != null) {
			q.append(" ORDER BY ").append(orderByClause.getBody());
			if (!this.orderByList.isEmpty())
				q.append(", ").append(this.orderByList);
		} else if (!this.orderByList.isEmpty()) {
			q.append(" ORDER BY ").append(this.orderByList);
		}

		// return the query
		return q.toString();
	}
}
