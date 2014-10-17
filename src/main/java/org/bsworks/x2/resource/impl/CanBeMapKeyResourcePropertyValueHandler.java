package org.bsworks.x2.resource.impl;


/**
 * Abstract resource property value handler for values that can be used as map
 * keys.
 *
 * @author Lev Himmelfarb
 */
abstract class CanBeMapKeyResourcePropertyValueHandler
	extends SingleValueResourcePropertyValueHandler {

	/**
	 * Create new handler.
	 *
	 * @param type Handled value type category.
	 */
	protected CanBeMapKeyResourcePropertyValueHandler(
			final ResourcePropertyValueType type) {
		super(type);
	}


	/**
	 * Returns {@code true}.
	 */
	@Override
	public final boolean hasStringRepresentation() {

		return true;
	}

	/**
	 * Default implementation simply calls {@link Object#toString()} on the
	 * value object if it is not {@code null}.
	 */
	@Override
	public String toString(final Object val) {

		if (val == null)
			return null;

		return val.toString();
	}
}
