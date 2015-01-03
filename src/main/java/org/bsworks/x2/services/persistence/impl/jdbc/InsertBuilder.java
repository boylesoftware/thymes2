package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.IdHandling;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PersistentResourceHandler;
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
import org.bsworks.x2.util.MutableInt;


/**
 * Builder of a series of "INSERT" statements for creating a persistent resource
 * record.
 *
 * @author Lev Himmelfarb
 */
class InsertBuilder {

	/**
	 * Execution plan step.
	 */
	private static final class ExecutionPlanStep {

		/**
		 * Marks special step for an existing parent record. The step does not
		 * perform an insert and serves as an anchor for subsequent child
		 * inserts that use the existing record's id as the parent record id.
		 */
		final boolean existingRecord;

		/**
		 * The object that is inserted at this execution plan step, or
		 * {@code null} if simple value (normally a collection of simple
		 * values).
		 */
		final Object obj;

		/**
		 * Record id property handler for the object, or {@code null} if none.
		 */
		final IdPropertyHandler idPropHandler;

		/**
		 * Auto-generated record id column name (the record id column name), if
		 * the object has auto-generated id. Otherwise, {@code null}.
		 */
		final String[] generatedIdColName;

		/**
		 * Table id for caching. The statement is always assumed to be the same
		 * for the same table id.
		 */
		final String tableId;

		/**
		 * Table nesting level, 0 for the root resource table.
		 */
		final int tableNestingLevel;

		/**
		 * "INSERT" statement text.
		 */
		final String stmtText;

		/**
		 * Statement parameter values, excluding the parent record id, if any.
		 */
		final List<JDBCParameterValue> params;


		/**
		 * Create new execution plan step.
		 *
		 * @param existingRecord {@code true} if special existing record step.
		 * @param obj The object that is inserted at this execution plan step,
		 * or {@code null} if simple value.
		 * @param idPropHandler Record id property handler for the object, or
		 * {@code null} if none.
		 * @param tableId Table id.
		 * @param tableNestingLevel Property nesting level, 0 for the root
		 * resource table.
		 * @param stmtText "INSERT" statement text.
		 * @param params Statement parameter values, excluding the parent record
		 * id, if any.
		 */
		ExecutionPlanStep(final boolean existingRecord, final Object obj,
				final IdPropertyHandler idPropHandler, final String tableId,
				final int tableNestingLevel, final String stmtText,
				final List<JDBCParameterValue> params) {

			this.existingRecord = existingRecord;
			this.obj = obj;
			this.idPropHandler = idPropHandler;
			if ((idPropHandler != null) && (idPropHandler.getHandling()
					== IdHandling.AUTO_GENERATED))
				this.generatedIdColName = new String[] {
					idPropHandler.getPersistence().getFieldName()
				};
			else
				this.generatedIdColName = null;
			this.tableId = tableId;
			this.tableNestingLevel = tableNestingLevel;
			this.stmtText = stmtText;
			this.params = new ArrayList<>(params);
		}
	}


	/**
	 * "INSERT" statement builder context.
	 */
	private static final class StatementBuilderContext {

		/**
		 * Parent context, or {@code null} for the top.
		 */
		private final StatementBuilderContext parentCtx;

		/**
		 * Tells if special existing record context.
		 */
		private final boolean existingRecord;

		/**
		 * Table id.
		 */
		private final String tableId;

		/**
		 * Table nesting level, 0 for top resource.
		 */
		private final int tableNestingLevel;

		/**
		 * Record id property handler, or {@code null} if none.
		 */
		private final IdPropertyHandler idPropHandler;

		/**
		 * Tells if auto-generated id.
		 */
		private final boolean autoGeneratedId;

		/**
		 * Table name.
		 */
		private final String tableName;

		/**
		 * Parent record id column name, or {@code null} if top resource.
		 */
		private final String parentIdColName;

