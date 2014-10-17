package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Long} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class LongResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final LongResourcePropertyValueHandler INSTANCE_REF =
		new LongResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final LongResourcePropertyValueHandler INSTANCE_PRIM =
		new LongResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private LongResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.LONG, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Long valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Long.valueOf(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid long integer number value " + str + ".", e);
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

		out.writeValue(((Long) val).longValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Long readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readLongValue();
	}
}
