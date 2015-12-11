package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.LockType;
import org.bsworks.x2.services.persistence.PersistentResourceFetch;
import org.bsworks.x2.services.persistence.PersistentResourceFetchResult;
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
	 */
	private final class MainQuery {

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
	 * Tell if total count needs to be included in the ranged fetch result.
	 */
	private boolean includeTotalCount = false;

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
	 * @see org.bsworks.x2.services.persistence.PersistentResourceFetch#includeTotalCount()
	 */
	@Override
	public PersistentResourceFetch<R> includeTotalCount() {

		this.includeTotalCount = true;

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

		// build the query
		final QueryBuilder qb = this.buildQuery(
				this.resources.getPropertiesFetchSpec(
						this.prsrcHandler.getResourceClass()));

		// get the main query
		final MainQuery mainQuery = this.getMainQuery(qb, true);

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
		final QueryBuilder qb = this.buildQuery(this.propsFetch);

		// references fetch result map and the count
		final Map<String, Object> refsFetchResultMap =
			this.getRefsFetchResultMap();
		long totalCount = -1;

		// get the main query
		final MainQuery mainQuery = this.getMainQuery(qb, false);

		// execute pre-statements
		if (mainQuery.preStatements != null)
			for (final PersistenceUpdateImpl stmt : mainQuery.preStatements)
				stmt.execute();
		try {

			// get the records count if requested
			if (mainQuery.countQuery != null)
				totalCount =
					mainQuery.countQuery.getFirstResult(null).longValue();

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

			// return the result
			return new PersistentResourceFetchResult<>(mainResult, totalCount,
					refsFetchResultMap);

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
	public R getSingleResult() {

		// build the query
		final PropertiesFetchSpec<R> propsFetch = this.propsFetch;
		final QueryBuilder qb = this.buildQuery(new PropertiesFetchSpec<R>() {
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
			public FilterSpec<? extends Object> getAggregateFilter(
					final String propPath) {
				return propsFetch.getAggregateFilter(propPath);
			}
		});

		// get the main query
		final MainQuery mainQuery = this.getMainQuery(qb, false);

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
	 * Build the query.
	 *
	 * @param propsFetch Properties fetch specification to use.
	 *
	 * @return The top query builder.
	 */
	private QueryBuilder buildQuery(final PropertiesFetchSpec<R> propsFetch) {

		return QueryBuilder.createQueryBuilder(this.resources,
				this.tx.getSQLDialect(), this.tx.getParameterValuesFactory(),
				this.tx.getActor(), this.prsrcHandler, propsFetch, this.filter,
				this.order);
	}

	/**
	 * Build, prepare and return the main query.
	 *
	 * @param qb Top query builder.
	 * @param forceCount {@code true} to build records count query even if no
	 * range set in the fetch.
	 *
	 * @return The main query.
	 */
	private MainQuery getMainQuery(final QueryBuilder qb,
			final boolean forceCount) {

		// the queries, the pre- and post- statements, the parameters
		final List<String> preStmtTexts = new ArrayList<>();
		final List<String> postStmtTexts = new ArrayList<>();
		final String countQueryText;
		final String dataQueryText;
		final Map<String, JDBCParameterValue> params = new HashMap<>();

		// needed objects from the transaction
		final SQLDialect dialect = this.tx.getSQLDialect();
		final ParameterValuesFactoryImpl paramsFactory =
			this.tx.getParameterValuesFactory();

		// get "WHERE" and "ORDER BY" clauses
		final WhereClause whereClause = (
				(this.filter == null) || this.filter.isEmpty() ?
						null : qb.buildWhereClause(params));
		final OrderByClause orderByClause = (this.order == null ? null :
			qb.buildOrderByClause(params));

		// check if no branches
		if (qb.getBranches().isEmpty()) {

			// check if no range
			if (this.range == null) {

				// direct select
				final String q =
					qb.buildDirectSelectQuery(whereClause, orderByClause);
				if (this.lockType == null)
					dataQueryText = q;
				else switch (this.lockType) {
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
							this.range, paramsFactory, params);
					if (this.lockType == null)
						dataQueryText = q;
					else switch (this.lockType) {
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
							this.range, paramsFactory, params);
					dialect.makeSelectIntoTempTable(
							this.anchorTableName,
							this.tx.addTempTable(this.anchorTableName),
							(this.lockType == LockType.EXCLUSIVE
									? dialect.makeSelectWithExclusiveLock(
											q, qb.getRootTableAlias())
									: dialect.makeSelectWithShareLock(
											q, qb.getRootTableAlias())),
							preStmtTexts, postStmtTexts);

					// anchored select
					dataQueryText =
						qb.buildAnchoredSelectQuery(this.anchorTableName,
								orderByClause);
				}
			}

		} else { // branched query

			// check if no range
			if (this.range == null) {

				// create anchor table
				final String q = qb.buildIdsQuery(whereClause, null);
				dialect.makeSelectIntoTempTable(
						this.anchorTableName,
						this.tx.addTempTable(this.anchorTableName),
						(this.lockType == LockType.EXCLUSIVE
								? dialect.makeSelectWithExclusiveLock(
										q, qb.getRootTableAlias())
								: dialect.makeSelectWithShareLock(
										q, qb.getRootTableAlias())),
						preStmtTexts, postStmtTexts);

			} else {  // ranged query

				// create anchor table
				final String q = dialect.makeRangedSelect(
						qb.buildIdsQuery(whereClause, orderByClause),
						this.range, paramsFactory, params);
				dialect.makeSelectIntoTempTable(
						this.anchorTableName,
						this.tx.addTempTable(this.anchorTableName),
						(this.lockType == LockType.EXCLUSIVE
								? dialect.makeSelectWithExclusiveLock(
										q, qb.getRootTableAlias())
								: dialect.makeSelectWithShareLock(
										q, qb.getRootTableAlias())),
						preStmtTexts, postStmtTexts);
			}

			// anchored select
			dataQueryText =
				qb.buildAnchoredSelectQuery(this.anchorTableName,
						orderByClause);
		}

		// prepare pre- and post- statements
		final List<PersistenceUpdateImpl> preStmts;
		if (!preStmtTexts.isEmpty()) {
			preStmts = new ArrayList<>(preStmtTexts.size());
			for (final String stmtText : preStmtTexts)
				preStmts.add(new PersistenceUpdateImpl(this.resources, this.tx,
						stmtText, params));
		} else {
			preStmts = null;
		}
		final List<PersistenceUpdateImpl> postStmts;
		if (!postStmtTexts.isEmpty()) {
			postStmts = new ArrayList<>(postStmtTexts.size());
			for (final String stmtText : postStmtTexts)
				postStmts.add(new PersistenceUpdateImpl(this.resources, this.tx,
						stmtText, params));
		} else {
			postStmts = null;
		}

		// create the query
		ResourcePersistenceQueryImpl<R> query =
			new ResourcePersistenceQueryImpl<>(this.resources, this.tx,
					dataQueryText, this.prsrcHandler.getResourceClass(),
					params);
		query.setSessionCache(new ResourceReadSessionCache());

		// create records count query
		if (this.includeTotalCount || forceCount)
			countQueryText = qb.buildCountQuery(whereClause);
		else
			countQueryText = null;

		// create and return the main query object
		return new MainQuery(preStmts, postStmts, query,
				(countQueryText == null ? null :
					new SimpleValuePersistenceQueryImpl<>(this.resources,
							this.tx, countQueryText,
							ResultSetValueReader.LONG_VALUE_READER, params)));
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
}
