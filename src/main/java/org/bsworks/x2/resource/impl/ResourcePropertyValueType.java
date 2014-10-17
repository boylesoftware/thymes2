package org.bsworks.x2.resource.impl;


/**
 * Resource property value type.
 *
 * @author Lev Himmelfarb
 */
enum ResourcePropertyValueType {

	/**
	 * {@link String}.
	 */
	STRING,

	/**
	 * {@link Byte}.
	 */
	BYTE,

	/**
	 * {@link Short}.
	 */
	SHORT,

	/**
	 * {@link Integer}.
	 */
	INTEGER,

	/**
	 * {@link Long}.
	 */
	LONG,

	/**
	 * {@link java.math.BigDecimal}.
	 */
	BIG_DECIMAL,

	/**
	 * {@link Float}.
	 */
	FLOAT,

	/**
	 * {@link Double}.
	 */
	DOUBLE,

	/**
	 * {@link Boolean}.
	 */
	BOOLEAN,

	/**
	 * {@link java.util.Date}.
	 */
	DATE,

	/**
	 * {@link java.lang.Enum}.
	 */
	ENUM,

	/**
	 * {@link org.bsworks.x2.resource.Ref}.
	 */
	REF,

	/**
	 * Nested object.
	 */
	OBJECT,

	/**
	 * Type determined dynamically during runtime.
	 */
	DYNAMIC,

	/**
	 * {@link java.util.List}.
	 */
	LIST,

	/**
	 * {@link java.util.Set}.
	 */
	SET,

	/**
	 * {@link java.util.Map}.
	 */
	MAP
}
