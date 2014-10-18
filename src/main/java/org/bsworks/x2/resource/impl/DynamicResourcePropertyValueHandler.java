package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Resource property value handler that determines the type of the handled value
 * dynamically during runtime.
 *
 * @author Lev Himmelfarb
 */
class DynamicResourcePropertyValueHandler
	extends SingleValueResourcePropertyValueHandler {

	/**
	 * Application resources manager.
	 */
	private final ResourcesImpl resources;


	/**
	 * Create new handler.
	 *
	 * @param resources Application resources manager.
	 */
	DynamicResourcePropertyValueHandler(final ResourcesImpl resources) {
		super(ResourcePropertyValueType.DYNAMIC);

		this.resources = resources;
	}


	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean hasStringRepresentation() {

		return false;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString(final Object val) {

		throw new UnsupportedOperationException(
				"Dynamic property values cannot be represented as strings.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str) {

		throw new UnsupportedOperationException(
				"Dynamic property values cannot be represented as strings.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		throw new UnsupportedOperationException(
				"Dynamic property values do not have predetermined value"
						+ " class.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		throw new UnsupportedOperationException(
				"Dynamic property values do not have predetermined persistent"
						+ " value type.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		final AbstractResourceHandlerImpl<?> rsrcHandler =
			this.resources.getResourceHandler(val.getClass());

		rsrcHandler.getResourceValueHandler().writeValue(access, val, out);
	}

	/**
	 * Always swallows the value in the input and returns {@code null}.
	 */
	@Override
	public Object readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		in.swallowValue();

		return null;
	}
}