		/**
		 *  Map key column name, or {@code null} if none.
		 */
		private final String keyColName;

		/**
		 * Columns list builder.
		 */
		private StringBuilder colsList;

		/**
		 * Complete "INSERT" statement if has been already built for the
		 * context's property.
		 */
		private String stmtText;

		/**
		 * The object inserted by the context's statement, or {@code null} if
		 * simple value.
		 */
		private Object obj;

		/**
		 * Parameter values.
		 */
		private final List<JDBCParameterValue> params = new ArrayList<>();

		/**
		 * Nested execution plan.
		 */
		private final List<ExecutionPlanStep> nestedExecutionPlan =
			new ArrayList<>();


		/**
		 * Create new context.
		 *
		 * @param parentCtx Parent context, or {@code null} for the top.
		 * @param existingRecord {@code true} if special existing record
		 * context.
		 * @param tableId Table id.
		 * @param obj The object that is inserted by this context's statement,
		 * or {@code null} if simple value.
		 * @param idPropHandler Record id property handler, or {@code null} if
		 * none.
		 * @param tableName Table name.
		 * @param parentIdColName Parent record id column name, or {@code null}
		 * if top resource.
		 * @param keyColName Map key column name, or {@code null} if none.
		 * @param maxTableNestingLevel Maximum table nesting level to update, if
		 * this context's nesting level is greater.
		 */
		StatementBuilderContext(final StatementBuilderContext parentCtx,
				final boolean existingRecord, final String tableId,
				final Object obj, final IdPropertyHandler idPropHandler,
				final String tableName, final String parentIdColName,
				final String keyColName,
				final MutableInt maxTableNestingLevel) {

			this.existingRecord = existingRecord;
			this.parentCtx = parentCtx;
			this.tableId = tableId;
			this.tableNestingLevel = (parentCtx != null ?
					parentCtx.tableNestingLevel + 1 : 0);
			if (maxTableNestingLevel.get() < this.tableNestingLevel)
				maxTableNestingLevel.set(this.tableNestingLevel);
			this.idPropHandler = idPropHandler;
			this.autoGeneratedId = ((idPropHandler != null)
					&& (idPropHandler.getHandling()
							== IdHandling.AUTO_GENERATED));
			this.tableName = tableName;
			this.parentIdColName = parentIdColName;
			this.keyColName = keyColName;

			this.colsList = new StringBuilder(256);
			this.stmtText = null;
			this.obj = obj;

			if (parentIdColName != null)
				this.colsList.append(parentIdColName);

			if (keyColName != null) {
				if (this.colsList.length() > 0)
					this.colsList.append(", ");
				this.colsList.append(keyColName);
			}
		}


		/**
		 * Reset context and make it ready for re-use.
		 *
		 * @param obj The object that is inserted by this context's statement,
		 * or {@code null} if simple value.
		 */
		void reset(final Object obj) {

			this.obj = obj;
			this.params.clear();
			this.nestedExecutionPlan.clear();
		}

		/**
		 * Get context table id.
		 *
		 * @return Table id.
		 */
		String getTableId() {

			return this.tableId;
		}

		/**
		 * Add map key to the statement.
		 *
		 * @param keyVal Map key value.
		 */
		void addMapKey(final JDBCParameterValue keyVal) {

			this.params.add(keyVal);
		}

		/**
		 * Add column to the statement.
		 *
		 * @param colName Column name.
		 * @param val Column value.
		 */
		void addColumn(final String colName, final JDBCParameterValue val) {

			if (colName.equals(this.parentIdColName)
					|| colName.equals(this.keyColName)
					|| (this.autoGeneratedId && colName.equals(
							this.idPropHandler.getPersistence()
								.getFieldName())))
				return;

			if (this.stmtText == null) {
				if (this.colsList.length() > 0)
					this.colsList.append(", ");
				this.colsList.append(colName);
			}

			this.params.add(val);
		}

