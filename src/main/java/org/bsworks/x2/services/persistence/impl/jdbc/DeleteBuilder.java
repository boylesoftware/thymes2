package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.ResourcePropertiesContainer;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyPersistence;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.util.CollectionUtils;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Builder of a series of "DELETE" statements for deleting persistent resource
 * records.
 *
 * @author Lev Himmelfarb
 */
class DeleteBuilder {

	/**
	 * Execution plan step.
	 */
	private static final class ExecutionPlanStep {

		/**
		 * Statement text.
		 */
		final String stmtText;

		/**
		 * Statement parameter values.
		 */
		final List<JDBCParameterValue> params;

		/**
		 * Persistent resource class, records of which are deleted by this step,
		 * or {@code null} if not applicable.
		 */
		final Class<?> prsrcClass;


		/**
		 * Create new execution plan step.
		 *
		 * @param stmtText Statement text.
		 * @param params Statement parameter values.
		 * @param prsrcClass Persistent resource class, records of which are
		 * deleted by this step, or {@code null} if not applicable.
		 */
		ExecutionPlanStep(final String stmtText,
				final List<JDBCParameterValue> params,
				final Class<?> prsrcClass) {

			this.stmtText = stmtText;
			this.params = params;
			this.prsrcClass = prsrcClass;
		}
	}


	/**
	 * Generated execution plan.
	 */
	private final List<ExecutionPlanStep> executionPlan = new ArrayList<>();


	/**
	 * Create new builder for deleting persistent resource records matching the
	 * specified filter.
	 *
	 * @param resources Application resources manager.
	 * @param tx The transaction.
	 * @param prsrcHandler Persistent resource handler.
	 * @param filter Filter specification, may be {@code null} for deleting all
	 * records.
	 */
	DeleteBuilder(final Resources resources,
			final JDBCPersistenceTransaction tx,
			final PersistentResourceHandler<?> prsrcHandler,
			final FilterSpec<?> filter) {

		final SQLDialect dialect = tx.getSQLDialect();

		if ((filter == null) || filter.isEmpty()) { // no filter

			createSteps(
					resources,
					dialect,
					prsrcHandler.getResourceClass(),
					prsrcHandler,
					prsrcHandler.getPersistentCollectionName(),
					"t",
					0,
					new StringBuilder(128),
					new StringBuilder(128),
					null,
					Collections.<JDBCParameterValue>emptyList(),
					this.executionPlan);

		} else if (filter.isByIdOnly()) { // simple filter

			final IdPropertyHandler idPropHandler =
				prsrcHandler.getIdProperty();
			final Map<String, JDBCParameterValue> params = new HashMap<>();
			final WhereClause whereClauseBuilder = new WhereClause(
					resources,
					dialect,
					tx.getParameterValuesFactory(),
					filter,
					Collections.singletonMap(idPropHandler.getName(),
							new SingleValuedQueryProperty(
									"t." + idPropHandler.getPersistence()
										.getFieldName(),
									idPropHandler.getValueHandler()
										.getPersistentValueType())),
					Collections.<String, CollectionQueryProperty>emptyMap(),
					CollectionUtils.<String, String>emptySortedMap(),
					params);
			final List<JDBCParameterValue> paramsList = new ArrayList<>();
			final String whereClause = Utils.processSQL(resources,
					whereClauseBuilder.getBody(), params, paramsList);

			createSteps(
					resources,
					dialect,
					prsrcHandler.getResourceClass(),
					prsrcHandler,
					prsrcHandler.getPersistentCollectionName(),
					"t",
					0,
					new StringBuilder(128),
					new StringBuilder(128),
					whereClause,
					paramsList,
					this.executionPlan);

		} else { // complex filter

			final Class<?> prsrcClass = prsrcHandler.getResourceClass();
			final IdPropertyHandler idPropHandler =
				prsrcHandler.getIdProperty();
			final QueryBuilder queryBuilder = QueryBuilder.createQueryBuilder(
					resources,
					dialect,
					tx.getParameterValuesFactory(),
					tx.getActor(),
					prsrcHandler,
					resources
						.getPropertiesFetchSpec(prsrcClass)
						.include(idPropHandler.getName()),
					null,
					filter,
					null);

			final Map<String, JDBCParameterValue> params = new HashMap<>();
			final WhereClause whereClauseBuilder =
				queryBuilder.buildWhereClause(params);

			final String anchorTableName =
				"q_" + prsrcHandler.getPersistentCollectionName();
			final List<String> preStatements = new ArrayList<>();
			final List<String> postStatements = new ArrayList<>();
			dialect.makeSelectIntoTempTable(anchorTableName,
					tx.addTempTable(anchorTableName),
					dialect.makeSelectWithExclusiveLock(
							queryBuilder.buildIdsQuery(whereClauseBuilder,
									null),
							"t"),
					preStatements, postStatements);

			final List<JDBCParameterValue> paramsList = new ArrayList<>();
			for (final String preStatement : preStatements) {
				paramsList.clear();
				final String stmtText = Utils.processSQL(resources,
						preStatement, params, paramsList);
				this.executionPlan.add(new ExecutionPlanStep(stmtText,
						(paramsList.isEmpty()
								? Collections.<JDBCParameterValue>emptyList()
								: new ArrayList<>(paramsList)),
						null));
			}

			final StringBuilder joinedTablesBuf = new StringBuilder(128);
			joinedTablesBuf.append(anchorTableName).append(" AS a");
			final StringBuilder joinConditionsBuf = new StringBuilder(128);
			joinConditionsBuf.append("t.")
				.append(idPropHandler.getPersistence().getFieldName())
				.append(" = a.").append(QueryBuilder.ANCHOR_ID_COLNAME);

			createSteps(
					resources,
					dialect,
					prsrcHandler.getResourceClass(),
					prsrcHandler,
					prsrcHandler.getPersistentCollectionName(),
					"t",
					1,
					joinedTablesBuf,
					joinConditionsBuf,
					null,
					Collections.<JDBCParameterValue>emptyList(),
					this.executionPlan);

			for (final String postStatement : postStatements) {
				paramsList.clear();
				final String stmtText = Utils.processSQL(resources,
						postStatement, params, paramsList);
				this.executionPlan.add(new ExecutionPlanStep(stmtText,
						(paramsList.isEmpty()
								? Collections.<JDBCParameterValue>emptyList()
								: new ArrayList<>(paramsList)),
						null));
			}
		}
	}

