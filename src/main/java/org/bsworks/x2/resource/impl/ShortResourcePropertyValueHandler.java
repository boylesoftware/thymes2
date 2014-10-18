package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Short} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class ShortResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final ShortResourcePropertyValueHandler INSTANCE_REF =
		new ShortResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final ShortResourcePropertyValueHandler INSTANCE_PRIM =
		new ShortResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private ShortResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.SHORT, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Short valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Short.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid short integer number value " + str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<Short> getValueClass() {

		return Short.class;
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

		out.writeValue(((Short) val).shortValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Short readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readShortValue();
	}
}
