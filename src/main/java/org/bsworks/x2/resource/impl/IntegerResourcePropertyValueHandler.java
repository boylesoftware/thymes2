package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Integer} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class IntegerResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final IntegerResourcePropertyValueHandler INSTANCE_REF =
		new IntegerResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final IntegerResourcePropertyValueHandler INSTANCE_PRIM =
		new IntegerResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private IntegerResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.INTEGER, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Integer valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Integer.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid integer number value " + str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<Integer> getValueClass() {

		return Integer.class;
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

		out.writeValue(((Integer) val).intValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Integer readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readIntegerValue();
	}
}
