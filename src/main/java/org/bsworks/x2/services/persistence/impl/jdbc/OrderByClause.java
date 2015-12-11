package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.OrderSpecElement;
import org.bsworks.x2.resource.PropertyOrderSpecElement;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.SegmentOrderSpecElement;
import org.bsworks.x2.resource.SortDirection;
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Represents persistent resource fetch query "ORDER BY" clause.
 *
 * @author Lev Himmelfarb
 */
class OrderByClause {

	/**
	 * The clause body.
	 */
	private final String body;

	/**
	 * Paths of used properties that need joins.
	 */
	private final Set<String> usedJoins = new HashSet<>();


	/**
	 * Create new "ORDER BY" clause.
	 *
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param order Order specification.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param collectionProps Collection property stumps from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 */
	OrderByClause(final Resources resources, final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final OrderSpec<?> order,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final Map<String, CollectionQueryProperty> collectionProps,
			final SortedMap<String, String> allSingleJoins,
			final Map<String, JDBCParameterValue> params) {

		// create buffer for building the clause
		final StringBuilder body = new StringBuilder(64);

		// process order specification elements
		int segmentNum = 0;
		final StringBuilder propPathBuf = new StringBuilder(128);
		for (final OrderSpecElement orderEl : order.getElements()) {

			// get value expression
			final String valExpr;
			if (orderEl instanceof PropertyOrderSpecElement)
				valExpr = processPropertyOrderSpecElement(
						(PropertyOrderSpecElement) orderEl, dialect,
						singlePropExprs, allSingleJoins, this.usedJoins,
						propPathBuf);
			else
				valExpr = processSegmentOrderSpecElement(
						(SegmentOrderSpecElement) orderEl, segmentNum++,
						resources, dialect, paramsFactory, singlePropExprs,
						collectionProps, allSingleJoins, params);

			// add element to the clause
			if (body.length() > 0)
				body.append(", ");
			body.append(valExpr)
				.append(orderEl.getSortDirection() == SortDirection.ASC ?
						" ASC" : " DESC");
		}

		// save the clause
		this.body = body.toString();
	}

	/**
	 * Get order clause value expression for the specified property order
	 * specification element.
	 *
	 * @param orderEl Order specification element.
	 * @param dialect SQL dialect.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 * @param usedJoins Paths of used properties that need joins. The method may
	 * add elements to this set.
	 * @param propPathBuf Reusable string buffer.
	 *
	 * @return The value expression.
	 */
	private static String processPropertyOrderSpecElement(
			final PropertyOrderSpecElement orderEl,
			final SQLDialect dialect,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final SortedMap<String, String> allSingleJoins,
			final Set<String> usedJoins,
			final StringBuilder propPathBuf) {

		// get property descriptor from the query
		final String propPath = orderEl.getPropertyPath();
		final SingleValuedQueryProperty prop = singlePropExprs.get(propPath);

		// get used joins
		propPathBuf.setLength(0);
		propPathBuf.append(propPath);
		int dotInd = propPathBuf.length();
		do {
			propPathBuf.setLength(dotInd);
			final String propPathPrefix = propPathBuf.toString();
			if (allSingleJoins.containsKey(propPathPrefix))
				usedJoins.add(propPathPrefix);
		} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);

		// get value expression
		final Object[] funcParams = orderEl.getValueFunctionParams();
		switch (orderEl.getValueFunction()) {
		case LENGTH:
			return dialect.stringLength(
					prop.getValueType() == PersistentValueType.STRING ?
					prop.getValueExpression() :
					dialect.castToString(prop.getValueExpression())
			);
		case SUBSTRING:
			return dialect.stringSubstring(
					(prop.getValueType() == PersistentValueType.STRING ?
						prop.getValueExpression() :
						dialect.castToString(prop.getValueExpression())),
					((Integer) funcParams[0]).intValue(),
					((Integer) funcParams[1]).intValue());
		case LPAD:
			return dialect.stringLeftPad(
					(prop.getValueType() == PersistentValueType.STRING ?
						prop.getValueExpression() :
						dialect.castToString(prop.getValueExpression())),
					((Integer) funcParams[0]).intValue(),
					((Character) funcParams[1]).charValue());
		default:
			return prop.getValueExpression();
		}
	}

	/**
	 * Get order clause value expression for the specified segment order
	 * specification element.
	 *
	 * @param orderEl Order specification element.
	 * @param segmentNum Unique segment number.
	 * @param resources Application resources manager.
	 * @param dialect SQL dialect.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param collectionProps Collection property stumps from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 * @param params Parameters collection, to which to add any query
	 * parameters.
	 *
	 * @return The value expression.
	 */
	private static String processSegmentOrderSpecElement(
			final SegmentOrderSpecElement orderEl,
			final int segmentNum,
			final Resources resources,
			final SQLDialect dialect,
			final ParameterValuesFactoryImpl paramsFactory,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final Map<String, CollectionQueryProperty> collectionProps,
			final SortedMap<String, String> allSingleJoins,
			final Map<String, JDBCParameterValue> params) {

		final WhereClause splitClause = new WhereClause(resources, dialect,
				paramsFactory, orderEl.getSegmentFilter(), "s" + segmentNum,
				singlePropExprs, collectionProps, allSingleJoins, params);

		return splitClause.getBody();
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
}
