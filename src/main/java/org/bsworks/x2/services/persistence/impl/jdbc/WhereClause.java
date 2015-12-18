package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterCondition;
import org.bsworks.x2.resource.FilterConditionOperand;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyPersistence;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.util.StringUtils;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Represents persistent resource fetch query "WHERE" clause.
 *
 * @author Lev Himmelfarb
 */
class WhereClause {

	/**
	 * Maker of an expression with two operands.
	 */
	private static interface BinaryExprFactory {

		/**
		 * Make expression.
		 *
		 * @param dialect SQL dialect.
		 * @param op1 Left operand.
		 * @param op2 Right operand.
		 *
		 * @return The expression.
		 */
		String make(SQLDialect dialect, String op1, String op2);
	}

	/**
	 * "Less than" expression factory.
	 */
	private static final BinaryExprFactory EXPR_LT = new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return op1 + " < " + op2;
		}
	};

	/**
	 * "Less than or equal" expression factory.
	 */
	private static final BinaryExprFactory EXPR_LE = new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return op1 + " <= " + op2;
		}
	};

	/**
	 * "Greater than" expression factory.
	 */
	private static final BinaryExprFactory EXPR_GT = new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return op1 + " > " + op2;
		}
	};

	/**
	 * "Greater than or equal" expression factory.
	 */
	private static final BinaryExprFactory EXPR_GE = new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return op1 + " >= " + op2;
		}
	};

	/**
	 * "Match" expression factory.
	 */
	private static final BinaryExprFactory EXPR_MATCH =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.regularExpressionMatch(op1, op2, false, false);
		}
	};

	/**
	 * "Not match" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_MATCH =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.regularExpressionMatch(op1, op2, true, false);
		}
	};

	/**
	 * Case-sensitive "match" expression factory.
	 */
	private static final BinaryExprFactory EXPR_MATCH_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.regularExpressionMatch(op1, op2, false, true);
		}
	};

	/**
	 * Case-sensitive "not match" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_MATCH_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.regularExpressionMatch(op1, op2, true, true);
		}
	};

	/**
	 * "Substring" expression factory.
	 */
	private static final BinaryExprFactory EXPR_SUBSTRING =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.substringMatch(op1, op2, false, false);
		}
	};

	/**
	 * "Not substring" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_SUBSTRING =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.substringMatch(op1, op2, true, false);
		}
	};

	/**
	 * Case-sensitive "substring" expression factory.
	 */
	private static final BinaryExprFactory EXPR_SUBSTRING_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.substringMatch(op1, op2, false, true);
		}
	};

	/**
	 * Case-sensitive "not substring" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_SUBSTRING_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.substringMatch(op1, op2, true, true);
		}
	};

	/**
	 * "Prefix" expression factory.
	 */
	private static final BinaryExprFactory EXPR_PREFIX =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.prefixMatch(op1, op2, false, false);
		}
	};

	/**
	 * "Not prefix" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_PREFIX =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.prefixMatch(op1, op2, true, false);
		}
	};

	/**
	 * Case-sensitive "prefix" expression factory.
	 */
	private static final BinaryExprFactory EXPR_PREFIX_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.prefixMatch(op1, op2, false, true);
		}
	};

	/**
	 * Case-sensitive "not prefix" expression factory.
	 */
	private static final BinaryExprFactory EXPR_NOT_PREFIX_CS =
		new BinaryExprFactory() {
		@Override
		public String make(final SQLDialect dialect, final String op1,
				final String op2) {

			return dialect.prefixMatch(op1, op2, true, true);
		}
	};


	/**
	 * Sub-query builder.
	 */
	private static final class SubQueryBuilder {

		/**
		 * Collection property stump.
		 */
		final CollectionQueryProperty stump;

		/**
		 * Body of the "FROM" clause.
		 */
		final StringBuilder fromClause = new StringBuilder(64);

		/**
		 * Body of the "WHERE" clause with the conditions for joining tables.
		 */
		final StringBuilder whereClauseJoins = new StringBuilder(64);

		/**
		 * Body of the "WHERE" clause with conditions for the filter.
		 */
		final StringBuilder whereClauseConditions = new StringBuilder(64);

		/**
		 * Aliases of tables included in the "FROM" clause by table names.
		 */
		final Map<String, String> includedTableAliases = new HashMap<>();

		/**
		 * Suffix for the next table alias.
		 */
		char nextTableAliasChar = 'a';


		/**
		 * Create new sub-query builder.
		 *
		 * @param stump Collection property stump.
		 */
		SubQueryBuilder(final CollectionQueryProperty stump) {

			this.stump = stump;
			this.fromClause.append(stump.getCollectionTableName())
				.append(" AS ").append(stump.getCollectionTableAlias());
			this.whereClauseJoins.append(
					stump.getCollectionTableJoinCondition());
			this.includedTableAliases.put(stump.getCollectionTableName(),
					stump.getCollectionTableAlias());
		}


		/**
		 * Build sub-query and append it to the specified buffer.
		 *
		 * @param buf Buffer, to which to append the sub-query.
		 * @param whereClauseBuf Re-usable buffer for internal use by the method
		 * for building the sub-query's "WHERE" clause.
		 */
		void appendSubQuery(final StringBuilder buf,
				final StringBuilder whereClauseBuf) {

			buf.append("SELECT 1 FROM ").append(this.fromClause);

			whereClauseBuf.setLength(0);
			if (this.whereClauseJoins.length() > 0)
				whereClauseBuf.append(this.whereClauseJoins);
			if (this.whereClauseConditions.length() > 0) {
				if (this.whereClauseJoins.length() > 0)
					whereClauseBuf.append(" AND ");
				whereClauseBuf.append("(").append(this.whereClauseConditions)
					.append(")");
			}
			if (whereClauseBuf.length() > 0)
				buf.append(" WHERE ").append(whereClauseBuf);
		}
	}


	/**
	 * The clause body.
	 */
	private final String body;

	/**
	 * Paths of used properties that need joins.
	 */
	private final Set<String> usedJoins = new HashSet<>();


	/**
	 * Create new "WHERE" clause.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param filter Filter specification.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param collectionProps Collection property stumps from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 */
	WhereClause(final Resources resources, final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final FilterSpec<?> filter, final String paramPrefix,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final Map<String, CollectionQueryProperty> collectionProps,
			final SortedMap<String, String> allSingleJoins,
			final Map<String, JDBCParameterValue> params) {

		final StringBuilder body = new StringBuilder(256);
		buildFilterExpression(resources, body, paramPrefix, 0, dialect,
				paramsFactory, filter, singlePropExprs, collectionProps,
				allSingleJoins, params, this.usedJoins);
		this.body = body.toString();
	}

	/**
	 * Build filter expression.
	 *
	 * @param resources Application resources manager.
	 * @param buf Filter expression builder.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param nextParamInd Index for the next query parameter placeholder name.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param filter Filter specification.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param collectionProps Collection property stumps from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 * @param usedJoins Set, to which to add used joins.
	 *
	 * @return Index for the next query parameter placeholder name.
	 */
	private static int buildFilterExpression(final Resources resources,
			final StringBuilder buf, final String paramPrefix,
			final int nextParamInd, final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final FilterSpec<?> filter,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final Map<String, CollectionQueryProperty> collectionProps,
			final SortedMap<String, String> allSingleJoins,
			final Map<String, JDBCParameterValue> params,
			final Set<String> usedJoins) {

		// add filter conditions
		int newNextParamInd = nextParamInd;
		final boolean disjunction = filter.isDisjunction();
		final Map<String, SubQueryBuilder> pSubqueries = new HashMap<>();
		final Map<String, SubQueryBuilder> nSubqueries = new HashMap<>();
		final StringBuilder propPathBuf = new StringBuilder(128);
		for (final FilterCondition cond : filter.getConditions()) {

			// get property path and initialize property path buffer
			final String propPath = cond.getPropertyPath();
			propPathBuf.setLength(0);
			propPathBuf.append(propPath);

			// check if single-valued or collection
			final SingleValuedQueryProperty prop =
				singlePropExprs.get(propPath);
			if (prop != null) { // single-valued

				// get used joins
				int dotInd = propPathBuf.length();
				do {
					propPathBuf.setLength(dotInd);
					final String propPathPrefix = propPathBuf.toString();
					if (allSingleJoins.containsKey(propPathPrefix))
						usedJoins.add(propPathPrefix);
				} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);

				// add the condition
				newNextParamInd = appendCondition(buf, paramPrefix,
						newNextParamInd, dialect, paramsFactory, disjunction,
						cond, prop.getValueExpression(), prop.getValueType(),
						params);

			} else { // collection

				// find the stump and get used joins
				SubQueryBuilder subquery = null;
				int dotInd = propPathBuf.length();
				do {
					propPathBuf.setLength(dotInd);
					final String propPathPrefix = propPathBuf.toString();
					if (subquery == null) {
						final CollectionQueryProperty stump =
							collectionProps.get(propPathPrefix);
						if (stump == null)
							continue;
						final Map<String, SubQueryBuilder> subqueries =
							(cond.isNegated() ? nSubqueries : pSubqueries);
						subquery = subqueries.get(propPathPrefix);
						if (subquery != null)
							break;
						subquery = new SubQueryBuilder(stump);
						subqueries.put(propPathPrefix, subquery);
					} else {
						if (allSingleJoins.containsKey(propPathPrefix))
							usedJoins.add(propPathPrefix);
					}
				} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);
				if (subquery == null) // shouldn't happen
					throw new RuntimeException("Could not find collection stump"
							+ " for property " + propPath + ".");

				// add condition to the sub-query
				newNextParamInd = appendConditionToSubquery(resources, subquery,
						paramPrefix, newNextParamInd, dialect, paramsFactory,
						disjunction, cond, params);
			}
		}

		// add collection conditions
		final StringBuilder subqueryWhereClause = new StringBuilder(128);
		for (final SubQueryBuilder subquery : pSubqueries.values()) {
			if (buf.length() > 0)
				buf.append(disjunction ? " OR " : " AND ");
			buf.append("EXISTS (");
			subquery.appendSubQuery(buf, subqueryWhereClause);
			buf.append(")");
		}
		for (final SubQueryBuilder subquery : nSubqueries.values()) {
			if (buf.length() > 0)
				buf.append(disjunction ? " OR " : " AND ");
			buf.append("NOT EXISTS (");
			subquery.appendSubQuery(buf, subqueryWhereClause);
			buf.append(")");
		}

		// add sub-junctions
		for (final FilterSpec<?> junc : filter.getJunctions()) {
			if (buf.length() > 0)
				buf.append(disjunction ? " OR " : " AND ");
			final boolean diffType = (junc.isDisjunction() != disjunction);
			if (diffType)
				buf.append("(");
			final StringBuilder juncBody = new StringBuilder(256);
			newNextParamInd = buildFilterExpression(resources, juncBody,
					paramPrefix, newNextParamInd, dialect, paramsFactory, junc,
					singlePropExprs, collectionProps, allSingleJoins, params,
					usedJoins);
			buf.append(juncBody);
			if (diffType)
				buf.append(")");
		}

		// done, return next parameter index
		return newNextParamInd;
	}

	/**
	 * Append condition to the sub-query.
	 *
	 * @param resources Application resources manager.
	 * @param subquery The sub-query builder.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param nextParamInd Index for the next query parameter placeholder name.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param disjunction {@code true} if the condition is part of a
	 * disjunction.
	 * @param cond The condition descriptor.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 *
	 * @return Index for the next query parameter placeholder name.
	 */
	private static int appendConditionToSubquery(final Resources resources,
			final SubQueryBuilder subquery, final String paramPrefix,
			final int nextParamInd, final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final boolean disjunction, final FilterCondition cond,
			final Map<String, JDBCParameterValue> params) {

		// find the stump property in the condition property chain
		final Iterator<? extends ResourcePropertyHandler> propChainI =
				cond.getPropertyChain().iterator();
		ResourcePropertyHandler propHandler = propChainI.next();
		while (propHandler != subquery.stump.getPropHandler())
			propHandler = propChainI.next();

		// add tables to the sub-query
		String propTableAlias = subquery.stump.getCollectionTableAlias();
		String propValExpr = subquery.stump.getValueExpression();
		String propKeyExpr = subquery.stump.getKeyExpression();
		ResourcePropertyHandler prevPropHandler = null;
		while (propChainI.hasNext()) {
			prevPropHandler = propHandler;
			propHandler = propChainI.next();

			// get property persistence
			final ResourcePropertyPersistence propPersistence =
				propHandler.getPersistence();

			// check if transient type property
			if (propPersistence == null) {

				// build type value expression and add concrete type tables
				final Collection<? extends ObjectPropertyHandler> valueTypes =
					((ObjectPropertyHandler) prevPropHandler)
						.getObjectProperties();
				final StringBuilder typeValExpr = new StringBuilder(256);
				typeValExpr.append("(CASE");
				for (final ObjectPropertyHandler valueType : valueTypes) {
					final String typeTableAlias =
						"c" + (subquery.nextTableAliasChar++);
					final ResourcePropertyPersistence valueTypePersistence =
						valueType.getPersistence();
					subquery.fromClause.append(" LEFT OUTER JOIN ")
						.append(valueTypePersistence.getCollectionName())
						.append(" AS ").append(typeTableAlias).append(" ON ")
						.append(typeTableAlias).append(".")
						.append(valueTypePersistence.getParentIdFieldName())
						.append(" = ").append(propValExpr);
					typeValExpr.append(" WHEN ").append(typeTableAlias)
						.append(".")
						.append(valueTypePersistence.getParentIdFieldName())
						.append(" IS NOT NULL THEN '")
						.append(valueType.getName()).append("'");
				}
				typeValExpr.append(" END)");
				propValExpr = typeValExpr.toString();
				propKeyExpr = null;

				// can be only last property
				break;
			}

			// add table from property persistence
			final String propTable = propPersistence.getCollectionName();
			if (propTable != null) {
				propTableAlias = subquery.includedTableAliases.get(propTable);
				if (propTableAlias == null) {
					propTableAlias = "c" + (subquery.nextTableAliasChar++);
					subquery.fromClause.append(", ").append(propTable)
						.append(" AS ").append(propTableAlias);
					subquery.whereClauseJoins.append(" AND ")
						.append(propTableAlias).append(".")
						.append(propPersistence.getParentIdFieldName())
						.append(" = ").append(propValExpr);
					subquery.includedTableAliases.put(propTable,
							propTableAlias);
				}
			}

			// get value and key expressions depending on the property type
			if (propHandler instanceof ObjectPropertyHandler) {
				final ObjectPropertyHandler objPropHandler =
					(ObjectPropertyHandler) propHandler;

				if (propTable != null) { // not embedded
					final IdPropertyHandler idPropHandler =
						objPropHandler.getIdProperty();
					propValExpr = propTableAlias + "."
						+ (idPropHandler != null
							? idPropHandler.getPersistence().getFieldName()
							: propPersistence.getParentIdFieldName());
					propKeyExpr = propPersistence.getKeyFieldName();
				}

			} else if (propHandler instanceof RefPropertyHandler) {

				propValExpr =
					propTableAlias + "." + propPersistence.getFieldName();
				propKeyExpr = propPersistence.getKeyFieldName();

				if (propChainI.hasNext()) {
					final PersistentResourceHandler<?> targetHandler =
						resources.getPersistentResourceHandler(
								((RefPropertyHandler) propHandler)
									.getReferredResourceClass());
					propTableAlias = "c" + (subquery.nextTableAliasChar++);
					subquery.fromClause.append(", ")
						.append(targetHandler.getPersistentCollectionName())
						.append(" AS ").append(propTableAlias);
					subquery.whereClauseJoins.append(" AND ")
						.append(propTableAlias).append(".")
						.append(targetHandler.getIdProperty().getPersistence()
								.getFieldName())
						.append(" = ").append(propValExpr);
					subquery.includedTableAliases.put(
							targetHandler.getPersistentCollectionName(),
							propTableAlias);
				}

			} else if (propHandler instanceof DependentRefPropertyHandler) {

				final PersistentResourceHandler<?> targetHandler =
					resources.getPersistentResourceHandler(
							((DependentRefPropertyHandler) propHandler)
								.getReferredResourceClass());

				propValExpr = propTableAlias + "."
						+ targetHandler.getIdProperty().getPersistence()
							.getFieldName();
				propKeyExpr = null;

			} else {
				propValExpr =
					propTableAlias + "." + propPersistence.getFieldName();
				propKeyExpr = propPersistence.getKeyFieldName();
			}
		}

		// no need for condition if value presence test
		final FilterConditionType condType = cond.getType();
		if ((condType == FilterConditionType.EMPTY)
				|| (condType == FilterConditionType.NOT_EMPTY))
			return nextParamInd;

		// determine value expression and type
		final String valueExpr;
		final PersistentValueType valueType;
		switch (cond.getPropertyValueType()) {
		case VALUE:
		case ID:
			valueExpr = propValExpr;
			valueType = propHandler.getValueHandler().getPersistentValueType();
			break;
		case KEY:
			valueExpr = propKeyExpr;
			valueType =
				propHandler.getKeyValueHandler().getPersistentValueType();
			break;
		default: // cannot be
			throw new RuntimeException("Invalid condition value type.");
		}

		// append condition to the sub-query
		return appendCondition(subquery.whereClauseConditions, paramPrefix,
				nextParamInd, dialect, paramsFactory, disjunction, cond,
				valueExpr, valueType, params);
	}

	/**
	 * Append condition to the filter expression. The method also applies value
	 * transformation function from the filter condition if any.
	 *
	 * @param buf Filter expression builder.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param nextParamInd Index for the next query parameter placeholder name.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param disjunction {@code true} if the filter expression is a
	 * disjunction.
	 * @param cond The condition descriptor.
	 * @param rawValueExpr Value expression that the condition tests.
	 * @param rawValueType Value type.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 *
	 * @return Index for the next query parameter placeholder name.
	 */
	private static int appendCondition(final StringBuilder buf,
			final String paramPrefix, final int nextParamInd,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final boolean disjunction, final FilterCondition cond,
			final String rawValueExpr, final PersistentValueType rawValueType,
			final Map<String, JDBCParameterValue> params) {

		final String valueExpr = Utils.getTransformedValueExpression(dialect,
				rawValueExpr, rawValueType, cond.getValueFunction(),
				cond.getValueFunctionParams());
		final PersistentValueType valueType = Utils.getTransformedValueType(
				rawValueType, cond.getValueFunction());

		if (buf.length() > 0)
			buf.append(disjunction ? " OR " : " AND ");

		final FilterConditionType condType = cond.getType();

		final boolean convertToString = (condType.requiresString()
				&& (valueType != PersistentValueType.STRING));

		int newNextParamInd = nextParamInd;
		final Collection<? extends FilterConditionOperand> operands =
			cond.getOperands();
		switch (condType) {
		case EQ:
			newNextParamInd = appendInCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory, cond.isNegated(),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NE:
			newNextParamInd = appendInCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory, !cond.isNegated(),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case LT:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_LT : EXPR_GE), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case LE:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_LE : EXPR_GT), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case GT:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_GT : EXPR_LE), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case GE:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_GE : EXPR_LT), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case MATCH:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_MATCH : EXPR_NOT_MATCH), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case NOT_MATCH:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_MATCH : EXPR_MATCH), operands,
					valueExpr, valueType, convertToString, params);
			break;
		case MATCH_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_MATCH_CS : EXPR_NOT_MATCH_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NOT_MATCH_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_MATCH_CS : EXPR_MATCH_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case SUBSTRING:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_SUBSTRING : EXPR_NOT_SUBSTRING),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NOT_SUBSTRING:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_SUBSTRING : EXPR_SUBSTRING),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case SUBSTRING_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_SUBSTRING_CS :
						EXPR_NOT_SUBSTRING_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NOT_SUBSTRING_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_SUBSTRING_CS :
						EXPR_SUBSTRING_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case PREFIX:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_PREFIX : EXPR_NOT_PREFIX),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NOT_PREFIX:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_PREFIX : EXPR_PREFIX),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case PREFIX_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_PREFIX_CS : EXPR_NOT_PREFIX_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case NOT_PREFIX_CS:
			newNextParamInd = appendBinaryCondition(buf, paramPrefix,
					newNextParamInd, dialect, paramsFactory,
					(!cond.isNegated() ? EXPR_NOT_PREFIX_CS : EXPR_PREFIX_CS),
					operands, valueExpr, valueType, convertToString, params);
			break;
		case EMPTY:
			buf.append(valueExpr)
				.append(!cond.isNegated() ? " IS NULL" : " IS NOT NULL");
			break;
		case NOT_EMPTY:
			buf.append(valueExpr)
				.append(!cond.isNegated() ? " IS NOT NULL" : " IS NULL");
			break;
		default:
		}

		return newNextParamInd;
	}

	/**
	 * Append binary operator condition to the filter expression.
	 *
	 * @param buf Filter expression builder.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param nextParamInd Index for the next query parameter placeholder name.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param opFactory Operator factory.
	 * @param operands Operands.
	 * @param valueExpr Value expression that the condition tests.
	 * @param valueType Value type.
	 * @param convertToString {@code true} to explicitly convert the values to a
	 * string type.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 *
	 * @return Index for the next query parameter placeholder name.
	 */
	private static int appendBinaryCondition(final StringBuilder buf,
			final String paramPrefix, final int nextParamInd,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final BinaryExprFactory opFactory,
			final Collection<? extends FilterConditionOperand> operands,
			final String valueExpr, final PersistentValueType valueType,
			final boolean convertToString,
			final Map<String, JDBCParameterValue> params) {

		int newNextParamInd = nextParamInd;
		if (operands.size() == 1) {
			final FilterConditionOperand operand = operands.iterator().next();
			final String paramName = paramPrefix + (newNextParamInd++);
			if (convertToString) {
				buf.append(opFactory.make(dialect,
						dialect.castToString(valueExpr), "?" + paramName));
				params.put(paramName, paramsFactory.getParameterValue(
						PersistentValueType.STRING,
						StringUtils.asString(operand.getValue())));
			} else {
				buf.append(opFactory.make(dialect,
						valueExpr, "?" + paramName));
				params.put(paramName, paramsFactory.getParameterValue(valueType,
						operand.getValue()));
			}
		} else {
			buf.append("(");
			for (final Iterator<? extends FilterConditionOperand> i =
					operands.iterator(); i.hasNext();) {
				final FilterConditionOperand operand = i.next();
				if (newNextParamInd > nextParamInd)
					buf.append(" OR ");
				final String paramName = paramPrefix + (newNextParamInd++);
				if (convertToString) {
					buf.append(opFactory.make(dialect,
							dialect.castToString(valueExpr), "?" + paramName));
					params.put(paramName, paramsFactory.getParameterValue(
							PersistentValueType.STRING,
							StringUtils.asString(operand.getValue())));
				} else {
					buf.append(opFactory.make(dialect,
							valueExpr, "?" + paramName));
					params.put(paramName, paramsFactory.getParameterValue(
							valueType, operand.getValue()));
				}
			}
			buf.append(")");
		}

		return newNextParamInd;
	}

	/**
	 * Append equality condition to the filter expression.
	 *
	 * @param buf Filter expression builder.
	 * @param paramPrefix Query parameter names prefix to use.
	 * @param nextParamInd Index for the next query parameter placeholder name.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param negate {@code true} for inequality.
	 * @param operands Operands.
	 * @param valueExpr Value expression that the condition tests.
	 * @param valueType Value type.
	 * @param convertToString {@code true} to explicitly convert the values to a
	 * string type.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 *
	 * @return Index for the next query parameter placeholder name.
	 */
	private static int appendInCondition(final StringBuilder buf,
			final String paramPrefix, final int nextParamInd,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final boolean negate,
			final Collection<? extends FilterConditionOperand> operands,
			final String valueExpr, final PersistentValueType valueType,
			final boolean convertToString,
			final Map<String, JDBCParameterValue> params) {

		int newNextParamInd = nextParamInd;
		final int numOps = operands.size();
		if (numOps == 1) {
			final String paramName = paramPrefix + (newNextParamInd++);
			if (convertToString) {
				buf.append(dialect.castToString(valueExpr)).append(" ")
					.append(negate ? "<>" : "=").append(" ?").append(paramName);
				params.put(paramName, paramsFactory.getParameterValue(
						PersistentValueType.STRING,
						StringUtils.asString(
								operands.iterator().next().getValue())));
			} else {
				buf.append(valueExpr).append(" ")
					.append(negate ? "<>" : "=").append(" ?").append(paramName);
				params.put(paramName, paramsFactory.getParameterValue(valueType,
						operands.iterator().next().getValue()));
			}
		} else {
			final List<Object> opVals =
				new ArrayList<>(numOps > 10 ? numOps : 10);
			for (final Iterator<? extends FilterConditionOperand> i =
					operands.iterator(); i.hasNext();) {
				if (convertToString)
					opVals.add(StringUtils.asString(i.next().getValue()));
				else
					opVals.add(i.next().getValue());
			}
			final String paramName = paramPrefix + (newNextParamInd++);
			if (convertToString)
				buf.append(dialect.castToString(valueExpr));
			else
				buf.append(valueExpr);
			if (negate)
				buf.append(" NOT");
			buf.append(" IN (??").append(paramName);
			buf.append(")");
			params.put(paramName,
					paramsFactory.getParameterValue(valueType, opVals));
		}

		return newNextParamInd;
	}


	/**
	 * Get clause body.
	 *
	 * @return The clause body.
	 */
	String getBody() {

		return this.body;
	}

	/**
	 * Get properties that need joins.
	 *
	 * @return Set of property paths.
	 */
	Set<String> getUsedJoins() {

		return this.usedJoins;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString() {

		return "[" + this.body + "], used joins: " + this.usedJoins;
	}
}