		/**
		 * Add execution plan of this context to its parent's execution plan.
		 * The plan steps are placed after the step created by the parent
		 * context.
		 */
		void addToParent() {

			this.toExecutionPlanSteps(this.parentCtx.nestedExecutionPlan);
		}

		/**
		 * Generate execution plan steps from the context.
		 *
		 * @param executionPlan Execution plan, to which to append the generated
		 * steps.
		 */
		void toExecutionPlanSteps(final List<ExecutionPlanStep> executionPlan) {

			if (!this.existingRecord && (this.stmtText == null)) {
				final StringBuilder stmtBuf = new StringBuilder(256);
				stmtBuf.append("INSERT INTO ").append(this.tableName)
					.append(" (").append(this.colsList).append(") VALUES (");
				final int numParams = (this.tableNestingLevel > 0 ?
						this.params.size() + 1 : this.params.size());
				for (int i = 0; i < numParams; i++) {
					if (i > 0)
						stmtBuf.append(", ");
					stmtBuf.append("?");
				}
				stmtBuf.append(")");
				this.stmtText = stmtBuf.toString();
				this.colsList = null;
			}

			executionPlan.add(new ExecutionPlanStep(
					this.existingRecord,
					this.obj,
					this.idPropHandler,
					this.tableId,
					this.tableNestingLevel,
					this.stmtText,
					this.params));

			executionPlan.addAll(this.nestedExecutionPlan);
		}
	}


	/**
	 * Generated execution plan.
	 */
	private final List<ExecutionPlanStep> executionPlan = new ArrayList<>();

	/**
	 * Maximum table nesting level.
	 */
	private final int maxTableNestingLevel;


	/**
	 * Create builder for inserting a persistent resource record.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param prsrcHandler Persistent resource handler.
	 * @param recTmpl Persistent resource record template.
	 * @param actor The actor.
	 */
	InsertBuilder(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final PersistentResourceHandler<?> prsrcHandler,
			final Object recTmpl, final Actor actor) {

		final MutableInt maxTableNestingLevel = new MutableInt(0);

		final StatementBuilderContext topCtx = new StatementBuilderContext(
				null,
				false,
				prsrcHandler.getPersistentCollectionName(),
				recTmpl,
				prsrcHandler.getIdProperty(),
				prsrcHandler.getPersistentCollectionName(),
				null,
				null,
				maxTableNestingLevel);

		addObjectProperties(resources, paramsFactory, actor, topCtx,
				prsrcHandler, recTmpl,
				new HashMap<String, StatementBuilderContext>(),
				maxTableNestingLevel);

		topCtx.toExecutionPlanSteps(this.executionPlan);

		this.maxTableNestingLevel = maxTableNestingLevel.get();
	}

	/**
	 * Create builder for inserting nested objects.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param parentTableName Parent table name.
	 * @param parentObj Parent object.
	 * @param parentIdPropHandler Parent object record id property handler.
	 * @param propHandler Nested object property handler.
	 * @param objTmpls Nested object templates to insert.
	 * @param actor The actor.
	 */
	InsertBuilder(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final String parentTableName, final Object parentObj,
			final IdPropertyHandler parentIdPropHandler,
			final ObjectPropertyHandler propHandler,
			final Collection<?> objTmpls, final Actor actor) {

		final MutableInt maxTableNestingLevel = new MutableInt(0);

		final StatementBuilderContext topCtx = new StatementBuilderContext(
				null,
				true,
				parentTableName,
				parentObj,
				parentIdPropHandler,
				parentTableName,
				null,
				null,
				maxTableNestingLevel);

		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		final String propTableName = propPersistence.getCollectionName();
		final StatementBuilderContext propCtx = new StatementBuilderContext(
				topCtx,
				false,
				parentTableName + "/" + propTableName,
				null,
				propHandler.getIdProperty(),
				propTableName,
				propPersistence.getParentIdFieldName(),
				propPersistence.getKeyFieldName(),
				maxTableNestingLevel);

		final Map<String, StatementBuilderContext> ctxCache = new HashMap<>();
		for (final Object objTmpl : objTmpls) {
			propCtx.reset(objTmpl);
			addObjectProperties(resources, paramsFactory, actor, propCtx,
					propHandler, objTmpl, ctxCache, maxTableNestingLevel);
			propCtx.addToParent();
		}

		topCtx.toExecutionPlanSteps(this.executionPlan);

		this.maxTableNestingLevel = maxTableNestingLevel.get();
	}

