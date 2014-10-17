package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.TypePropertyHandler;


/**
 * Handler for concrete type pseudo-property of a polymorphic object.
 *
 * @author Lev Himmelfarb
 */
class ConcreteObjectPropertyHandler
	extends ObjectPropertyHandlerWrapper {

	/**
	 * Type property handler.
	 */
	private final TypePropertyHandlerImpl typePropHandler;

	/**
	 * Concrete type name.
	 */
	private final String concreteTypeName;


	/**
	 * Create new handler.
	 *
	 * @param typeHandler Concrete type object property handler.
	 * @param typePropHandler Type property handler.
	 * @param concreteTypeName Concrete type name.
	 */
	ConcreteObjectPropertyHandler(final ObjectPropertyHandlerImpl typeHandler,
			final TypePropertyHandlerImpl typePropHandler,
			final String concreteTypeName) {
		super(typeHandler);

		this.typePropHandler = typePropHandler;
		this.concreteTypeName = concreteTypeName;
	}


	/**
	 * Returns concrete type name as the property name.
	 */
	@Override
	public String getName() {

		return this.concreteTypeName;
	}

	/**
	 * Returns {@code true}.
	 */
	@Override
	public boolean isGettable() {

		return true;
	}

	/**
	 * Returns the specified object itself if its concrete type is the same as
	 * the handler's. Otherwise, returns {@code null}.
	 */
	@Override
	public Object getValue(final Object obj) {

		final String objTypeName =
			this.typePropHandler.getValueHandler().toString(
					this.typePropHandler.getValue(obj));

		return (this.concreteTypeName.equals(objTypeName) ? obj : null);
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean isSettable() {

		return false;
	}

	/**
	 * Throws {@link UnsupportedOperationException}.
	 */
	@Override
	public void setValue(final Object obj, final Object val) {

		throw new UnsupportedOperationException(
				"The property is not writable.");
	}

	/**
	 * Returns {@code true} as it is always single-valued.
	 */
	@Override
	public boolean isSingleValued() {

		return true;
	}

	/**
	 * Returns {@code true}.
	 */
	@Override
	public boolean updateIfNull() {

		return true;
	}

	/**
	 * Returns {@code null} as a concrete type cannot be polymorphic itself.
	 */
	@Override
	public TypePropertyHandler getTypeProperty() {

		return null;
	}

	/**
	 * Returns {@code true}.
	 */
	@Override
	public boolean isConcreteType() {

		return true;
	}
}
