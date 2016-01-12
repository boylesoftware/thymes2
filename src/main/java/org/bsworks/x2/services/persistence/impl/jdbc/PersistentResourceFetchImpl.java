package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceFetchResult;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.LockType;
import org.bsworks.x2.services.persistence.PersistentResourceFetch;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Persistent resource fetch implementation.
 *
 * @param <R> Persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class PersistentResourceFetchImpl<R>
	implements PersistentResourceFetch<R> {

	/**
	 * Main query with an optional count query.
	 *
	 * @param <R> Persistent resource type.
	 */
	private static final class MainQuery<R> {

		/**
		 * Statements to execute before the query.
		 */
		final List<PersistenceUpdateImpl> preStatements;

		/**
		 * Statements to execute after the query.
		 */
		final List<PersistenceUpdateImpl> postStatements;

		/**
		 * Query for selecting the data.
		 */
		final ResourcePersistenceQueryImpl<R> dataQuery;

		/**
		 * Query for selecting the records count, or {@code null}.
		 */
		final SimpleValuePersistenceQueryImpl<Long> countQuery;


		/**
		 * Create new queries couple.
		 *
		 * @param preStatements Statements to execute before the query, may be
		 * {@code null}.
		 * @param postStatements Statements to execute after the query, may be
		 * {@code null}.
		 * @param dataQuery Query for selecting the data.
		 * @param countQuery Query for selecting the records count, or
		 * {@code null}.
		 */
		MainQuery(final List<PersistenceUpdateImpl> preStatements,
				final List<PersistenceUpdateImpl> postStatements,
				final ResourcePersistenceQueryImpl<R> dataQuery,
				final SimpleValuePersistenceQueryImpl<Long> countQuery) {

			this.preStatements = preStatements;
			this.postStatements = postStatements;
			this.dataQuery = dataQuery;
			this.countQuery = countQuery;
		}
	}


	/**
	 * Application resources manager.
	 */
	private final Resources resources;

	/**
	 * The transaction.
	 */
	private final JDBCPersistenceTransaction tx;

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandler<R> prsrcHandler;

	/**
	 * Properties fetch specification, or {@code null} for default.
	 */
	private PropertiesFetchSpec<R> propsFetch = null;

	/**
	 * Filter, or {@code null} for no filter.
	 */
	private FilterSpec<R> filter = null;

	/**
	 * Order, or {@code null} for unspecified order.
	 */
	private OrderSpec<R> order = null;

	/**
	 * Range, or {@code null} for no range.
	 */
	private RangeSpec range = null;

	/**
	 Lock type for the result, or {@code null} if none requested.
	 */
	private LockType lockType = null;

	/**
	 * Name of the temporary table used to anchor multi-query fetches.
	 */
	private final String anchorTableName;


	/**
	 * Create new fetch.
	 *
	 * @param resources Application resources manager.
	 * @param tx The transaction.
	 * @param prsrcClass Persistent resource class.
	 */
	PersistentResourceFetchImpl(final Resources resources,
			final JDBCPersistenceTransaction tx, final Class<R> prsrcClass) {

		this.resources = resources;
		this.tx = tx;
		this.prsrcHandler = resources.getPersistentResourceHandler(prsrcClass);

		this.anchorTableName =
			"q_" + this.prsrcHandler.getPersistentCollectionName();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetch<R> setPropertiesFetch(
			final PropertiesFetchSpec<R> propsFetch) {

		this.propsFetch = propsFetch;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetch<R> setFilter(final FilterSpec<R> filter) {

		this.filter = filter;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetch<R> setOrder(final OrderSpec<R> order) {

		this.order = order;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetch<R> setRange(final RangeSpec range) {

		this.range = range;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetch<R> lockResult(final LockType lockType) {

		this.lockType = lockType;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public long getCount() {

		// special properties fetch specification
		final PropertiesFetchSpec<R> propsFetch = this.resources
				.getPropertiesFetchSpec(this.prsrcHandler.getResourceClass())
				.includeSuperAggregate(
						PersistentResourceFetchResult.TOTAL_COUNT_PROP);

		// build the query
		final QueryBuilder qb = buildQuery(
				this.resources, this.tx, this.prsrcHandler,
				propsFetch, this.filter, null);

		// get the main query
		final MainQuery<R> mainQuery = getMainQuery(
				this.resources, this.tx, this.prsrcHandler,
				qb, propsFetch, this.filter, null, null,
				this.lockType, this.anchorTableName);

		// execute pre-statements
		if (mainQuery.preStatements != null)
			for (final PersistenceUpdateImpl stmt : mainQuery.preStatements)
				stmt.execute();
		try {

			// return the records count
			return mainQuery.countQuery.getFirstResult(null).longValue();

		} finally {

			// execute post-statements
			if (mainQuery.postStatements != null)
				for (final PersistenceUpdateImpl stmt :
						mainQuery.postStatements)
					stmt.execute();
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceFetchResult<R> getResult() {

		// build the query
		final QueryBuilder qb = buildQuery(
				this.resources, this.tx, this.prsrcHandler,
				this.propsFetch, this.filter, this.order);

		// references fetch result map and the count
		final Map<String, Object> refsFetchResultMap =
			this.getRefsFetchResultMap();
		Long totalCount = null;

		// get the main query
		final MainQuery<R> mainQuery = getMainQuery(
				this.resources, this.tx, this.prsrcHandler,
				qb, this.propsFetch, this.filter, this.order, this.range,
				this.lockType, this.anchorTableName);

		// execute pre-statements
		if (mainQuery.preStatements != null)
			for (final PersistenceUpdateImpl stmt : mainQuery.preStatements)
				stmt.execute();
		try {

			// get the records count if requested
			if (mainQuery.countQuery != null)
				totalCount = mainQuery.countQuery.getFirstResult(null);

			// execute the main query
			final List<R> mainResult =
				mainQuery.dataQuery.getResultList(refsFetchResultMap);

			// execute branch queries
			if (!mainResult.isEmpty()) {
				for (final QueryBranch branch : qb.getBranches()) {
					mainQuery.dataQuery.setQueryText(
							branch.getQueryBuilder().buildAnchoredSelectQuery(
									this.anchorTableName, null))
						.getResultList(refsFetchResultMap);
				}
			}

			// build the result
			final PersistentResourceFetchResult<R> res =
				this.prsrcHandler.newFetchResult(mainResult,
						refsFetchResultMap);
			res.setTotalCount(totalCount);

			// return the result
			return res;

		} finally {

			// execute post-statements
			if (mainQuery.postStatements != null)
				for (final PersistenceUpdateImpl stmt :
						mainQuery.postStatements)
					stmt.execute();
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <C extends PersistentResourceFetchResult<R>> C getResult(
			final Class<C> fetchResultClass) {

		return fetchResultClass.cast(this.getResult());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public R getSingleResult() {

		// special properties fetch specification
		final PropertiesFetchSpec<R> propsFetch = this.propsFetch;
		final PropertiesFetchSpec<R> propsFetchToUse;
		if (propsFetch == null)
			propsFetchToUse = null;
		else
			propsFetchToUse = new PropertiesFetchSpec<R>() {
				@Override
				public Class<R> getPersistentResourceClass() {
					return propsFetch.getPersistentResourceClass();
				}
				@Override
				public boolean isIncluded(final String propPath) {
					return propsFetch.isIncluded(propPath);
				}
				@Override
				public boolean isFetchRequested(final String propPath) {
					return false;
				}
				@Override
				public SortedMap<String, Class<?>> getFetchedRefProperties() {
					return Collections.emptySortedMap();
				}
				@Override
				public FilterSpec<R> getAggregateFilter(final String propPath) {
					return propsFetch.getAggregateFilter(propPath);
				}
				@Override
				public boolean isSuperAggregateIncluded(final String propName) {
					return false;
				}
			};

		// build the query
		final QueryBuilder qb = buildQuery(
				this.resources, this.tx, this.prsrcHandler,
				propsFetchToUse, this.filter, this.order);

		// get the main query
		final MainQuery<R> mainQuery = getMainQuery(
				this.resources, this.tx, this.prsrcHandler,
				qb, propsFetchToUse, this.filter, this.order,
					new RangeSpec(0, 1),
				this.lockType, this.anchorTableName);

		// execute pre-statements
		if (mainQuery.preStatements != null)
			for (final PersistenceUpdateImpl stmt : mainQuery.preStatements)
				stmt.execute();
		try {

			// execute the main query
			final R mainResult = mainQuery.dataQuery.getFirstResult(null);

			// execute branch queries
			if (mainResult != null) {
				for (final QueryBranch branch : qb.getBranches()) {
					mainQuery.dataQuery.setQueryText(
							branch.getQueryBuilder().buildAnchoredSelectQuery(
									this.anchorTableName, null))
						.getFirstResult(null);
				}
			}

			// return the result
			return mainResult;

		} finally {

			// execute post-statements
			if (mainQuery.postStatements != null)
				for (final PersistenceUpdateImpl stmt :
						mainQuery.postStatements)
					stmt.execute();
		}
	}

	/**
	 * Get map for the references fetch result.
	 *
	 * @return The references fetch result map, or {@code null} if not required.
	 */
	private Map<String, Object> getRefsFetchResultMap() {

		if ((this.propsFetch == null)
				|| this.propsFetch.getFetchedRefProperties().isEmpty())
			return null;

		return new HashMap<>();
	}

	/**
	 * Build the query.
	 *
	 * @param resources Resources.
	 * @param tx Transaction.
	 * @param prsrcHandler Persistent resource handler.
	 * @param propsFetch Properties fetch specification.
	 * @param filter Filter specification.
	 * @param order Order specification.
	 *
	 * @return The top query builder.
	 */
	private static <R> QueryBuilder buildQuery(
			final Resources resources,
			final JDBCPersistenceTransaction tx,
			final PersistentResourceHandler<R> prsrcHandler,
			final PropertiesFetchSpec<R> propsFetch,
			final FilterSpec<R> filter,
			final OrderSpec<R> order) {

		return QueryBuilder.createQueryBuilder(resources, tx.getSQLDialect(),
				tx.getParameterValuesFactory(), tx.getActor(), prsrcHandler,
				propsFetch, filter, order);
	}

	/**
	 * Build, prepare and return the main query.
	 *
	 * @param resources Resources.
	 * @param tx Transaction.
	 * @param prsrcHandler Persistent resource handler.
	 * @param qb Top query builder.
	 * @param propsFetch Properties fetch specification.
	 * @param filter Filter specification.
	 * @param order Order specification.
	 * @param range Range specification.
	 * @param lockType Lock type.
	 * @param anchorTableName Anchor table name.
	 *
	 * @return The main query.
	 */
	private static <R> MainQuery<R> getMainQuery(
			final Resources resources,
			final JDBCPersistenceTransaction tx,
			final PersistentResourceHandler<R> prsrcHandler,
			final QueryBuilder qb,
			final PropertiesFetchSpec<R> propsFetch,
			final FilterSpec<R> filter,
			final OrderSpec<R> order,
			final RangeSpec range,
			final LockType lockType,
			final String anchorTableName) {

		// the queries, the pre- and post- statements, the parameters
		final List<String> preStmtTexts = new ArrayList<>();
		final List<String> postStmtTexts = new ArrayList<>();
		final String countQueryText;
		final String dataQueryText;
		final Map<String, JDBCParameterValue> params = new HashMap<>();

		// get aggregation parameters from the query builder
		params.putAll(qb.getAggregationParams());

		// needed objects from the transaction
		final SQLDialect dialect = tx.getSQLDialect();
		final ParameterValuesFactoryImpl paramsFactory =
			tx.getParameterValuesFactory();

		// get "WHERE" and "ORDER BY" clauses
		final WhereClause whereClause = (
				(filter == null) || filter.isEmpty() ? null :
					qb.buildWhereClause(params));
		final OrderByClause orderByClause =
				(order == null ? null :
					qb.buildOrderByClause(params));

		// check if no branches
		if (qb.getBranches().isEmpty()) {

			// check if no range
			if (range == null) {

				// direct select
				final String q =
					qb.buildDirectSelectQuery(whereClause, orderByClause);
				if (lockType == null)
					dataQueryText = q;
				else switch (lockType) {
				case SHARED:
					dataQueryText = dialect.makeSelectWithShareLock(
							q, qb.getRootTableAlias());
					break;
				case EXCLUSIVE:
					dataQueryText = dialect.makeSelectWithExclusiveLock(
							q, qb.getRootTableAlias());
					break;
				default: // cannot happen
					throw new AssertionError("Unknown lock type.");
				}

			} else { // ranged query

				// check if no collections
				if (!qb.hasCollections()) {

					// ranged direct select
					final String q = dialect.makeRangedSelect(
							qb.buildDirectSelectQuery(whereClause,
									orderByClause),
							range, paramsFactory, params);
					if (lockType == null)
						dataQueryText = q;
					else switch (lockType) {
					case SHARED:
						dataQueryText = dialect.makeSelectWithShareLock(
								q, qb.getRootTableAlias());
						break;
					case EXCLUSIVE:
						dataQueryText =
							dialect.makeSelectWithExclusiveLock(
									q, qb.getRootTableAlias());
						break;
					default: // cannot happen
						throw new RuntimeException("Unknown lock type.");
					}

				} else { // ranged and has collections

					// create anchor table
					final String q = dialect.makeRangedSelect(
							qb.buildIdsQuery(whereClause, orderByClause),
							range, paramsFactory, params);
					dialect.makeSelectIntoTempTable(
							anchorTableName,
							tx.addTempTable(anchorTableName),
							(lockType == LockType.EXCLUSIVE
									? dialect.makeSelectWithExclusiveLock(
											q, qb.getRootTableAlias())
									: dialect.makeSelectWithShareLock(
											q, qb.getRootTableAlias())),
							preStmtTexts, postStmtTexts);

					// anchored select
					dataQueryText =
						qb.buildAnchoredSelectQuery(anchorTableName,
								orderByClause);
				}
			}

		} else { // branched query

			// check if no range
			if (range == null) {

				// create anchor table
				final String q = qb.buildIdsQuery(whereClause, null);
				dialect.makeSelectIntoTempTable(
						anchorTableName,
						tx.addTempTable(anchorTableName),
						(lockType == LockType.EXCLUSIVE
								? dialect.makeSelectWithExclusiveLock(
										q, qb.getRootTableAlias())
								: dialect.makeSelectWithShareLock(
										q, qb.getRootTableAlias())),
						preStmtTexts, postStmtTexts);

			} else {  // ranged query

				// create anchor table
				final String q = dialect.makeRangedSelect(
						qb.buildIdsQuery(whereClause, orderByClause),
						range, paramsFactory, params);
				dialect.makeSelectIntoTempTable(
						anchorTableName,
						tx.addTempTable(anchorTableName),
						(lockType == LockType.EXCLUSIVE
								? dialect.makeSelectWithExclusiveLock(
										q, qb.getRootTableAlias())
								: dialect.makeSelectWithShareLock(
										q, qb.getRootTableAlias())),
						preStmtTexts, postStmtTexts);
			}

			// anchored select
			dataQueryText =
				qb.buildAnchoredSelectQuery(anchorTableName, orderByClause);
		}

		// prepare pre- and post- statements
		final List<PersistenceUpdateImpl> preStmts;
		if (!preStmtTexts.isEmpty()) {
			preStmts = new ArrayList<>(preStmtTexts.size());
			for (final String stmtText : preStmtTexts)
				preStmts.add(new PersistenceUpdateImpl(resources, tx,
						stmtText, params));
		} else {
			preStmts = null;
		}
		final List<PersistenceUpdateImpl> postStmts;
		if (!postStmtTexts.isEmpty()) {
			postStmts = new ArrayList<>(postStmtTexts.size());
			for (final String stmtText : postStmtTexts)
				postStmts.add(new PersistenceUpdateImpl(resources, tx,
						stmtText, params));
		} else {
			postStmts = null;
		}

		// create the query
		final ResourcePersistenceQueryImpl<R> query =
			new ResourcePersistenceQueryImpl<>(resources, tx,
					dataQueryText, prsrcHandler.getResourceClass(),
					params);
		query.setSessionCache(new ResourceReadSessionCache());

		// create records count query
		if ((propsFetch != null) && propsFetch.isSuperAggregateIncluded(
				PersistentResourceFetchResult.TOTAL_COUNT_PROP))
			countQueryText = qb.buildCountQuery(whereClause);
		else
			countQueryText = null;

		// create and return the main query object
		return new MainQuery<>(preStmts, postStmts, query,
				(countQueryText == null ? null :
					new SimpleValuePersistenceQueryImpl<>(resources,
							tx, countQueryText,
							ResultSetValueReader.LONG_VALUE_READER, params)));
	}
}