	/**
	 * Create new builder for deleting persistent resource nested objects.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param propHandler The nested object property handler.
	 * @param tableAlias Alias to use for the nested objects table.
	 * @param whereClause Body of the "WHERE" clause.
	 * @param whereClauseParams Parameters for the "WHERE" clause.
	 */
	DeleteBuilder(final Resources resources, final SQLDialect dialect,
			final ObjectPropertyHandler propHandler, final String tableAlias,
			final String whereClause,
			final List<JDBCParameterValue> whereClauseParams) {

		createSteps(
				resources,
				dialect,
				null,
				propHandler,
				propHandler.getPersistence().getCollectionName(),
				tableAlias,
				0,
				new StringBuilder(128),
				new StringBuilder(128),
				whereClause,
				whereClauseParams,
				this.executionPlan);
	}

	/**
	 * Recursively create execution plan steps.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param prsrcClass Class of the persistent resource deleted at this level,
	 * or {@code null} for none.
	 * @param objHandler Object handler to recursively go into, or {@code null}
	 * for none.
	 * @param objTableName Table, from which to delete at this level.
	 * @param objTableAlias Table alias.
	 * @param numJoinedTables Number of joined tables in the buffer.
	 * @param joinedTablesBuf Joined tables buffer.
	 * @param joinConditionsBuf Join conditions buffer.
	 * @param whereClause Optional "WHERE" clause body, or {@code null} for
	 * none.
	 * @param params Parameters for the statement at this level.
	 * @param executionPlan The execution plan, to which to add generated steps.
	 */
	private static void createSteps(
			final Resources resources,
			final SQLDialect dialect,
			final Class<?> prsrcClass,
			final ResourcePropertiesContainer objHandler,
			final String objTableName,
			final String objTableAlias,
			final int numJoinedTables,
			final StringBuilder joinedTablesBuf,
			final StringBuilder joinConditionsBuf,
			final String whereClause,
			final List<JDBCParameterValue> params,
			final List<ExecutionPlanStep> executionPlan) {

		final IdPropertyHandler idPropHandler =
			(objHandler == null ? null : objHandler.getIdProperty());
		if ((idPropHandler != null) && (objHandler != null)) {

			final int joinedTablesBufLen = joinedTablesBuf.length();
			final int joinConditionsBufLen = joinConditionsBuf.length();

			for (final ResourcePropertyHandler propHandler :
					objHandler.getProperties().values()) {

				final ResourcePropertyPersistence propPersistence =
					propHandler.getPersistence();
				if (propPersistence == null)
					continue;

				final String propTableName =
					propPersistence.getCollectionName();
				if (propTableName == null)
					continue;

				if ((propHandler instanceof ObjectPropertyHandler)
						&& ((ObjectPropertyHandler) propHandler).isBorrowed())
					continue;

				if ((propHandler instanceof DependentRefPropertyHandler)
						&& !((DependentRefPropertyHandler) propHandler)
								.isDeleteCascaded())
					continue;

				if (joinedTablesBufLen > 0)
					joinedTablesBuf.append(", ");
				joinedTablesBuf.append(objTableName).append(" AS ")
					.append(objTableAlias);

				final String propTableAlias = "t" + numJoinedTables;
				if (joinConditionsBufLen > 0)
					joinConditionsBuf.append(" AND ");
				joinConditionsBuf.append(propTableAlias).append(".")
					.append(propPersistence.getParentIdFieldName())
					.append(" = ").append(objTableAlias).append(".")
					.append(idPropHandler.getPersistence().getFieldName());

				if (propHandler instanceof ObjectPropertyHandler) {
					final ObjectPropertyHandler objPropHandler =
						(ObjectPropertyHandler) propHandler;
					createSteps(
							resources,
							dialect,
							null,
							objPropHandler,
							propTableName,
							propTableAlias,
							numJoinedTables + 1,
							joinedTablesBuf,
							joinConditionsBuf,
							whereClause,
							params,
							executionPlan);
				} else if (propHandler instanceof DependentRefPropertyHandler) {
					final DependentRefPropertyHandler depRefPropHandler =
						(DependentRefPropertyHandler) propHandler;
					createSteps(
							resources,
							dialect,
							depRefPropHandler.getReferredResourceClass(),
							resources.getPersistentResourceHandler(
								depRefPropHandler.getReferredResourceClass()),
							propTableName,
							propTableAlias,
							numJoinedTables + 1,
							joinedTablesBuf,
							joinConditionsBuf,
							whereClause,
							params,
							executionPlan);
				} else {
					createSteps(
							resources,
							dialect,
							null,
							null,
							propTableName,
							propTableAlias,
							numJoinedTables + 1,
							joinedTablesBuf,
							joinConditionsBuf,
							whereClause,
							params,
							executionPlan);
				}

				joinedTablesBuf.setLength(joinedTablesBufLen);
				joinConditionsBuf.setLength(joinConditionsBufLen);
			}
		}

		if (numJoinedTables > 0) {
			executionPlan.add(new ExecutionPlanStep(
					dialect.createDeleteWithJoins(
							objTableName,
							objTableAlias,
							joinedTablesBuf.toString(),
							joinConditionsBuf.toString(),
							whereClause),
					params,
					prsrcClass));
		} else {
			executionPlan.add(new ExecutionPlanStep(
					dialect.createDeleteFromAliasedTable(
							objTableName,
							objTableAlias,
							whereClause),
					params,
					prsrcClass));
		}
	}


