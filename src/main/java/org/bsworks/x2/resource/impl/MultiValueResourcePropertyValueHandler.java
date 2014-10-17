package org.bsworks.x2.resource.impl;


/**
 * Common parent for multi-value resource property value handler
 * implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class MultiValueResourcePropertyValueHandler
	extends AbstractResourcePropertyValueHandlerImpl {

	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 * @param collectionDegree Value collection degree.
	 * @param nextInChain Next value handler in the collection chain.
	 */
	protected MultiValueResourcePropertyValueHandler(
			final ResourcePropertyValueType type, final int collectionDegree,
			final AbstractResourcePropertyValueHandlerImpl nextInChain) {
		super(type, collectionDegree, nextInChain);
	}
}
