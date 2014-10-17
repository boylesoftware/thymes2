package org.bsworks.x2.resource;


/**
 * Filter condition operand type.
 *
 * @author Lev Himmelfarb
 */
public enum FilterConditionOperandType {

	/**
	 * Constant value.
	 */
	CONSTANT,

	/**
	 * Resource property value.
	 */
	VALUE,

	/**
	 * Resource map property element key.
	 */
	KEY,

	/**
	 * Resource reference property referred record id.
	 */
	ID
}