	/**
	 * Create builder for inserting nested objects map.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param parentTableName Parent table name.
	 * @param parentObj Parent object.
	 * @param parentIdPropHandler Parent object record id property handler.
	 * @param propHandler Nested object property handler.
	 * @param objTmpls Nested object templates by map keys.
	 * @param actor The actor.
	 */
	InsertBuilder(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory,
			final String parentTableName, final Object parentObj,
			final IdPropertyHandler parentIdPropHandler,
			final ObjectPropertyHandler propHandler, final Map<?, ?> objTmpls,
			final Actor actor) {

		final MutableInt maxTableNestingLevel = new MutableInt(0);

		final StatementBuilderContext topCtx = new StatementBuilderContext(
				null,
				true,
				parentTableName,
				parentObj,
				parentIdPropHandler,
				parentTableName,
				null,
				null,
				maxTableNestingLevel);

		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		final String propTableName = propPersistence.getCollectionName();
		final StatementBuilderContext propCtx = new StatementBuilderContext(
				topCtx,
				false,
				parentTableName + "/" + propTableName,
				null,
				propHandler.getIdProperty(),
				propTableName,
				propPersistence.getParentIdFieldName(),
				propPersistence.getKeyFieldName(),
				maxTableNestingLevel);

		final ResourcePropertyValueHandler keyHandler =
			propHandler.getKeyValueHandler();
		final boolean refKey = keyHandler.isRef();
		final Map<String, StatementBuilderContext> ctxCache = new HashMap<>();
		for (final Map.Entry<?, ?> entry : objTmpls.entrySet()) {
			propCtx.reset(entry.getValue());
			final Object key = (refKey ?
					((Ref<?>) entry.getKey()).getId() : entry.getKey());
			propCtx.addMapKey(paramsFactory.getParameterValue(
					keyHandler.getPersistentValueType(), key));
			addObjectProperties(resources, paramsFactory, actor, propCtx,
					propHandler, entry.getValue(), ctxCache,
					maxTableNestingLevel);
			propCtx.addToParent();
		}

		topCtx.toExecutionPlanSteps(this.executionPlan);

		this.maxTableNestingLevel = maxTableNestingLevel.get();
	}

