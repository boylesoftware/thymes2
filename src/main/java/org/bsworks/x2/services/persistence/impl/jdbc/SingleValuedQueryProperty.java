package org.bsworks.x2.services.persistence.impl.jdbc;

import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Descriptor of a single-valued property available within a query.
 *
 * @author Lev Himmelfarb
 */
class SingleValuedQueryProperty {

	/**
	 * Property value expression.
	 */
	private final String valueExpr;

	/**
	 * Property value type.
	 */
	private final PersistentValueType valueType;


	/**
	 * Create new property descriptor.
	 *
	 * @param valueExpr Property value expression.
	 * @param valueType Property value type.
	 */
	SingleValuedQueryProperty(final String valueExpr,
			final PersistentValueType valueType) {

		this.valueExpr = valueExpr;
		this.valueType = valueType;
	}


	/**
	 * Get property value expression.
	 *
	 * @return Property value expression.
	 */
	String getValueExpression() {

		return this.valueExpr;
	}

	/**
	 * Get property value type.
	 *
	 * @return Property value type.
	 */
	PersistentValueType getValueType() {

		return this.valueType;
	}
}
