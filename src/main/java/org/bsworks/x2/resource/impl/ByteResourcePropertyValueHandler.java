package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Byte} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class ByteResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final ByteResourcePropertyValueHandler INSTANCE_REF =
		new ByteResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final ByteResourcePropertyValueHandler INSTANCE_PRIM =
		new ByteResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private ByteResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.BYTE, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Byte valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Byte.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid byte value " + str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<Byte> getValueClass() {

		return Byte.class;
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

		out.writeValue(((Byte) val).byteValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Byte readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readByteValue();
	}
}