	/**
	 * Add object properties to the statement builder.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param actor The actor.
	 * @param ctx The statement builder context.
	 * @param objHandler Object handler.
	 * @param obj Object instance. May be {@code null}.
	 * @param ctxCache Statement builder contexts cache.
	 * @param maxTableNestingLevel Maximum table nesting level to update if
	 * necessary.
	 */
	private static void addObjectProperties(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory, final Actor actor,
			final StatementBuilderContext ctx,
			final ResourcePropertiesContainer objHandler, final Object obj,
			final Map<String, StatementBuilderContext> ctxCache,
			final MutableInt maxTableNestingLevel) {

		// add record id column, if any
		final IdPropertyHandler idPropHandler = objHandler.getIdProperty();
		if (idPropHandler != null)
			ctx.addColumn(idPropHandler.getPersistence().getFieldName(),
					JDBCParameterValueGetter.VAL.get(resources, paramsFactory,
							idPropHandler,
							(obj == null ? null :
								idPropHandler.getValue(obj))));

		// add meta-properties if persistent resource
		if (objHandler instanceof PersistentResourceHandler) {
			final PersistentResourceHandler<?> prsrcHandler =
				(PersistentResourceHandler<?>) objHandler;
			for (final MetaPropertyType metaType : MetaPropertyType.values()) {
				final MetaPropertyHandler metaPropHandler =
					prsrcHandler.getMetaProperty(metaType);
				if (metaPropHandler == null)
					continue;
				ctx.addColumn(metaPropHandler.getPersistence().getFieldName(),
						JDBCParameterValueGetter.VAL.get(resources,
								paramsFactory, metaPropHandler,
								(obj == null ? null :
									metaPropHandler.getValue(obj))));
			}
		}

		// add simple properties
		for (final SimplePropertyHandler propHandler :
				objHandler.getSimpleProperties())
			addProperty(resources, paramsFactory, actor, ctx, propHandler, obj,
					ctxCache, JDBCParameterValueGetter.VAL,
					maxTableNestingLevel);

		// add reference properties
		for (final RefPropertyHandler propHandler :
				objHandler.getRefProperties())
			addProperty(resources, paramsFactory, actor, ctx, propHandler, obj,
					ctxCache, JDBCParameterValueGetter.REF,
					maxTableNestingLevel);

		// get type property of a polymorphic object
		final TypePropertyHandler typePropHandler;
		final Object typeVal;
		if (objHandler instanceof ObjectPropertyHandler) {
			typePropHandler =
				((ObjectPropertyHandler) objHandler).getTypeProperty();
			if ((typePropHandler != null) && (obj != null))
				typeVal = typePropHandler.getValue(obj);
			else
				typeVal = null;
		} else {
			typePropHandler = null;
			typeVal = null;
		}

		// add type property if it is persistent
		if (typePropHandler != null) {
			final ResourcePropertyPersistence typePropPersistence =
				typePropHandler.getPersistence();
			if (typePropPersistence != null) {
				ctx.addColumn(typePropPersistence.getFieldName(),
						JDBCParameterValueGetter.VAL.get(resources,
								paramsFactory, typePropHandler, typeVal));
			}
		}

		// add nested object properties
		for (final ObjectPropertyHandler propHandler :
				objHandler.getObjectProperties()) {

			// check it persistent and is allowed to persist
			if (!propHandler.isAllowed(ResourcePropertyAccess.PERSIST, actor))
				continue;

			// get property value
			final Object propVal;
			if (propHandler.isConcreteType() && (typePropHandler != null)) {
				if (propHandler.getName().equals(
						typePropHandler.getValueHandler().toString(typeVal))) {
					propVal = obj;
				} else {
					propVal = null;
				}
			} else {
				propVal = (obj == null ? null : propHandler.getValue(obj));
			}

			// check if embedded
			final ResourcePropertyPersistence propPersistence =
				propHandler.getPersistence();
			final String propTableName = propPersistence.getCollectionName();
			if (propTableName == null) {

				// check that not an embedded collection
				if (!propHandler.isSingleValued())
					throw new PersistenceException(
							"Embedded collections are not supported.");

				// add object properties to the same statement builder
				addObjectProperties(resources, paramsFactory, actor, ctx,
						propHandler, propVal, ctxCache, maxTableNestingLevel);

			} else if (propVal != null) { // not embedded

				// create context for nested object insert
				final String propTableId =
					ctx.getTableId() + "/" + propTableName;
				StatementBuilderContext propCtx = ctxCache.get(propTableId);
				if (propCtx == null) {
					propCtx = new StatementBuilderContext(
							ctx,
							false,
							propTableId,
							propVal,
							propHandler.getIdProperty(),
							propTableName,
							propPersistence.getParentIdFieldName(),
							propPersistence.getKeyFieldName(),
							maxTableNestingLevel);
					ctxCache.put(propTableId, propCtx);
				} else {
					propCtx.reset(propVal);
				}

				// single-valued, collection or map?
				if (propHandler.isSingleValued()) {

					addObjectProperties(resources, paramsFactory, actor,
							propCtx, propHandler, propVal, ctxCache,
							maxTableNestingLevel);
					propCtx.addToParent();

				} else if (propVal instanceof Collection) {

					for (final Object elVal : (Collection<?>) propVal) {
						propCtx.reset(elVal);
						addObjectProperties(resources, paramsFactory, actor,
								propCtx, propHandler, elVal, ctxCache,
								maxTableNestingLevel);
						propCtx.addToParent();
					}

				} else if (propVal instanceof Map) { // should always be

					for (final Map.Entry<?, ?> entry :
							((Map<?, ?>) propVal).entrySet()) {
						propCtx.reset(entry.getValue());
						propCtx.addMapKey(paramsFactory.getParameterValue(
								propHandler.getKeyValueHandler()
									.getPersistentValueType(),
								entry.getKey()));
						addObjectProperties(resources, paramsFactory, actor,
								propCtx, propHandler, entry.getValue(),
								ctxCache, maxTableNestingLevel);
						propCtx.addToParent();
					}
				}
			}
		}
	}

