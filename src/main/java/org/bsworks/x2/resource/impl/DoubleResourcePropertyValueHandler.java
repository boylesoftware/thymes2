package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Double} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class DoubleResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final DoubleResourcePropertyValueHandler INSTANCE_REF =
		new DoubleResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final DoubleResourcePropertyValueHandler INSTANCE_PRIM =
		new DoubleResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private DoubleResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.DOUBLE, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Double valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Double.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid double precision floating point number value"
							+ str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<Double> getValueClass() {

		return Double.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return PersistentValueType.NUMERIC;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.writeValue(((Double) val).doubleValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Double readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readDoubleValue();
	}
}
