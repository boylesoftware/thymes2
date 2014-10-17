package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Float} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class FloatResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final FloatResourcePropertyValueHandler INSTANCE_REF =
		new FloatResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final FloatResourcePropertyValueHandler INSTANCE_PRIM =
		new FloatResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private FloatResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.FLOAT, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Float valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Float.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid floating point number value " + str + ".", e);
		}
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

		out.writeValue(((Float) val).floatValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Float readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readFloatValue();
	}
}
