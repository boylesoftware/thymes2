package org.bsworks.x2.services.persistence.impl.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.Ref;
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
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Builder of a series of SQL statements for updating a persistent resource
 * record.
 *
 * @author Lev Himmelfarb
 */
class UpdateBuilder {

	/**
	 * Used to compare two simple values for equality.
	 */
	private static interface ValueComparator {

		/**
		 * Tell if two non-null values are equal.
		 *
		 * @param val1 Value one.
		 * @param val2 Value two.
		 *
		 * @return {@code true} if values are equal.
		 */
		boolean equal(Object val1, Object val2);
	}

	/**
	 * Simple value comparator using {@link Object#equals(Object)} method.
	 */
	private static final ValueComparator SIMPLE_VALUE_COMPARATOR =
		new ValueComparator() {
		@Override
		public boolean equal(final Object val1, final Object val2) {

			return val1.equals(val2);
		}
	};

	/**
	 * Value comparator for {@link BigDecimal}s that may have different scales.
	 */
	private static final ValueComparator DECIMAL_VALUE_COMPARATOR =
		new ValueComparator() {
		@Override
		public boolean equal(final Object val1, final Object val2) {

			if (val1 == val2)
				return true;

			if ((val1 == null) || (val2 == null))
				return false;

			final BigDecimal num1 = (BigDecimal) val1;
			final BigDecimal num2 = (BigDecimal) val2;

			final int scale1 = num1.scale();
			final int scale2 = num2.scale();
			final BigDecimal scaledNum1;
			final BigDecimal scaledNum2;
			if (scale1 > scale2) {
				scaledNum1 = num1;
				scaledNum2 = num2.setScale(scale1, RoundingMode.UNNECESSARY);
			} else if (scale2 > scale1) {
				scaledNum1 = num1.setScale(scale2, RoundingMode.UNNECESSARY);
				scaledNum2 = num2;
			} else {
				scaledNum1 = num1;
				scaledNum2 = num2;
			}

			return scaledNum1.equals(scaledNum2);
		}
	};


	/**
	 * Execution plan step.
	 */
	private static interface ExecutionPlanStep {

		/**
		 * Execute the step.
		 *
		 * @param con Database connection.
		 *
		 * @throws SQLException If a database error happens.
		 */
		void execute(Connection con)
			throws SQLException;
	}

