package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.ResourcePropertyValueHandler;


/**
 * Common parent for various resource property value handler implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class AbstractResourcePropertyValueHandlerImpl
	implements ResourcePropertyValueHandler {

	/**
	 * Handled value type category.
	 */
	private final ResourcePropertyValueType type;

	/**
	 * Collection degree.
	 */
	private final int collectionDegree;

	/**
	 * Next value handler in the collection chain, or {@code null} for the last
	 * element.
	 */
	private final AbstractResourcePropertyValueHandlerImpl nextInChain;


	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 * @param collectionDegree Value collection degree, with 0 meaning the value
	 * is single-valued.
	 * @param nextInChain Next value handler in the collection chain, or
	 * {@code null} for the last element.
	 */
	protected AbstractResourcePropertyValueHandlerImpl(
			final ResourcePropertyValueType type, final int collectionDegree,
			final AbstractResourcePropertyValueHandlerImpl nextInChain) {

		this.type = type;
		this.collectionDegree = collectionDegree;
		this.nextInChain = nextInChain;
	}


	/**
	 * Get handled value type category.
	 *
	 * @return Handled value type category.
	 */
	final ResourcePropertyValueType getType() {

		return this.type;
	}

	/**
	 * Get value collection degree.
	 *
	 * @return Value collection degree, with 0 meaning the value is
	 * single-valued.
	 */
	final int getCollectionDegree() {

		return this.collectionDegree;
	}

	/**
	 * Get next value handler in the collection chain.
	 *
	 * @return Next value handler, or {@code null} if the end of the chain.
	 */
	final AbstractResourcePropertyValueHandlerImpl getNextInChain() {

		return this.nextInChain;
	}

	/**
	 * Get last value handler in the collection chain.
	 *
	 * @return Last value handler, or this handler is the end of the chain.
	 */
	final AbstractResourcePropertyValueHandlerImpl getLastInChain() {

		return (this.nextInChain != null
				? this.nextInChain.getLastInChain() : this);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final boolean isRef() {

		return (this.type == ResourcePropertyValueType.REF);
	}

	/**
	 * Default implementation throws {@link UnsupportedOperationException}.
	 * Reference value handler must override it.
	 */
	@Override
	public Class<?> getRefTargetClass() {

		throw new UnsupportedOperationException(
				"Not a reference value handler.");
	}
}
