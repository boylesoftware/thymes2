package org.bsworks.x2.resource.impl;


/**
 * Common parent for single-value resource property value handler
 * implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class SingleValueResourcePropertyValueHandler
	extends AbstractResourcePropertyValueHandlerImpl {

	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 */
	protected SingleValueResourcePropertyValueHandler(
			final ResourcePropertyValueType type) {
		super(type, 0, null);
	}
}