	/**
	 * Add simple or reference property to the statement builder.
	 *
	 * @param resources Application resources manager.
	 * @param paramsFactory Parameter value handlers factory.
	 * @param actor The actor.
	 * @param ctx The statement builder context.
	 * @param propHandler The property handler.
	 * @param obj Object that contains the property. May be {@code null}, in
	 * which case the property value is assumed to be {@code null}.
	 * @param ctxCache Statement builder contexts cache.
	 * @param paramValueGetter Parameter value getter implementation.
	 * @param maxTableNestingLevel Maximum table nesting level to update if
	 * necessary.
	 */
	private static void addProperty(final Resources resources,
			final ParameterValuesFactoryImpl paramsFactory, final Actor actor,
			final StatementBuilderContext ctx,
			final ResourcePropertyHandler propHandler, final Object obj,
			final Map<String, StatementBuilderContext> ctxCache,
			final JDBCParameterValueGetter paramValueGetter,
			final MutableInt maxTableNestingLevel) {

		// check it the property is persistent and is allowed to persist
		if (!propHandler.isAllowed(ResourcePropertyAccess.PERSIST, actor))
			return;

		// get property value
		final Object propVal = (obj == null ? null : propHandler.getValue(obj));

		// check if stored in its own table
		final ResourcePropertyPersistence propPersistence =
			propHandler.getPersistence();
		final String propTableName = propPersistence.getCollectionName();
		if (propTableName != null) {

			// create context for property table insert
			final String propTableId = ctx.getTableId() + "/" + propTableName;
			StatementBuilderContext propCtx = ctxCache.get(propTableId);
			if (propCtx == null) {
				propCtx = new StatementBuilderContext(
						ctx,
						false,
						propTableId,
						null,
						null,
						propTableName,
						propPersistence.getParentIdFieldName(),
						propPersistence.getKeyFieldName(),
						maxTableNestingLevel);
				ctxCache.put(propTableId, propCtx);
			} else {
				propCtx.reset(null);
			}

			// single-valued, collection or map?
			if (propHandler.isSingleValued()) {

				propCtx.addColumn(propPersistence.getFieldName(),
						paramValueGetter.get(resources, paramsFactory,
								propHandler, propVal));
				propCtx.addToParent();

			} else if (propVal instanceof Collection) {

				for (final Object elVal : (Collection<?>) propVal) {
					propCtx.reset(null);
					propCtx.addColumn(propPersistence.getFieldName(),
							paramValueGetter.get(resources, paramsFactory,
									propHandler, elVal));
					propCtx.addToParent();
				}

			} else if (propVal instanceof Map) { // should always be

				for (final Map.Entry<?, ?> entry :
						((Map<?, ?>) propVal).entrySet()) {
					propCtx.reset(null);
					propCtx.addMapKey(paramsFactory.getParameterValue(
							propHandler.getKeyValueHandler()
								.getPersistentValueType(),
							entry.getKey()));
					propCtx.addColumn(propPersistence.getFieldName(),
							paramValueGetter.get(resources, paramsFactory,
									propHandler, entry.getValue()));
					propCtx.addToParent();
				}
			}

		} else { // stored in the container's table

			// check that not an embedded collection
			if (!propHandler.isSingleValued())
				throw new PersistenceException(
						"Embedded collections are not supported.");

			// add property column to the container table's insert
			ctx.addColumn(propPersistence.getFieldName(),
					paramValueGetter.get(resources, paramsFactory, propHandler,
							propVal));
		}
	}


