package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.OrderSpecElement;
import org.bsworks.x2.resource.OrderType;


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
	 * @param order Order specification.
	 * @param singlePropExprs Single-valued properties available from the query.
	 * @param allSingleJoins Joins for the single-valued properties.
	 */
	OrderByClause(final OrderSpec<?> order,
			final Map<String, SingleValuedQueryProperty> singlePropExprs,
			final SortedMap<String, String> allSingleJoins) {

		// create buffer for building the clause
		final StringBuilder body = new StringBuilder(64);

		// process order specification elements
		final StringBuilder propPathBuf = new StringBuilder(128);
		for (final OrderSpecElement orderEl : order.getElements()) {

			// get property descriptor from the query
			final String propPath = orderEl.getPropertyPath();
			final SingleValuedQueryProperty prop =
				singlePropExprs.get(propPath);

			// get used joins
			propPathBuf.setLength(0);
			propPathBuf.append(propPath);
			int dotInd = propPathBuf.length();
			do {
				propPathBuf.setLength(dotInd);
				final String propPathPrefix = propPathBuf.toString();
				if (allSingleJoins.containsKey(propPathPrefix))
					this.usedJoins.add(propPathPrefix);
			} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);

			// add element to the clause
			if (body.length() > 0)
				body.append(", ");
			body.append(prop.getValueExpression())
				.append(orderEl.getType() == OrderType.ASC ? " ASC" : " DESC");
		}

		// save the clause
		this.body = body.toString();
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
