package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Boolean} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class BooleanResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The reference instance.
	 */
	static final BooleanResourcePropertyValueHandler INSTANCE_REF =
		new BooleanResourcePropertyValueHandler(false);

	/**
	 * The primitive instance.
	 */
	static final BooleanResourcePropertyValueHandler INSTANCE_PRIM =
		new BooleanResourcePropertyValueHandler(true);


	/**
	 * Single stateless instance.
	 *
	 * @param primitive {@code true} if the handled type is a primitive type.
	 */
	private BooleanResourcePropertyValueHandler(final boolean primitive) {
		super(ResourcePropertyValueType.BOOLEAN, primitive);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Boolean valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		if (str.equals("true"))
			return Boolean.TRUE;
		if (str.equals("false"))
			return Boolean.FALSE;

		throw new InvalidResourceDataException(
				"Invalid Boolean value " + str + ".");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<Boolean> getValueClass() {

		return Boolean.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return PersistentValueType.BOOLEAN;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.writeValue(((Boolean) val).booleanValue());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Boolean readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readBooleanValue();
	}
}