	/**
	 * Execute the generated statements.
	 *
	 * @param con Database connection.
	 *
	 * @throws SQLException If a database error happens.
	 */
	@SuppressWarnings("resource") // prepared statements are closed in finally
	void execute(final Connection con)
		throws SQLException {

		// the log
		final Log log = LogFactory.getLog(this.getClass());
		final boolean debug = log.isDebugEnabled();

		// parent record ids chain
		final Object[] parentIdsChain =
			new Object[this.maxTableNestingLevel + 1];

		// create reusable prepared statements cache
		final Map<String, PreparedStatement> pstmts = new HashMap<>();
		try {

			// execute generated plan steps
			for (final ExecutionPlanStep step : this.executionPlan) {

				// check if special existing record step
				if (step.existingRecord) {
					parentIdsChain[step.tableNestingLevel] =
						step.idPropHandler.getValue(step.obj);
					continue;
				}

				// get prepared statement
				PreparedStatement pstmt = pstmts.get(step.tableId);
				if (pstmt == null) {
					pstmt = (step.generatedIdColName != null
							? con.prepareStatement(step.stmtText,
									step.generatedIdColName)
							: con.prepareStatement(step.stmtText));
					pstmts.put(step.tableId, pstmt);
				}

				// parameter counter
				int nextParamInd = 1;

				// set parent id parameter
				if (step.tableNestingLevel > 0)
					pstmt.setObject(nextParamInd++,
							parentIdsChain[step.tableNestingLevel - 1]);

				// set parameters
				for (final JDBCParameterValue paramVal : step.params)
					nextParamInd = paramVal.set(pstmt, nextParamInd);

				// execute the statement
				if (debug)
					log.debug("executing SQL query:\n" + step.stmtText
							+ (step.tableNestingLevel == 0 ? "" :
								"\nparent record id: "
								+ parentIdsChain[step.tableNestingLevel - 1])
							+ "\nparams: " + step.params);
				pstmt.execute();
				Utils.logWarnings(log, pstmt.getWarnings());

				// get new record id
				final Object newRecId;
				if (step.generatedIdColName != null) { // auto-generated id

					// get generated record id
					try (final ResultSet rs = pstmt.getGeneratedKeys()) {
						rs.next();
						Utils.logWarnings(log, rs.getWarnings());
						newRecId = step.idPropHandler.getValueHandler().valueOf(
								rs.getString(1));
					} catch (final InvalidResourceDataException e) {
						throw new RuntimeException(
								"Invalid generated record id value.", e);
					}
					if (debug)
						log.debug("auto-generated record id: " + newRecId
								+ " (" + newRecId.getClass().getName() + ")");

					// set record id in the object
					step.idPropHandler.setValue(step.obj, newRecId);

				} else if (step.idPropHandler != null) { // assigned record id

					newRecId = step.idPropHandler.getValue(step.obj);

				} else { // no record id

					newRecId = null;
				}

				// save record id in the parent ids chain
				parentIdsChain[step.tableNestingLevel] = newRecId;
			}

		} finally {
			for (final PreparedStatement pstmt : pstmts.values())
				pstmt.close();
		}
	}
}
