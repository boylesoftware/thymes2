package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.FilterConditionOperand;


/**
 * Filter condition operand implementation.
 *
 * @author Lev Himmelfarb
 */
class FilterConditionOperandImpl
	implements FilterConditionOperand {

	/**
	 * The value.
	 */
	private final Object value;


	/**
	 * Create new operand.
	 *
	 * @param value The value.
	 */
	FilterConditionOperandImpl(final Object value) {

		this.value = value;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object getValue() {

		return this.value;
	}
}
