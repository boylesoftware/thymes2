package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.DependentAggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.RefsFetchSpec;
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
		final PropertiesFetchSpec<?> propsFetch;

		/**
		 * References fetch specification.
		 */
		final RefsFetchSpec<?> refsFetch;

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
		final boolean forceOuter;

		/**
		 * Select list for single-valued properties.
		 */
		final StringBuilder singlesSelectList = new StringBuilder(512);

		/**
		 * Select list for collection properties.
		 */
		final StringBuilder collectionsSelectList = new StringBuilder(512);

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
		 * Order by list.
		 */
		final StringBuilder orderByList = new StringBuilder(64);

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


		/**
		 * Create new context.
		 *
		 * @param parentCtx Parent context, or {@code null} for the seed.
		 * @param resources Application resources manager.
		 * @param dialect SQL dialect.
		 * @param paramsFactory Query parameter value handlers factory.
		 * @param actor Data consumer.
		 * @param propsFetch Properties fetch specification.
		 * @param refsFetch References fetch specification.
		 * @param filter Filter specification.
		 * @param order Order specification.
		 * @param parentPropPath Parent property path.
		 * @param parentPropChain Parent property chain.
		 * @param rootTableName Query root table name.
		 * @param rootTableAlias Query root table alias.
		 * @param parentAnchorColExpr Parent anchor column expression.
		 * @param parentJoinExpr Full parent join expression.
		 * @param colLabelPrefix Column labels prefix.
		 * @param rootIdColName Record id column name. May be parent record id
		 * column name if the query root object does not have its own id. May be
		 * {@code null} for the seed context.
		 * @param rootIdColExpr Record id column expression. In contrast to
		 * {@link #rootIdColName}, {@code null} if the query root object does
		 * not have its own id.
		 * @param forceOuter {@code true} if outer joins must be forced.
		 */
		QueryBuilderContext(final QueryBuilderContext parentCtx,
				final Resources resources, final SQLDialect dialect,
				final ParameterValuesFactoryImpl paramsFactory,
				final Actor actor, final PropertiesFetchSpec<?> propsFetch,
				final RefsFetchSpec<?> refsFetch, final FilterSpec<?> filter,
				final OrderSpec<?> order, final String parentPropPath,
				final List<ResourcePropertyHandler> parentPropChain,
				final String rootTableName, final String rootTableAlias,
				final String parentAnchorColExpr, final String parentJoinExpr,
				final String colLabelPrefix, final String rootIdColName,
				final String rootIdColExpr, final boolean forceOuter) {

			this.parentCtx = parentCtx;
			this.resources = resources;
			this.dialect = dialect;
			this.paramsFactory = paramsFactory;
			this.actor = actor;
			this.propsFetch = propsFetch;
			this.refsFetch = refsFetch;
			this.filter = filter;
			this.order = order;
			this.parentPropPath = parentPropPath;
			this.parentPropChain = parentPropChain;
			this.rootTableName = rootTableName;
			this.rootTableAlias = rootTableAlias;
			this.parentAnchorColExpr = parentAnchorColExpr;
			this.parentJoinExpr = parentJoinExpr;
			this.colLabelPrefix = colLabelPrefix;
			this.rootIdColName = rootIdColName;
			this.rootIdColExpr = rootIdColExpr;
			this.forceOuter = forceOuter;

			this.propChain =
				new ArrayList<>(this.parentPropChain.size() + 1);
			this.propChain.addAll(this.parentPropChain);
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
				"t" + this.propColLabelPrefix.replace("$", "");
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
			final String collectionJoins, final String orderByList,
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
	 * @param refsFetch References fetch specification, or {@code null}.
	 * @param filter Filter specification, or {@code null}.
	 * @param order Order specification, or {@code null}.
	 *
	 * @return The query builder.
	 */
	static QueryBuilder createQueryBuilder(final Resources resources,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory, final Actor actor,
			final PersistentResourceHandler<?> prsrcHandler,
			final PropertiesFetchSpec<?> propsFetch,
			final RefsFetchSpec<?> refsFetch, final FilterSpec<?> filter,
			final OrderSpec<?> order) {

		final QueryBuilderContext seedCtx = new QueryBuilderContext(null,
				resources, dialect, paramsFactory, actor, propsFetch, refsFetch,
				filter, order, null,
				Collections.<ResourcePropertyHandler>emptyList(), null, null,
				null, null, null, null, null, false);

		final QueryBranch b = createBranch(seedCtx, "", false, null, null, null,
				null, null, "",
				prsrcHandler, prsrcHandler.getPersistentCollectionName(), "t",
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
		final boolean forceOuter = (parentCtx.forceOuter || optional);

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

		// create query builder context
		final QueryBuilderContext ctx = new QueryBuilderContext(parentCtx,
				parentCtx.resources, parentCtx.dialect, parentCtx.paramsFactory,
				parentCtx.actor, parentCtx.propsFetch, parentCtx.refsFetch,
				parentCtx.filter, parentCtx.order, parentPropPath,
				parentCtx.propChain, tableName, tableAlias, anchorColExpr,
				joinExpr, colLabelPrefix, idColName,
				(idPropHandler == null ? null :
					tableAlias + "."
						+ idPropHandler.getPersistence().getFieldName()
						+ " AS " + parentCtx.dialect.quoteColumnLabel(
								colLabelPrefix + idPropHandler.getName())),
				forceOuter);

		// determine if polymorphic object
		final boolean polymorphic =
			((container instanceof ObjectPropertyHandler)
					&& (((ObjectPropertyHandler) container).getTypeProperty()
							!= null));

		// add record id to the select list and the properties
		if ((idPropHandler != null) && !polymorphic) {
			ctx.singlesSelectList.append(ctx.rootIdColExpr);
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
				final String metaValExpr = ctx.rootTableAlias + "."
						+ metaHandler.getPersistence().getFieldName();
				ctx.singlesSelectList.append(", ").append(metaValExpr)
					.append(" AS ").append(ctx.dialect.quoteColumnLabel(
							colLabelPrefix + metaHandler.getName()));
				ctx.singlePropExprs.put(
						ctx.getPropertyPath(metaHandler.getName()),
						new SingleValuedQueryProperty(metaValExpr,
								metaHandler.getValueHandler()
									.getPersistentValueType()));
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

		// add dependent resource properties
		if (prsrcHandler != null) {

			// add dependent resource reference properties
			for (final DependentRefPropertyHandler propHandler :
					prsrcHandler.getDependentRefProperties())
				addDependentRefProperty(ctx, propHandler);

			// add dependent resource aggregate properties
			for (final DependentAggregatePropertyHandler propHandler :
					prsrcHandler.getDependentAggregateProperties())
				addDependentAggregateProperty(ctx, propHandler);
		}

		// merge the select list
		final StringBuilder selectList = new StringBuilder(512);
		selectList.append(ctx.singlesSelectList);
		if (ctx.collectionsSelectList.length() > 0) {
			if (selectList.length() > 0)
				selectList.append(", ");
			selectList.append(ctx.collectionsSelectList);
		}

		// order by record id if has collections
		if (ctx.collectionJoins.length() > 0) {
			if (ctx.orderByList.length() > 0)
				ctx.orderByList.insert(0,
						ctx.rootTableAlias + "." + ctx.rootIdColName + ", ");
			else
				ctx.orderByList.append(ctx.rootTableAlias).append(".")
					.append(ctx.rootIdColName);
		}

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
				selectList.toString(),
				ctx.selectSingleJoins,
				ctx.allSingleJoins,
				ctx.collectionJoins.toString(),
				ctx.orderByList.toString(),
				ctx.singlePropExprs,
				ctx.collectionProps,
				ctx.branches);
		final QueryBranch resBranch = new QueryBranch(resQB, anchorColExpr,
				joinExpr, parentPropPath, parentCtx.propChain);

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
	 *
	 * @return The branch.
	 */
	private static QueryBranch createBranch(final QueryBuilderContext ctx,
			final String propPath, final String propTableName,
			final String propIdColName, final String propValExpr,
			final boolean optional, final String propAnchorColExpr,
			final String propJoinCondition) {

		final String propJoinExpr =
				(optional || ctx.forceOuter ?
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
						Collections.<String, SingleValuedQueryProperty>
						emptyMap(),
						Collections.<String, CollectionQueryProperty>emptyMap(),
						Collections.<QueryBranch>emptyList()),
				propAnchorColExpr,
				propJoinExpr,
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
					isSelected(ctx,
							ctx.getPropertyPath(propHandler.getName())));
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
		if (!isSelected(ctx, propPath))
			return;

		// check if adding to existing list
		if (ctx.singlesSelectList.length() > 0)
			ctx.singlesSelectList.append(", ");

		// add property to the list
		ctx.singlesSelectList.append(propValExpr).append(" AS ")
			.append(ctx.dialect.quoteColumnLabel(
					ctx.colLabelPrefix + propHandler.getName()));
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
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propPersistence.getParentIdFieldName()
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
				propAnchorColExpr, propJoinCondition);
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
					isSelected(ctx, propPath));
		else if (propHandler.isSingleValued()) // single-valued
			addBranch(ctx, createSingleObjectPropertyBranch(ctx,
					propHandler, propPersistence, propPath, propTableName),
					isSelected(ctx, propPath));
		else // not embedded collection
			addBranch(ctx, createCollectionObjectPropertyBranch(ctx,
					propHandler, propPersistence, propPath, propTableName),
					isSelected(ctx, propPath));

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
		if ((ctx.refsFetch != null)
				&& ctx.refsFetch.isFetchRequested(propPath)) {

			// single of collection?
			if (propHandler.isSingleValued())
				addBranch(ctx, createSingleFetchedRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), isSelected(ctx, propPath));
			else // collection
				addBranch(ctx, createCollectionFetchedRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), isSelected(ctx, propPath));

		} else { // no fetch requested

			// single of collection?
			if (propHandler.isSingleValued())
				addSingleRefProperty(ctx, propHandler, propPersistence,
						propPath, refTargetClass);
			else // collection
				addBranch(ctx, createCollectionRefPropertyBranch(ctx,
						propHandler, propPersistence, refTargetClass),
						isSelected(ctx, propPath));
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

		// create anchor column expression
		final String propAnchorColExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
					propValExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// add to select list if selected
		if (isSelected(ctx, propPath)) {

			// check if adding to existing list
			if (ctx.singlesSelectList.length() > 0)
				ctx.singlesSelectList.append(", ");

			// add property to the list
			ctx.singlesSelectList.append(propAnchorColExpr);
		}

		// check if used
		if (!isUsed(ctx, propPath))
			return;

		// create join expression
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		final String propJoinCondition =
			ctx.propTableAlias + "." + propIdColName
				+ " = " + ctx.rootTableAlias + "."
					+ propPersistence.getFieldName();

		// create property branch
		addBranch(ctx, createBranch(ctx, propPath, propPersistence.isOptional(),
				propAnchorColExpr, propJoinCondition, null, null, null,
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
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propPersistence.getParentIdFieldName()
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
				propAnchorColExpr, propJoinCondition);
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
				+ " = " + ctx.rootTableAlias + "."
					+ propPersistence.getFieldName();

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
		final String linkJoinCondition = linkTableAlias + "."
				+ propPersistence.getParentIdFieldName() + " = "
				+ ctx.rootTableAlias + "." + ctx.rootIdColName;
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
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propIdColName + " = " + propValueExpr;

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
		if ((ctx.refsFetch != null)
				&& ctx.refsFetch.isFetchRequested(propPath)) {

			// single of collection?
			if (propHandler.isSingleValued())
				addBranch(ctx, createSingleFetchedDependentRefPropertyBranch(
						ctx, propHandler, propPersistence, propPath,
						refTargetClass), isSelected(ctx, propPath));
			else // collection
				addBranch(ctx,
						createCollectionFetchedDependentRefPropertyBranch(ctx,
								propHandler, propPersistence, propPath,
								refTargetClass), isSelected(ctx, propPath));

		} else { // no fetch requested

			// single of collection?
			if (propHandler.isSingleValued())
				addSingleDependentRefProperty(ctx, propHandler, propPersistence,
						propPath, refTargetClass);
			else // collection
				addBranch(ctx, createCollectionDependentRefPropertyBranch(ctx,
						propHandler, propPersistence, propPath,
						refTargetClass), isSelected(ctx, propPath));
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

		// create anchor column expression
		final String propAnchorColExpr =
			ctx.dialect.nullableConcat(refTargetClass.getSimpleName() + "#",
					propValExpr)
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// create join expression
		final String propJoinCondition =
			ctx.propTableAlias + "." + propPersistence.getParentIdFieldName()
				+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;

		// add to select list if selected
		if (isSelected(ctx, propPath)) {

			// add the join (will be created by the branch)
			ctx.selectSingleJoins.add(propPath);

			// check if adding to existing list
			if (ctx.singlesSelectList.length() > 0)
				ctx.singlesSelectList.append(", ");

			// add property to the list
			ctx.singlesSelectList.append(propAnchorColExpr);
		}

		// create branch
		final QueryBranch branch = createBranch(ctx, propPath,
				propPersistence.isOptional(), propAnchorColExpr,
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
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propPersistence.getParentIdFieldName()
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
				propAnchorColExpr, propJoinCondition);
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
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propPersistence.getParentIdFieldName()
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
	 * Add dependent resource aggregate property.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 */
	private static void addDependentAggregateProperty(
			final QueryBuilderContext ctx,
			final DependentAggregatePropertyHandler propHandler) {

		// get property persistence
		final ResourcePropertyPersistence propPersistence =
			getPropertyPersistence(ctx, propHandler);
		if (propPersistence == null)
			return;

		// construct property path
		final String propPath = ctx.getPropertyPath(propHandler.getName());

		// check if included in the fetch
		if ((ctx.propsFetch == null) || !ctx.propsFetch.isIncluded(propPath))
			return;

		// get reference target
		final Class<?> refTargetClass = propHandler.getReferredResourceClass();

		// TODO: verify that other dependent resource properties that refer to
		//       the same resource and are not aggregates are not included and
		//       are not fetched.

		// add the property
		addBranch(ctx,
				createDependentAggregatePropertyBranch(ctx, propHandler,
						propPersistence, propPath, refTargetClass),
				isSelected(ctx, propPath));
	}

	/**
	 * Create dependent resource aggregate property branch.
	 *
	 * @param ctx Query builder context.
	 * @param propHandler Property handler.
	 * @param propPersistence Property persistence descriptor.
	 * @param propPath Property path.
	 * @param refTargetClass Reference target class.
	 *
	 * @return The branch.
	 */
	private static QueryBranch createDependentAggregatePropertyBranch(
			final QueryBuilderContext ctx,
			final DependentAggregatePropertyHandler propHandler,
			final ResourcePropertyPersistence propPersistence,
			final String propPath, final Class<?> refTargetClass) {

		// get target resource handler
		final PersistentResourceHandler<?> refTargetHandler =
			ctx.resources.getPersistentResourceHandler(refTargetClass);

		// column label prefix and property table alias
		ctx.prepareNestedProperty(propHandler);

		// property value expression
		final String aggregationPropName =
			propHandler.getAggregationPropertyName();
		final String aggregationFieldName = (aggregationPropName == null ? null
				: refTargetHandler.getProperties().get(aggregationPropName)
					.getPersistence().getFieldName());
		final String func;
		switch (propHandler.getFunction()) {
		case COUNT:
			func = "COUNT(";
			break;
		case COUNT_DISTINCT:
			func = "COUNT(DISTINCT ";
			break;
		case SUM:
			func = "SUM(";
			break;
		case MAX:
			func = "MAX(";
			break;
		case MIN:
			func = "MIN(";
			break;
		default: // AVG
			func = "AVG(";
		}
		final String propValueExpr = func
				+ (aggregationFieldName == null ? "*" :
					ctx.propTableAlias + "." + aggregationFieldName)
				+ ")";

		// add collection property stump
		final String propTableName = propPersistence.getCollectionName();
		final String propJoinCondition = ctx.propTableAlias + "."
				+ propPersistence.getParentIdFieldName()
				+ " = " + ctx.rootTableAlias + "." + ctx.rootIdColName;
		final String propIdColName =
			refTargetHandler.getIdProperty().getPersistence().getFieldName();
		ctx.collectionProps.put(propPath,
				new CollectionQueryProperty(propHandler,
						propTableName, ctx.propTableAlias,
						propJoinCondition, null,
						ctx.propTableAlias + "." + propIdColName));

		// create anchor column expression
		final String propAnchorColExpr =
			propValueExpr
				+ " AS " + ctx.dialect.quoteColumnLabel(
						ctx.colLabelPrefix + propHandler.getName());

		// create property branch
		return createBranch(ctx, propPath, propTableName, propIdColName, "",
				propPersistence.isOptional(), propAnchorColExpr,
				propJoinCondition);
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
	 * Tell if the specified property needs to be selected.
	 *
	 * @param ctx Builder context.
	 * @param propPath Property path.
	 *
	 * @return {@code true} if selected.
	 */
	private static boolean isSelected(final QueryBuilderContext ctx,
			final String propPath) {

		return ((ctx.propsFetch == null)
				|| ctx.propsFetch.isIncluded(propPath));
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

		// check if collection and is not selected
		final boolean collection = branch.isCollection();
		if (collection && !selected)
			return;

		// decide whether to merge or attach
		if (!collection) {
			final QueryBuilder branchQB = branch.getQueryBuilder();
			if (!branchQB.hasCollections())
				mergeBranch(ctx, branch, selected);
			else if (ctx.collectionJoins.length() > 0)
				attachBranch(ctx, branch, selected);
			else
				mergeBranch(ctx, branch, selected);
		} else { // selected collection
			if (ctx.collectionJoins.length() > 0)
				attachBranch(ctx, branch, selected);
			else
				mergeBranch(ctx, branch, selected);
		}
	}

	/**
	 * Merge branch to the query context.
	 *
	 * @param ctx The context, to which to merge the branch.
	 * @param branch The branch.
	 * @param selected {@code true} if branch property is selected.
	 */
	private static void mergeBranch(final QueryBuilderContext ctx,
			final QueryBranch branch, final boolean selected) {

		// get branch query builder
		final QueryBuilder branchQB = branch.getQueryBuilder();

		// analyze the branch collection status
		final boolean collection = branch.isCollection();
		final boolean hasCollections = branchQB.hasCollections();

		// merge select list if selected
		if (selected) {

			// determine to which select list to add the branch
			final StringBuilder selectList = (collection || hasCollections ?
					ctx.collectionsSelectList : ctx.singlesSelectList);

			// add anchor column
			if (selectList.length() > 0)
				selectList.append(", ");
			selectList.append(branch.getAnchorColumnExpression());

			// add branch select list
			if (!branchQB.selectList.isEmpty())
				selectList.append(", ").append(branchQB.selectList);
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

		// merge properties
		if (!collection) {
			ctx.singlePropExprs.putAll(branchQB.singlePropExprs);
			ctx.collectionProps.putAll(branchQB.collectionProps);
		}

		// merge order by list
		if (selected) {
			if (!branchQB.orderByList.isEmpty()) {
				if (ctx.orderByList.length() > 0)
					ctx.orderByList.append(", ");
				ctx.orderByList.append(branchQB.orderByList);
			}
		}

		// add branches
		for (final QueryBranch subBranch : branchQB.branches)
			addBranch(ctx, subBranch, selected);
	}

	/**
	 * Attach branch to the query context.
	 *
	 * @param ctx The context, to which to merge the branch.
	 * @param branch The branch.
	 * @param selected {@code true} if branch property is selected.
	 */
	private static void attachBranch(final QueryBuilderContext ctx,
			final QueryBranch branch, final boolean selected) {

		// get branch query builder
		final QueryBuilder branchQB = branch.getQueryBuilder();

		// get the branch collection status
		final boolean collection = branch.isCollection();

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
						+ branch.getAnchorColumnExpression()
						+ (branchQB.selectList.isEmpty() ? "" :
								", " + branchQB.selectList),
					rebranchedSelectSingleJoins,
					rebranchedAllSingleJoins,
					rebranchedCollectionJoins.toString(),
					rebranchedOrderByList.toString(),
					Collections.<String, SingleValuedQueryProperty>emptyMap(),
					Collections.<String, CollectionQueryProperty>emptyMap(),
					Collections.<QueryBranch>emptyList());

			// create re-branched branch and add it to the context
			ctx.branches.add(new QueryBranch(
					rebranchedQB,
					ctx.parentAnchorColExpr,
					ctx.parentJoinExpr,
					branchPropPath,
					branch.getAttachmentChain()));
		}

		// attach branch branches
		for (final QueryBranch b : branchQB.branches)
			attachBranch(ctx, b, selected);
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
				this.filter, this.singlePropExprs, this.collectionProps,
				this.allSingleJoins, params);
	}

	/**
	 * Build "ORDER BY" clause for the query.
	 *
	 * @return Object representing the SQL "ORDER BY" clause.
	 */
	OrderByClause buildOrderByClause() {

		return new OrderByClause(this.order, this.singlePropExprs,
				this.allSingleJoins);
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