	/**
	 * Execute the generated statements.
	 *
	 * @param con Database connection.
	 * @param affectedResources Set, to which to add persistent resource
	 * classes, records of which were actually deleted. May be {@code null}.
	 *
	 * @return {@code true} if any records were deleted.
	 *
	 * @throws SQLException If a database error happens.
	 */
	boolean execute(final Connection con, final Set<Class<?>> affectedResources)
		throws SQLException {

		// the log
		final Log log = LogFactory.getLog(this.getClass());
		final boolean debug = log.isDebugEnabled();

		// execute the statements
		boolean deleted = false;
		for (final ExecutionPlanStep step : this.executionPlan) {
			try (final PreparedStatement pstmt =
				con.prepareStatement(step.stmtText)) {

				// set parameters
				int nextParamInd = 1;
				for (final JDBCParameterValue paramVal : step.params)
					nextParamInd = paramVal.set(pstmt, nextParamInd);

				// execute the statement
				if (debug)
					log.debug("executing SQL query:\n" + step.stmtText
							+ "\nparams: " + step.params);
				pstmt.execute();
				Utils.logWarnings(log, pstmt.getWarnings());

				// check if persistent resource records were deleted
				if (step.prsrcClass != null) {
					final int updateCount = pstmt.getUpdateCount();
					if (debug)
						log.debug("deleted " + updateCount + " records of "
								+ step.prsrcClass.getSimpleName());
					if (updateCount > 0) {
						if (affectedResources != null)
							affectedResources.add(step.prsrcClass);
						deleted = true;
					}
				}
			}
		}

		// log affected resources
		if (debug)
			log.debug("resources affected by delete: " + affectedResources);

		// done
		return deleted;
	}
}
