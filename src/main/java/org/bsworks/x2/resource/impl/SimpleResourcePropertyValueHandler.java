package org.bsworks.x2.resource.impl;


/**
 * Common parent for simple value resource property value handler
 * implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class SimpleResourcePropertyValueHandler
	extends CanBeMapKeyResourcePropertyValueHandler {

	/**
	 * Tells if the handled type is a primitive type.
	 */
	private final boolean primitive;


	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	protected SimpleResourcePropertyValueHandler(
			final ResourcePropertyValueType type, final boolean primitive) {
		super(type);

		this.primitive = primitive;
	}


	/**
	 * Tell if the handled type is a primitive type.
	 *
	 * @return {@code true} if the handled type is a primitive type.
	 */
	boolean isPrimitive() {

		return this.primitive;
	}
}