	/**
	 * Simple execution plan step with SQL statement and parameters.
	 */
	private static final class SimpleExecutionPlanStep
		implements ExecutionPlanStep {

		/**
		 * SQL statement text.
		 */
		private final String stmtText;

		/**
		 * Parameters.
		 */
		private final List<JDBCParameterValue> params;


		/**
		 * Create new execution plan step.
		 *
		 * @param stmtText SQL statement text.
		 * @param params Parameters.
		 */
		SimpleExecutionPlanStep(final String stmtText,
				final List<JDBCParameterValue> params) {

			this.stmtText = stmtText;
			this.params = params;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public void execute(final Connection con)
			throws SQLException {

			final Log log = LogFactory.getLog(UpdateBuilder.class);

			try (final PreparedStatement pstmt =
					con.prepareStatement(this.stmtText)) {

				int paramInd = 1;
				for (final JDBCParameterValue param : this.params)
					paramInd = param.set(pstmt, paramInd);

				if (log.isDebugEnabled())
					log.debug("executing SQL query:\n" + this.stmtText
							+ "\nparams: " + this.params);
				pstmt.execute();
				Utils.logWarnings(log, pstmt.getWarnings());
			}
		}
	}

	/**
	 * Execution plan step that executes same SQL statement multiple times for
	 * different parameters.
	 */
	private static final class MultiStatementExecutionPlanStep
		implements ExecutionPlanStep {

		/**
		 * SQL statement text.
		 */
		private final String stmtText;

		/**
		 * Constant parameters.
		 */
		private final List<JDBCParameterValue> constantParams;

		/**
		 * Variable parameters.
		 */
		private final List<List<JDBCParameterValue>> variableParams;

		/**
		 * Tells if constant parameter placeholders are at the end of the
		 * statement.
		 */
		private final boolean constantParamsAtTheEnd;


		/**
		 * Create new execution plan step.
		 *
		 * @param stmtText SQL statement text.
		 * @param constantParams Constant parameters.
		 * @param variableParams Variable parameters.
		 * @param constantParamsAtTheEnd {@code true} if constant parameter
		 * placeholders are at the end of the statement.
		 */
		MultiStatementExecutionPlanStep(final String stmtText,
				final List<JDBCParameterValue> constantParams,
				final List<List<JDBCParameterValue>> variableParams,
				final boolean constantParamsAtTheEnd) {

			this.stmtText = stmtText;
			this.constantParams = constantParams;
			this.variableParams = variableParams;
			this.constantParamsAtTheEnd = constantParamsAtTheEnd;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public void execute(final Connection con)
			throws SQLException {

			final Log log = LogFactory.getLog(UpdateBuilder.class);
			final boolean debug = log.isDebugEnabled();

			final int numConstParams = this.constantParams.size();
			final int numVarParams = this.variableParams.get(0).size();
			final int firstConstParamInd =
				(this.constantParamsAtTheEnd ? numVarParams + 1 : 1);
			final int firstVarParamInd =
				(this.constantParamsAtTheEnd ? 1 : numConstParams + 1);

			try (final PreparedStatement pstmt =
					con.prepareStatement(this.stmtText)) {

				int paramInd = firstConstParamInd;
				for (final JDBCParameterValue param : this.constantParams)
					paramInd = param.set(pstmt, paramInd);

				for (final List<JDBCParameterValue> params :
						this.variableParams) {

					paramInd = firstVarParamInd;
					for (final JDBCParameterValue param : params)
						paramInd = param.set(pstmt, paramInd);

					if (debug)
						log.debug("executing SQL query:\n" + this.stmtText
								+ "\nparams: " + (this.constantParamsAtTheEnd
										? params + ", " + this.constantParams
										: this.constantParams + ", " + params));
					pstmt.execute();
					Utils.logWarnings(log, pstmt.getWarnings());
				}
			}
		}
	}

	/**
	 * Execution plan step that executes a delete builder execution plan.
	 */
	private static final class DeleteBuilderExecutionPlanStep
		implements ExecutionPlanStep {

		/**
		 * The delete builder.
		 */
		private final DeleteBuilder deleteBuilder;


		/**
		 * Create new execution plan step.
		 *
		 * @param deleteBuilder The delete builder.
		 */
		DeleteBuilderExecutionPlanStep(final DeleteBuilder deleteBuilder) {

			this.deleteBuilder = deleteBuilder;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public void execute(final Connection con)
			throws SQLException {

			this.deleteBuilder.execute(con, null);
		}
	}

	/**
	 * Execution plan step that executes an insert builder execution plan.
	 */
	private static final class InsertBuilderExecutionPlanStep
		implements ExecutionPlanStep {

		/**
		 * The insert builder.
		 */
		private final InsertBuilder insertBuilder;


		/**
		 * Create new execution plan step.
		 *
		 * @param insertBuilder The insert builder.
		 */
		InsertBuilderExecutionPlanStep(final InsertBuilder insertBuilder) {

			this.insertBuilder = insertBuilder;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public void execute(final Connection con)
			throws SQLException {

			this.insertBuilder.execute(con);
		}
	}


	/**
	 * Update statement builder context.
	 */
	private static final class StatementBuilderContext {

		/**
		 * Parent context, or {@code null} for top context.
		 */
		private final StatementBuilderContext parentCtx;

		/**
		 * Application resources manager.
		 */
		private final Resources resources;

		/**
		 * SQL dialect.
		 */
		private final SQLDialect dialect;

		/**
		 * Parameter value handlers factory.
		 */
		private final ParameterValuesFactoryImpl paramsFactory;

		/**
		 * The actor.
		 */
		private final Actor actor;

		/**
		 * Handler of the record id property of the object updated by this
		 * context's statement. May be {@code null}.
		 */
		private final IdPropertyHandler idPropHandler;

		/**
		 * Map key value handler, or {@code null} if not nested object map
		 * element.
		 */
		private final ResourcePropertyValueHandler keyValueHandler;

		/**
		 * Name of the table updated by this context's statement.
		 */
		private final String tableName;

		/**
		 * The existing record updated by this context's statement.
		 */
		private Object rec;

		/**
		 * Id of the record updated by this context's statement, or {@code null}
		 * if no record id property (the map key is used).
		 */
		private JDBCParameterValue recId;

		/**
		 * Name of the record id column in the table updated by this context's
		 * statement, or {@code null} if no record id property (the map key is
		 * used).
		 */
		private final String recIdColName;

		/**
		 * Name of the column in the table updated by this context's statement
		 * used to identify the updated row. May be record id, map key, or
		 * parent record id in different cases.
		 */
		private final String rowIdColName;

		/**
		 * Id value to use to identify the row updated by this context's
		 * statement.
		 */
		private JDBCParameterValue rowId;

		/**
		 * "SET" clause body.
		 */
		private final StringBuilder setClause = new StringBuilder(128);

		/**
		 * Values for the "SET" clause.
		 */
		private final List<JDBCParameterValue> setValues = new ArrayList<>();

		/**
		 * Execution plan steps to execute before executing this context's
		 * update.
		 */
		private final List<ExecutionPlanStep> preExecutionPlan =
			new ArrayList<>();


		/**
		 * Create context for the top persistent resource table update.
		 *
		 * @param resources Application resources manager.
		 * @param dialect SQL dialect.
		 * @param paramsFactory Parameter value handlers factory.
		 * @param actor The actor.
		 * @param prsrcHandler Persistent resource handler.
		 * @param rec The existing persistent resource record being updated.
		 */
		StatementBuilderContext(final Resources resources,
				final SQLDialect dialect,
				final ParameterValuesFactoryImpl paramsFactory,
				final Actor actor,
				final PersistentResourceHandler<?> prsrcHandler,
				final Object rec) {

			this.parentCtx = null;
			this.resources = resources;
			this.dialect = dialect;
			this.paramsFactory = paramsFactory;
			this.actor = actor;
			this.idPropHandler = prsrcHandler.getIdProperty();
			this.keyValueHandler = null;
			this.tableName = prsrcHandler.getPersistentCollectionName();
			this.rec = rec;
			this.recId = paramsFactory.getParameterValue(
					this.idPropHandler.getValueHandler()
						.getPersistentValueType(),
					this.idPropHandler.getValue(rec));
			this.recIdColName =
				this.idPropHandler.getPersistence().getFieldName();
			this.rowIdColName = this.recIdColName;
			this.rowId = this.recId;
		}

		/**
		 * Create child context.
		 *
		 * @param parentCtx Parent context.
		 * @param propHandler Nested object property handler.
		 */
		private StatementBuilderContext(final StatementBuilderContext parentCtx,
				final ObjectPropertyHandler propHandler) {

			final ResourcePropertyPersistence propPersistentce =
				propHandler.getPersistence();

			this.parentCtx = parentCtx;
			this.resources = parentCtx.resources;
			this.dialect = parentCtx.dialect;
			this.paramsFactory = parentCtx.paramsFactory;
			this.actor = parentCtx.actor;
			this.idPropHandler = propHandler.getIdProperty();
			this.keyValueHandler = propHandler.getKeyValueHandler();
			this.recIdColName = (this.idPropHandler == null ? null :
				this.idPropHandler.getPersistence().getFieldName());
			this.tableName = propPersistentce.getCollectionName();
			if (this.recIdColName != null)
				this.rowIdColName = this.recIdColName;
			else if (propPersistentce.getKeyFieldName() != null)
				this.rowIdColName = propPersistentce.getKeyFieldName();
			else
				this.rowIdColName = propPersistentce.getParentIdFieldName();
		}


		/**
		 * Get context for a child table update.
		 *
		 * @param propHandler Nested object property handler.
		 * @param rec Existing nested object. May be {@code null}, in which case
		 * the context must be {@link #reset(Object, Object)} before it can be
		 * used.
		 * @param key Map key, or {@code null} if not a nested object map
		 * element or the key will be set later with a
		 * {@link #reset(Object, Object)}.
		 *
		 * @return Empty child context.
		 */
		StatementBuilderContext getChildContext(
				final ObjectPropertyHandler propHandler, final Object rec,
				final Object key) {

			final StatementBuilderContext childCtx =
				new StatementBuilderContext(this, propHandler);

			if (rec != null)
				childCtx.reset(rec, key);

			return childCtx;
		}

		/**
		 * Reset child context for a new nested object record.
		 *
		 * @param rec Existing nested object record.
		 * @param key Map key, or {@code null} if not a nested object map
		 * element.
		 */
		void reset(final Object rec, final Object key) {

			this.rec = rec;

			if (this.idPropHandler != null) {
				this.recId = this.paramsFactory.getParameterValue(
						this.idPropHandler.getValueHandler()
							.getPersistentValueType(),
						this.idPropHandler.getValue(rec));
				this.rowId = this.recId;
			} else if (key != null) {
				this.rowId = this.paramsFactory.getParameterValue(
						this.keyValueHandler.getPersistentValueType(),
						(this.keyValueHandler.isRef() ?
								((Ref<?>) key).getId() : key));
			} else {
				this.rowId = this.parentCtx.recId;
			}

			this.setClause.setLength(0);
			this.setValues.clear();

			this.preExecutionPlan.clear();
		}

		/**
		 * Tell if this context does not generate any execution plan steps.
		 *
		 * @return {@code true} if empty context.
		 */
		boolean isEmpty() {

			return ((this.setClause.length() == 0)
					&& this.preExecutionPlan.isEmpty());
		}

		/**
		 * Add this child context's statements to the parent context
		 * pre-execution plan.
		 */
		void mergeUp() {

			this.exportExecutionPlanSteps(this.parentCtx.preExecutionPlan);
		}

		/**
		 * Export this context's steps to another execution plan.
		 *
		 * @param steps Execution plan, to which to append this context's steps.
		 */
		void exportExecutionPlanSteps(final List<ExecutionPlanStep> steps) {

			steps.addAll(this.preExecutionPlan);

			final ExecutionPlanStep stmt = this.getStatement();
			if (stmt != null)
				steps.add(stmt);
		}

		/**
		 * Get this context's "UPDATE" statement.
		 *
		 * @return Execution plan step that executes the "UPDATE" statement, or
		 * {@code null} if no need for update.
		 */
		ExecutionPlanStep getStatement() {

			if (this.setClause.length() == 0)
				return null;

			final String stmtText =
				"UPDATE " + this.tableName
				+ " SET " + this.setClause
				+ " WHERE " + this.rowIdColName + " = ?";

			final int numVals = this.setValues.size();
			final List<JDBCParameterValue> params =
				new ArrayList<>(numVals > 9 ? numVals + 1 : 10);
			params.addAll(this.setValues);
			params.add(this.rowId);

			return new SimpleExecutionPlanStep(stmtText, params);
		}

		/**
		 * Add column to the "UPDATE" statement's "SET" clause.
		 *
		 * @param colName Column name.
		 * @param value Value to set.
		 */
		void addSet(final String colName, final JDBCParameterValue value) {

			if (this.setClause.length() > 0)
				this.setClause.append(", ");
			this.setClause.append(colName).append(" = ?");

			this.setValues.add(value);
		}

		/**
		 * Add "DELETE" statement to the context pre-execution plan to delete
		 * rows from a simple/reference value(s) table.
		 *
		 * @param valueTableName Value(s) table name.
		 * @param parentIdColName Name of the parent id column in the value(s)
		 * table.
		 * @param keyColName Map key column name, or {@code null} if none.
		 * @param keys Map key values to delete, or {@code null} if not
		 * applicable.
		 */
		void addDeleteFromValueTable(final String valueTableName,
				final String parentIdColName, final String keyColName,
				Collection<JDBCParameterValue> keys) {

			final StringBuilder stmtText = new StringBuilder(128);
			stmtText.append("DELETE FROM ").append(valueTableName)
				.append(" WHERE ").append(parentIdColName).append(" = ?");

			final List<JDBCParameterValue> params;

			if (keyColName == null) {
				params = Collections.singletonList(this.recId);
			} else {
				final int numKeys = keys.size();
				params = new ArrayList<>(numKeys > 9 ? numKeys + 1 : 10);
				params.add(this.recId);
				stmtText.append(" AND ").append(keyColName);
				if (numKeys > 1) {
					stmtText.append(" IN (");
					for (int i = 0; i < numKeys; i++) {
						if (i > 0)
							stmtText.append(", ");
						stmtText.append("?");
					}
					stmtText.append(")");
					params.addAll(keys);
				} else {
					stmtText.append(" = ?");
					params.add(keys.iterator().next());
				}
			}

			this.preExecutionPlan.add(new SimpleExecutionPlanStep(
					stmtText.toString(), params));
		}

		/**
		 * Add "UPDATE" statement to the context pre-execution plan to update a
		 * single valued simple/reference property stored in its own table
		 * (should be a very rare case).
		 *
		 * @param valueTableName Value table name.
		 * @param parentIdColName Name of the parent id column in the value
		 * table.
		 * @param valueColName Name of the value column in the value table.
		 * @param value The new value.
		 */
		void addUpdateValueTable(final String valueTableName,
				final String parentIdColName, final String valueColName,
				final JDBCParameterValue value) {

			final List<JDBCParameterValue> params = new ArrayList<>();
			params.add(this.recId);
			params.add(value);
			this.preExecutionPlan.add(new SimpleExecutionPlanStep(
					"UPDATE " + valueTableName
						+ " SET " + valueColName + " = ?"
						+ " WHERE " + parentIdColName + " = ?",
					params));
		}

		/**
		 * Add "UPDATE" statement to the context pre-execution plan to update
		 * simple/reference map property.
		 *
		 * @param valueTableName Value table name.
		 * @param parentIdColName Name of the parent id column in the value
		 * table.
		 * @param keyColName Name of the map key column in the value table.
		 * @param valueColName Name of the value column in the value table.
		 * @param entries The entries (first element value, second key).
		 */
		void addUpdateValueTable(final String valueTableName,
				final String parentIdColName, final String keyColName,
				final String valueColName,
				final List<List<JDBCParameterValue>> entries) {

			this.preExecutionPlan.add(new MultiStatementExecutionPlanStep(
					"UPDATE " + valueTableName
						+ " SET " + valueColName + " = ?"
						+ " WHERE " + keyColName + " = ? AND "
						+ parentIdColName + " = ?",
					Collections.singletonList(this.recId),
					entries, true));
		}

		/**
		 * Add "INSERT" statement to the context pre-execution plan to insert
		 * values of a simple/reference property stored in its own table.
		 *
		 * @param valueTableName Value table name.
		 * @param parentIdColName Name of the parent id column in the value
		 * table.
		 * @param valueColName Name of the value column in the value table.
		 * @param values The values.
		 */
		void addInsertIntoValueTable(final String valueTableName,
				final String parentIdColName, final String valueColName,
				final Collection<JDBCParameterValue> values) {

			final String stmtText =
				"INSERT INTO " + valueTableName
					+ " (" + parentIdColName + ", " + valueColName + ")"
					+ " VALUES (?, ?)";

			final int numValues = values.size();
			if (numValues > 1) {
				final List<JDBCParameterValue> constantParams =
					Collections.singletonList(this.recId);
				final List<List<JDBCParameterValue>> variableParams =
					new ArrayList<>(numValues > 10 ? numValues : 10);
				for (final JDBCParameterValue value : values)
					variableParams.add(Collections.singletonList(value));
				this.preExecutionPlan.add(
						new MultiStatementExecutionPlanStep(stmtText,
								constantParams, variableParams, false));
			} else {
				final List<JDBCParameterValue> params = new ArrayList<>();
				params.add(this.recId);
				params.add(values.iterator().next());
				this.preExecutionPlan.add(
						new SimpleExecutionPlanStep(stmtText, params));
			}
		}

		/**
		 * Add "INSERT" statement to the context pre-execution plan to insert
		 * values of a simple/reference map property.
		 *
		 * @param valueTableName Value table name.
		 * @param parentIdColName Name of the parent id column in the value
		 * table.
		 * @param keyColName Name of the map key column in the value table.
		 * @param valueColName Name of the value column in the value table.
		 * @param entries The entries (first element key, second value).
		 */
		void addInsertIntoValueTable(final String valueTableName,
				final String parentIdColName, final String keyColName,
				final String valueColName,
				final List<List<JDBCParameterValue>> entries) {

			final String stmtText =
				"INSERT INTO " + valueTableName
					+ " (" + parentIdColName + ", " + keyColName
						+ ", " + valueColName + ")"
					+ " VALUES (?, ?, ?)";

			final int numEntries = entries.size();
			if (numEntries > 1) {
				final List<JDBCParameterValue> constantParams =
					Collections.singletonList(this.recId);
				this.preExecutionPlan.add(
						new MultiStatementExecutionPlanStep(stmtText,
								constantParams, entries, false));
			} else {
				final List<JDBCParameterValue> params = new ArrayList<>();
				params.add(this.recId);
				params.addAll(entries.iterator().next());
				this.preExecutionPlan.add(
						new SimpleExecutionPlanStep(stmtText, params));
			}
		}

		/**
		 * Add delete nested objects to the context's pre-execution plan.
		 *
		 * @param propHandler Nested object property handler.
		 * @param keyColName Optional key column name (map key or record id), or
		 * {@code null} to delete all.
		 * @param keyVals Key values, of key column name is specified. Ignored
		 * otherwise.
		 */
		void addDeleteNestedObjects(final ObjectPropertyHandler propHandler,
				final String keyColName,
				final List<JDBCParameterValue> keyVals) {

			final StringBuilder whereClause = new StringBuilder(128);
			final List<JDBCParameterValue> whereClauseParams =
				new ArrayList<>();
			whereClause.append("t.")
				.append(propHandler.getPersistence().getParentIdFieldName())
				.append(" = ?");
			whereClauseParams.add(this.recId);
			if (keyColName != null) {
				whereClause.append(" AND t.").append(keyColName);
				final int numKeys = keyVals.size();
				if (numKeys > 1) {
					whereClause.append(" IN (");
					for (int i = 0; i < numKeys; i++) {
						if (i > 0)
							whereClause.append(", ");
						whereClause.append("?");
						whereClauseParams.add(keyVals.get(i));
					}
					whereClause.append(")");
				} else {
					whereClause.append(" = ?");
					whereClauseParams.add(keyVals.get(0));
				}
			}

			this.preExecutionPlan.add(new DeleteBuilderExecutionPlanStep(
					new DeleteBuilder(this.resources, this.dialect, propHandler,
							"t", whereClause.toString(), whereClauseParams)));
		}

		/**
		 * Add insert nested objects to the context's pre-execution plan.
		 *
		 * @param propHandler Nested object property handler.
		 * @param objTmpls Nested object templates to insert.
		 */
		void addInsertNestedObjects(final ObjectPropertyHandler propHandler,
				final Collection<?> objTmpls) {

			this.preExecutionPlan.add(new InsertBuilderExecutionPlanStep(
					new InsertBuilder(this.resources, this.paramsFactory,
							this.tableName, this.rec, this.idPropHandler,
							propHandler, objTmpls, this.actor)));
		}

		/**
		 * Add insert nested objects map to the context's pre-execution plan.
		 *
		 * @param propHandler Nested object property handler.
		 * @param objTmpls Nested object templates by map keys.
		 */
		void addInsertNestedObjects(final ObjectPropertyHandler propHandler,
				final Map<?, ?> objTmpls) {

			this.preExecutionPlan.add(new InsertBuilderExecutionPlanStep(
					new InsertBuilder(this.resources, this.paramsFactory,
							this.tableName, this.rec, this.idPropHandler,
							propHandler, objTmpls, this.actor)));
		}
	}


	/**
	 * Generated execution plan.
	 */
	private final List<ExecutionPlanStep> executionPlan = new ArrayList<>();


	/**
	 * Create builder.
	 *
	 * @param resources Application resources manager.
	 * @param tx The transaction.
	 * @param prsrcHandler Persistent resource handler.
	 * @param rec Existing persistent resource record.
	 * @param recTmpl Persistent resource record template with new data.
	 * @param updatedProps Set, to which to add paths of the record properties
	 * that were updated, or {@code null} if such information is not needed by
	 * the caller.
	 */
	UpdateBuilder(final Resources resources,
			final JDBCPersistenceTransaction tx,
			final PersistentResourceHandler<?> prsrcHandler,
			final Object rec, final Object recTmpl,
			final Set<String> updatedProps) {

		final ParameterValuesFactoryImpl paramsFactory =
			tx.getParameterValuesFactory();
		final Actor actor = tx.getActor();

		// create top update context
		final StatementBuilderContext topCtx = new StatementBuilderContext(
				resources, tx.getSQLDialect(), paramsFactory, actor,
				prsrcHandler, rec);

		// process persistent resource record
		final Map<Class<?>, Set<Object>> depPRsrcIdsToDelete = new HashMap<>();
		processObjectProperties(resources, paramsFactory, topCtx,
				new StringBuilder(64), prsrcHandler, rec, recTmpl, actor,
				updatedProps, depPRsrcIdsToDelete);

		// if any modifications, update relevant meta-properties
		if (!topCtx.isEmpty()) {
			final MetaPropertyHandler verPropHandler =
				prsrcHandler.getMetaProperty(MetaPropertyType.VERSION);
			if (verPropHandler != null) {
				final Number curVer = (Number) verPropHandler.getValue(rec);
				final Object newVer;
				if (curVer instanceof Integer)
					newVer = Integer.valueOf(curVer.intValue() + 1);
				else if (curVer instanceof Long)
					newVer = Long.valueOf(curVer.longValue() + 1);
				else // cannot happen
					throw new RuntimeException("Unsupported record version"
							+ " property class " + curVer.getClass().getName()
							+ ".");
				verPropHandler.setValue(rec, newVer);
				topCtx.addSet(
						verPropHandler.getPersistence().getFieldName(),
						paramsFactory.getParameterValue(
								verPropHandler.getValueHandler()
									.getPersistentValueType(),
								newVer));
			}
			final MetaPropertyHandler lastModTSPropHandler =
				prsrcHandler.getMetaProperty(
						MetaPropertyType.MODIFICATION_TIMESTAMP);
			if (lastModTSPropHandler != null) {
				final Date now = new Date();
				lastModTSPropHandler.setValue(rec, now);
				topCtx.addSet(
						lastModTSPropHandler.getPersistence().getFieldName(),
						paramsFactory.getParameterValue(
								lastModTSPropHandler.getValueHandler()
									.getPersistentValueType(),
								now));
			}
			final MetaPropertyHandler lastModByPropHandler =
				prsrcHandler.getMetaProperty(
						MetaPropertyType.MODIFICATION_ACTOR);
			if (lastModByPropHandler != null) {
				lastModByPropHandler.setValue(rec, actor.getActorName());
				topCtx.addSet(
						lastModByPropHandler.getPersistence().getFieldName(),
						paramsFactory.getParameterValue(
								lastModByPropHandler.getValueHandler()
									.getPersistentValueType(),
								actor.getActorName()));
			}
		}

		// get execution plan
		topCtx.exportExecutionPlanSteps(this.executionPlan);

		// add dependent record deletes
		for (final Map.Entry<Class<?>, Set<Object>> entry :
				depPRsrcIdsToDelete.entrySet()) {
			final Class<?> depRefClass = entry.getKey();
			final Set<Object> ids = entry.getValue();
			final PersistentResourceHandler<?> depRefHandler =
				resources.getPersistentResourceHandler(depRefClass);
			this.executionPlan.add(new DeleteBuilderExecutionPlanStep(
					new DeleteBuilder(
							resources,
							tx,
							depRefHandler,
							resources
								.getFilterSpec(depRefClass)
								.addCondition(
										depRefHandler.getIdProperty().getName(),
										PropertyValueFunction.PLAIN, null,
										FilterConditionType.EQ, false,
										ids.toArray(new Object[ids.size()])))));
		}

		// log updated properties for debugging
		final Log log = LogFactory.getLog(this.getClass());
		if (log.isDebugEnabled())
			log.debug("updates properties: " + updatedProps);
	}

	/**
	 * Recursively process object properties.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param ctx Current object statement builder context.
	 * @param propPath Path of the property corresponding to the object, empty
	 * buffer for top level resource record.
	 * @param objHandler Object handler.
	 * @param obj Existing object, may be {@code null}.
	 * @param objTmpl New object data, may be {@code null}.
	 * @param actor The actor.
	 * @param updatedProps Set, to which to add paths of the record properties
	 * that were updated, or {@code null} if such information is not needed by
	 * the caller.
	 * @param depPRsrcIdsToDelete Map, to which to add ids of the referred
	 * records that need to be deleted.
	 *
	 * @return {@code true} if the property is modified.
	 */
	private static boolean processObjectProperties(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final StatementBuilderContext ctx, final StringBuilder propPath,
			final ResourcePropertiesContainer objHandler, final Object obj,
			final Object objTmpl, final Actor actor,
			final Set<String> updatedProps,
			final Map<Class<?>, Set<Object>> depPRsrcIdsToDelete) {

		// process object properties
		boolean modified = false;
		for (final ResourcePropertyHandler propHandler :
				objHandler.getProperties().values()) {

			// check if property update is allowed
			if (!propHandler.isAllowed(ResourcePropertyAccess.UPDATE, actor))
				continue;

			// get new property value
			final Object newPropVal = (objTmpl == null ? null :
				propHandler.getValue(objTmpl));

			// check if needs to be updated if null
			if ((newPropVal == null) && !propHandler.updateIfNull())
				continue;

			// build property path
			final int parentPropPathLen = propPath.length();
			if (parentPropPathLen > 0)
				propPath.append(".");
			propPath.append(propHandler.getName());
			try {

				// get current property value
				final Object curPropVal = (obj == null ? null :
					propHandler.getValue(obj));

				// different logic depending on property type
				if (propHandler instanceof SimplePropertyHandler) {
					if (!processProperty(resources, paramsFactory,
							JDBCParameterValueGetter.VAL, ctx, obj, propHandler,
							curPropVal, newPropVal))
						continue;
				} else if (propHandler instanceof RefPropertyHandler) {
					if (!processProperty(resources, paramsFactory,
							JDBCParameterValueGetter.REF, ctx, obj, propHandler,
							curPropVal, newPropVal))
						continue;
				} else if (propHandler
								instanceof DependentRefPropertyHandler) {
					if (!processDepRefProperty(obj, propPath,
							(DependentRefPropertyHandler) propHandler,
							curPropVal, newPropVal, depPRsrcIdsToDelete))
						continue;
				} else if (propHandler instanceof ObjectPropertyHandler) {
					if (!processObjectProperty(resources, paramsFactory, ctx,
							obj, propPath, (ObjectPropertyHandler) propHandler,
							curPropVal, newPropVal, actor, updatedProps,
							depPRsrcIdsToDelete))
						continue;
				}

				// updated property ("continue" is used if not updated)
				modified = true;
				if (updatedProps != null)
					updatedProps.add(propPath.toString());

			} finally {
				propPath.setLength(parentPropPathLen);
			}

		} // loop by properties

		// return status if any property is modified
		return modified;
	}

	/**
	 * Process simple value or reference property, determine if it's modified,
	 * add appropriate elements to the statement builder context.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param paramValueGetter Parameter value handlers getter.
	 * @param ctx Current object statement builder context.
	 * @param obj Existing object that contains the property.
	 * @param propHandler Property handler.
	 * @param curPropVal Current property value.
	 * @param newPropVal New property value.
	 *
	 * @return {@code true} if the property is modified.
	 */
	private static boolean processProperty(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final JDBCParameterValueGetter paramValueGetter,
			final StatementBuilderContext ctx, final Object obj,
			final ResourcePropertyHandler propHandler, final Object curPropVal,
			final Object newPropVal) {

		// get property persistence descriptor and table name
		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		final String propTableName = propPersistence.getCollectionName();

		// single-valued or collection?
		if (propHandler.isSingleValued()) {

			if (newPropVal == null) { // setting null?

				// already null?
				if (curPropVal == null)
					return false;

				// stored in its own table?
				if (propTableName != null) {

					// optional join?
					if (propPersistence.isOptional()) {

						// delete value from the value table
						ctx.addDeleteFromValueTable(propTableName,
								propPersistence.getParentIdFieldName(), null,
								null);

					} else {

						// set value in the value table to NULL
						ctx.addUpdateValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getFieldName(),
								paramValueGetter.get(resources, paramsFactory,
										propHandler, null));
					}

				} else { // embedded

					// add to current update as NULL
					ctx.addSet(propPersistence.getFieldName(),
							paramValueGetter.get(resources, paramsFactory,
									propHandler, null));
				}

			} else { // new value

				// value did not change?
				final ValueComparator vc = (newPropVal instanceof BigDecimal ?
						DECIMAL_VALUE_COMPARATOR : SIMPLE_VALUE_COMPARATOR);
				if (vc.equal(newPropVal, curPropVal))
					return false;

				// stored in its own table?
				if (propTableName != null) {

					// optional join and adding new value?
					if (propPersistence.isOptional() && (curPropVal == null)) {

						// insert into value table
						ctx.addInsertIntoValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getFieldName(),
								Collections.singleton(
										paramValueGetter.get(resources,
												paramsFactory, propHandler,
												newPropVal)));

					} else {

						// update existing value table row
						ctx.addUpdateValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getFieldName(),
								paramValueGetter.get(resources,
										paramsFactory, propHandler,
										newPropVal));
					}

				} else { // embedded

					// add to current update
					ctx.addSet(propPersistence.getFieldName(),
							paramValueGetter.get(resources, paramsFactory,
									propHandler, newPropVal));
				}
			}

		} else { // value collection

			// make sure it's in its own table
			if (propTableName == null)
				throw new PersistenceException(
						"Embedded collections are not supported.");

			// clearing it?
			if (newPropVal == null) {

				// check if already empty
				if ((curPropVal == null)
						|| ((curPropVal instanceof Collection)
							&& ((Collection<?>) curPropVal).isEmpty())
						|| ((curPropVal instanceof Map)
							&& ((Map<?, ?>) curPropVal).isEmpty()))
					return false;

				// delete all from the value table
				ctx.addDeleteFromValueTable(propTableName,
						propPersistence.getParentIdFieldName(), null, null);

			} else if (newPropVal instanceof Collection) { // collection
				final Collection<?> newCol = (Collection<?>) newPropVal;
				final Collection<?> curCol = (Collection<?>) curPropVal;

				// check if different
				final int numNewEls = newCol.size();
				if ((curCol == null) || curCol.isEmpty()) {
					if (numNewEls == 0)
						return false;
				} else if (curCol.size() == numNewEls) {

					// get value comparator
					final ValueComparator vc =
						(curCol.iterator().next() instanceof BigDecimal
								? DECIMAL_VALUE_COMPARATOR
								: SIMPLE_VALUE_COMPARATOR);

					// compare collections element by element
					if (compareCollections(newCol, curCol, vc))
						return false;
				}

				// check if needs to be cleared
				if ((curCol != null) && !curCol.isEmpty()) {

					// delete all from the value table
					ctx.addDeleteFromValueTable(propTableName,
							propPersistence.getParentIdFieldName(), null, null);
				}

				// check if needs to be inserted
				if (numNewEls > 0) {

					// insert new elements into the value table
					final Collection<JDBCParameterValue> values =
						new ArrayList<>(numNewEls > 10 ? numNewEls : 10);
					for (final Object newEl : newCol)
						values.add(paramValueGetter.get(resources,
								paramsFactory, propHandler, newEl));
					ctx.addInsertIntoValueTable(propTableName,
							propPersistence.getParentIdFieldName(),
							propPersistence.getFieldName(), values);
				}

			} else if (newPropVal instanceof Map) { // map
				final Map<?, ?> newMap = (Map<?, ?>) newPropVal;
				final Map<?, ?> curMap = (Map<?, ?>) curPropVal;

				// get key info
				final ResourcePropertyValueHandler keyHandler =
					propHandler.getKeyValueHandler();
				final PersistentValueType keyPType =
					keyHandler.getPersistentValueType();
				final boolean refKey = keyHandler.isRef();

				// check if just clear or just insert, or difference
				final int numNewEntries = newMap.size();
				if (numNewEntries == 0) {

					// already empty?
					if ((curMap == null) || curMap.isEmpty())
						return false;

					// delete all from the value table
					ctx.addDeleteFromValueTable(propTableName,
							propPersistence.getParentIdFieldName(), null, null);

				} else if ((curMap == null) || curMap.isEmpty()) {

					// insert new entries into the value table
					final List<List<JDBCParameterValue>> entries =
						new ArrayList<>(
								numNewEntries > 10 ? numNewEntries : 10);
					for (final Map.Entry<?, ?> entry : newMap.entrySet()) {
						final Object key = (refKey
								? ((Ref<?>) entry.getKey()).getId()
								: entry.getKey());
						entries.add(Arrays.asList(
								paramsFactory.getParameterValue(keyPType, key),
								paramValueGetter.get(resources, paramsFactory,
										propHandler, entry.getValue())));
					}
					ctx.addInsertIntoValueTable(propTableName,
							propPersistence.getParentIdFieldName(),
							propPersistence.getKeyFieldName(),
							propPersistence.getFieldName(), entries);

				} else {

					// get value comparator
					final ValueComparator vc =
						(curMap.values().iterator().next() instanceof BigDecimal
								? DECIMAL_VALUE_COMPARATOR
								: SIMPLE_VALUE_COMPARATOR);

					// find differences
					final Set<Object> keysToDelete =
						new HashSet<>(curMap.keySet());
					keysToDelete.removeAll(newMap.keySet());
					final Map<Object, Object> entriesToAdd =
						new HashMap<>(newMap);
					final Map<Object, Object> entriesToUpdate =
						new HashMap<>();
					for (final Iterator<Map.Entry<Object, Object>> i =
							entriesToAdd.entrySet().iterator(); i.hasNext();) {
						final Map.Entry<Object, Object> newEntry = i.next();
						final Object key = newEntry.getKey();
						final Object curEntryVal = curMap.get(key);
						if (curEntryVal != null) {
							i.remove();
							final Object newEntryVal = newEntry.getValue();
							if (!vc.equal(curEntryVal, newEntryVal))
								entriesToUpdate.put(key, newEntryVal);
						}
					}

					// anything to delete?
					boolean different = false;
					final int numKeysToDelete = keysToDelete.size();
					if (numKeysToDelete > 0) {
						different = true;

						// delete removed keys from value table
						final Collection<JDBCParameterValue> keys =
							new ArrayList<>(numKeysToDelete > 10 ?
									numKeysToDelete : 10);
						for (final Object key : keysToDelete)
							keys.add(paramsFactory.getParameterValue(keyPType,
									(refKey ? ((Ref<?>) key).getId() : key)));
						ctx.addDeleteFromValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getKeyFieldName(), keys);
					}

					// anything to update?
					final int numEntriesToUpdate = entriesToUpdate.size();
					if (numEntriesToUpdate > 0) {
						different = true;

						// update value table for modified entries
						final List<List<JDBCParameterValue>> entries =
							new ArrayList<>(numEntriesToUpdate > 10 ?
									numEntriesToUpdate : 10);
						for (final Map.Entry<?, ?> entry :
								entriesToUpdate.entrySet()) {
							final Object key = (refKey
									? ((Ref<?>) entry.getKey()).getId()
									: entry.getKey());
							entries.add(Arrays.asList(
									paramValueGetter.get(resources,
											paramsFactory, propHandler,
											entry.getValue()),
									paramsFactory.getParameterValue(keyPType,
											key)));
						}
						ctx.addUpdateValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getKeyFieldName(),
								propPersistence.getFieldName(), entries);
					}

					// anything to add?
					final int numEntriesToAdd = entriesToAdd.size();
					if (numEntriesToAdd > 0) {
						different = true;

						// insert into value tables each added entry
						final List<List<JDBCParameterValue>> entries =
							new ArrayList<>(numEntriesToAdd > 10 ?
									numEntriesToAdd : 10);
						for (final Map.Entry<?, ?> entry :
								entriesToAdd.entrySet()) {
							final Object key = (refKey
									? ((Ref<?>) entry.getKey()).getId()
									: entry.getKey());
							entries.add(Arrays.asList(
									paramsFactory.getParameterValue(keyPType,
											key),
									paramValueGetter.get(resources,
											paramsFactory, propHandler,
											entry.getValue())));
						}
						ctx.addInsertIntoValueTable(propTableName,
								propPersistence.getParentIdFieldName(),
								propPersistence.getKeyFieldName(),
								propPersistence.getFieldName(), entries);
					}

					// is different?
					if (!different)
						return false;
				}
			}
		}

