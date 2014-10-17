package org.bsworks.x2.resource.impl;


/**
 * Abstract resource property value handler for values that can be used as
 * persistent resource record ids.
 *
 * @author Lev Himmelfarb
 */
abstract class CanBeIdResourcePropertyValueHandler
	extends SimpleResourcePropertyValueHandler {

	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	protected CanBeIdResourcePropertyValueHandler(
			final ResourcePropertyValueType type, final boolean primitive) {
		super(type, primitive);
	}
}