		// set property in the object
		propHandler.setValue(obj, newPropVal);

		// property will be modified
		return true;
	}

	/**
	 * Compare value collections.
	 *
	 * @param col1 Collection one.
	 * @param col2 Collection two.
	 * @param vc Value comparator to use.
	 *
	 * @return {@code true} if collections are equal.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Comparable<? super T>> boolean compareCollections(
			final Collection<?> col1, final Collection<?> col2,
			final ValueComparator vc) {

		final List<T> sortedCol1 = new ArrayList<>(col1.size());
		for (final Object el : col1)
			sortedCol1.add((T) el);
		Collections.sort(sortedCol1);

		final List<T> sortedCol2 = new ArrayList<>(col2.size());
		for (final Object el : col2)
			sortedCol2.add((T) el);
		Collections.sort(sortedCol2);

		final Iterator<T> i1 = sortedCol1.iterator();
		final Iterator<T> i2 = sortedCol2.iterator();

		while (i1.hasNext()) {
			if (!vc.equal(i1.next(), i2.next()))
				return false;
		}

		return true;
	}

	/**
	 * Recursively process nested object property.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param ctx Current object statement builder context.
	 * @param obj Existing object that contains the property.
	 * @param propPath Path of the nested object property.
	 * @param propHandler Nested object property handler.
	 * @param curPropVal Current property value.
	 * @param newPropVal New property value.
	 * @param actor The actor.
	 * @param updatedProps Set, to which to add paths of the record properties
	 * that were updated, or {@code null} if such information is not needed by
	 * the caller.
	 * @param depPRsrcIdsToDelete Map, to which to add ids of the referred
	 * records that need to be deleted.
	 *
	 * @return {@code true} if the property is modified.
	 */
	private static boolean processObjectProperty(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final StatementBuilderContext ctx, final Object obj,
			final StringBuilder propPath,
			final ObjectPropertyHandler propHandler, final Object curPropVal,
			final Object newPropVal, final Actor actor,
			final Set<String> updatedProps,
			final Map<Class<?>, Set<Object>> depPRsrcIdsToDelete) {

		// get property persistence descriptor and table name
		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		final String propTableName = propPersistence.getCollectionName();

		// single-valued or collection?
		if (propHandler.isSingleValued()) {

			if (newPropVal == null) { // setting null?

				// already null?
				if (curPropVal == null)
					return false;

				// stored in its own table?
				if (propTableName != null) {

					// optional join?
					if (propPersistence.isOptional()) {

						// delete from the nested object table
						ctx.addDeleteNestedObjects(propHandler, null, null);

					} else {
						throw new PersistenceException("Cannot set property "
								+ propPath.toString()
								+ " to null because it is not optional.");
					}

				} else { // embedded

					// clear type property if polymorphic
					final TypePropertyHandler typePropHandler =
						propHandler.getTypeProperty();
					if (typePropHandler != null) {

						// set type field to NULL
						ctx.addSet(
								typePropHandler.getPersistence().getFieldName(),
								paramsFactory.getParameterValue(
										typePropHandler.getValueHandler()
											.getPersistentValueType(),
										null));
					}

					// process object properties within the same context
					if (!processObjectProperties(resources, paramsFactory, ctx,
							propPath, propHandler, curPropVal, null, actor,
							updatedProps, depPRsrcIdsToDelete))
						return false;
				}

			} else if (curPropVal == null) { // current object does not exist

				// stored in its own table?
				if (propTableName != null) {

					// add insert for the object
					ctx.addInsertNestedObjects(propHandler,
							Collections.singleton(newPropVal));

				} else { // embedded

					// set type property if polymorphic
					final TypePropertyHandler typePropHandler =
						propHandler.getTypeProperty();
					if (typePropHandler != null) {

						// set type field
						ctx.addSet(
								typePropHandler.getPersistence().getFieldName(),
								paramsFactory.getParameterValue(
										typePropHandler.getValueHandler()
											.getPersistentValueType(),
										typePropHandler.getValue(newPropVal)));
					}

					// process object properties within the same context
					if (!processObjectProperties(resources, paramsFactory, ctx,
							propPath, propHandler, null, newPropVal, actor,
							updatedProps, depPRsrcIdsToDelete))
						return false;
				}

			} else { // updating single object

				// stored in its own table?
				if (propTableName != null) {

					// get child context
					final StatementBuilderContext propCtx =
						ctx.getChildContext(propHandler, curPropVal, null);

					// process object properties
					if (!processObjectProperties(resources, paramsFactory,
							propCtx, propPath, propHandler, curPropVal,
							newPropVal, actor, updatedProps,
							depPRsrcIdsToDelete))
						return false;

					// add child context to current context
					propCtx.mergeUp();

				} else { // embedded

					// process object properties within the same context
					if (!processObjectProperties(resources, paramsFactory, ctx,
							propPath, propHandler, curPropVal, newPropVal,
							actor, updatedProps, depPRsrcIdsToDelete))
						return false;
				}
			}

		} else { // object collection

			// make sure it's in its own table
			if (propTableName == null)
				throw new PersistenceException(
						"Embedded collections are not supported.");

			// clearing it?
			if (newPropVal == null) {

				// check if already empty
				if ((curPropVal == null)
						|| ((curPropVal instanceof Collection)
							&& ((Collection<?>) curPropVal).isEmpty())
						|| ((curPropVal instanceof Map)
							&& ((Map<?, ?>) curPropVal).isEmpty()))
					return false;

				// delete the objects
				ctx.addDeleteNestedObjects(propHandler, null, null);

			} else if (newPropVal instanceof Collection) { // collection
				final Collection<?> newCol = (Collection<?>) newPropVal;
				final Collection<?> curCol = (Collection<?>) curPropVal;

				// check if currently empty
				if ((curCol == null) || curCol.isEmpty()) {

					// check if new is empty too
					if (newCol.isEmpty())
						return false;

					// insert new elements
					ctx.addInsertNestedObjects(propHandler, newCol);

				} else {

					// arrange existing elements by ids (must have)
					final IdPropertyHandler idPropHandler =
						propHandler.getIdProperty();
					final int curColSize = curCol.size();
					final Map<Object, Object> curElsByIds = new HashMap<>(
							curColSize > 16 ? curColSize : 16);
					for (final Object curEl : curCol)
						curElsByIds.put(idPropHandler.getValue(curEl), curEl);

					// find differences
					final Collection<Object> elsToInsert = new ArrayList<>();
					final List<Object> curElsToCompare = new ArrayList<>();
					final List<Object> newElsToCompare = new ArrayList<>();
					for (final Object newEl : newCol) {
						final Object elId = idPropHandler.getValue(newEl);
						final Object curEl = curElsByIds.remove(elId);
						if (curEl == null) {
							elsToInsert.add(newEl);
						} else {
							curElsToCompare.add(curEl);
							newElsToCompare.add(newEl);
						}
					}

					// anything to delete?
					boolean different = false;
					final int numElsToDelete = curElsByIds.size();
					if (numElsToDelete > 0) {
						different = true;

						// delete remaining ids
						final List<JDBCParameterValue> ids = new ArrayList<>(
								numElsToDelete > 10 ? numElsToDelete : 10);
						final PersistentValueType idPType =
							idPropHandler.getValueHandler()
								.getPersistentValueType();
						for (final Object id : curElsByIds.keySet())
							ids.add(paramsFactory.getParameterValue(idPType,
									id));
						ctx.addDeleteNestedObjects(propHandler,
								idPropHandler.getPersistence().getFieldName(),
								ids);
					}

					// get child context for comparison
					final StatementBuilderContext propCtx =
						ctx.getChildContext(propHandler, null, null);

					// anything to compare/update?
					final int numToCompare = curElsToCompare.size();
					for (int i = 0; i < numToCompare; i++) {

						// reset child context
						final Object curEl = curElsToCompare.get(i);
						propCtx.reset(curEl, null);

						// find differences
						final boolean elDifferent = processObjectProperties(
								resources, paramsFactory, propCtx, propPath,
								propHandler, curEl, newElsToCompare.get(i),
								actor, updatedProps, depPRsrcIdsToDelete);

						// add child context to current context
						if (elDifferent)
							propCtx.mergeUp();

						// update difference flag for the whole collection
						different |= elDifferent;
					}

					// anything to add?
					if (!elsToInsert.isEmpty()) {
						different = true;

						// insert new elements
						ctx.addInsertNestedObjects(propHandler, elsToInsert);
					}

					// is different?
					if (!different)
						return false;
				}

			} else if (newPropVal instanceof Map) { // map
				final Map<?, ?> newMap = (Map<?, ?>) newPropVal;
				final Map<?, ?> curMap = (Map<?, ?>) curPropVal;

				// check if currently empty
				if ((curMap == null) || curMap.isEmpty()) {

					// check if new is empty too
					if (newMap.isEmpty())
						return false;

					// insert new entries
					ctx.addInsertNestedObjects(propHandler, newMap);

				} else {

					// find differences
					final Map<Object, Object> elsToRemove =
						new HashMap<>(curMap);
					final Map<Object, Object> elsToInsert = new HashMap<>();
					final Collection<Object> keysToCompare = new ArrayList<>();
					for (final Map.Entry<?, ?> entry : newMap.entrySet()) {
						final Object key = entry.getKey();
						final Object curEl = elsToRemove.remove(key);
						if (curEl == null)
							elsToInsert.put(key, entry.getValue());
						else
							keysToCompare.add(key);
					}

					// anything to delete?
					boolean different = false;
					final int numElsToRemove = elsToRemove.size();
					if (numElsToRemove > 0) {
						different = true;

						// delete remaining keys
						final List<JDBCParameterValue> keys = new ArrayList<>(
								numElsToRemove > 10 ? numElsToRemove : 10);
						final ResourcePropertyValueHandler keyHandler =
							propHandler.getKeyValueHandler();
						final PersistentValueType keyPType =
							keyHandler.getPersistentValueType();
						final boolean refKey = keyHandler.isRef();
						for (final Object key : elsToRemove.keySet())
							keys.add(paramsFactory.getParameterValue(keyPType,
									(refKey ? ((Ref<?>) key).getId() : key)));
						ctx.addDeleteNestedObjects(propHandler,
								propHandler.getPersistence().getKeyFieldName(),
								keys);
					}

					// get child context for comparison
					final StatementBuilderContext propCtx =
						ctx.getChildContext(propHandler, null, null);

					// anything to compare/update?
					for (final Object key : keysToCompare) {

						// reset child context
						final Object curElVal = curMap.get(key);
						propCtx.reset(curElVal, key);

						// find differences
						final boolean elDifferent = processObjectProperties(
								resources, paramsFactory, propCtx, propPath,
								propHandler, curElVal, newMap.get(key), actor,
								updatedProps, depPRsrcIdsToDelete);

						// add child context to current context
						if (elDifferent)
							propCtx.mergeUp();

						// update difference flag for the whole collection
						different |= elDifferent;
					}

					// anything to add?
					if (!elsToInsert.isEmpty()) {
						different = true;

						// insert new entries
						ctx.addInsertNestedObjects(propHandler, elsToInsert);
					}

					// is different?
					if (!different)
						return false;
				}
			}
		}

		// set property in the object
		propHandler.setValue(obj, newPropVal);

		// property will be modified
		return true;
	}

	/**
	 * Process dependent resource reference property and determine if any
	 * referred resource records need to be deleted.
	 *
	 * @param obj Existing object that contains the property.
	 * @param propPath Dependent resource reference property path.
	 * @param propHandler Dependent resource reference property handler.
	 * @param curPropVal Current property value.
	 * @param newPropVal New property value.
	 * @param depPRsrcIdsToDelete Map, to which to add ids of the referred
	 * records that need to be deleted.
	 *
	 * @return {@code true} if the property is modified.
	 */
	private static boolean processDepRefProperty(final Object obj,
			final StringBuilder propPath,
			final DependentRefPropertyHandler propHandler,
			final Object curPropVal, final Object newPropVal,
			final Map<Class<?>, Set<Object>> depPRsrcIdsToDelete) {

		// check if excluded from update
		if (!propHandler.isFetchedByDefault())
			return false;

		// get referred persistent resource info
		final Class<?> depPRsrcClass =
			propHandler.getReferredResourceClass();

		// single-valued or collection?
		if (propHandler.isSingleValued()) {

			if (newPropVal == null) { // setting null?

				// already null?
				if (curPropVal == null)
					return false;

				// add to dependent resources to delete
				Set<Object> ids = depPRsrcIdsToDelete.get(depPRsrcClass);
				if (ids == null) {
					ids = new HashSet<>();
					depPRsrcIdsToDelete.put(depPRsrcClass, ids);
				}
				ids.add(((Ref<?>) curPropVal).getId());

			} else { // new value

				// make sure not trying to change
				if (newPropVal.equals(curPropVal))
					return false;

				// can't update dependent resource reference
				throw new PersistenceException("Cannot change value of"
						+ " dependent resource reference property "
						+ propPath.toString() + " via update.");
			}

		} else { // collection
			final Collection<?> newCol = (Collection<?>) newPropVal;
			final Collection<?> curCol = (Collection<?>) curPropVal;

			// clearing it?
			if ((newCol == null) || newCol.isEmpty()) {

				// check if already empty
				if ((curCol == null) || curCol.isEmpty())
					return false;

				// add all to dependent resources to delete
				Set<Object> ids = depPRsrcIdsToDelete.get(depPRsrcClass);
				if (ids == null) {
					ids = new HashSet<>();
					depPRsrcIdsToDelete.put(depPRsrcClass, ids);
				}
				for (final Object curRefVal : curCol)
					ids.add(((Ref<?>) curRefVal).getId());

			} else  {

				// make sure not trying to add
				if ((curCol == null) || curCol.isEmpty())
					throw new PersistenceException(
							"Cannot change value of dependent resource"
							+ " reference property " + propPath.toString()
							+ " via update.");

				// find what to delete, make sure not adding anything
				final Set<Object> refsToDelete = new HashSet<>(curCol);
				for (final Object newRefVal : newCol) {
					if (!refsToDelete.remove(newRefVal))
						throw new PersistenceException(
								"Cannot change value of dependent resource"
								+ " reference property " + propPath.toString()
								+ " via update.");
				}

				// anything to delete?
				if (refsToDelete.isEmpty())
					return false;

				// add to dependent resources to delete
				Set<Object> ids = depPRsrcIdsToDelete.get(depPRsrcClass);
				if (ids == null) {
					ids = new HashSet<>();
					depPRsrcIdsToDelete.put(depPRsrcClass, ids);
				}
				for (final Object refVal : refsToDelete)
					ids.add(((Ref<?>) refVal).getId());
			}
		}

		// set property in the object
		propHandler.setValue(obj, newPropVal);

		// property will be modified
		return true;
	}


	/**
	 * Execute the generated statements.
	 *
	 * @param con Database connection.
	 *
	 * @return {@code true} if any properties were updated.
	 *
	 * @throws SQLException If a database error happens.
	 */
	boolean execute(final Connection con)
		throws SQLException {

		if (this.executionPlan.isEmpty())
			return false;

		for (final ExecutionPlanStep step : this.executionPlan)
			step.execute(con);

		return true;
	}
}
