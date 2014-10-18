package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Enum} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class EnumResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The enumeration class.
	 */
	@SuppressWarnings("rawtypes") // we know the class is an enum
	private final Class enumClass;


	/**
	 * Create new handler.
	 *
	 * @param enumClass Enumeration class.
	 */
	EnumResourcePropertyValueHandler(final Class<?> enumClass) {
		super(ResourcePropertyValueType.ENUM, false);

		this.enumClass = enumClass;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@SuppressWarnings("unchecked") // we know it's an enum
	@Override
	public Enum<?> valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return Enum.valueOf(this.enumClass, str);
		} catch (final IllegalArgumentException e) {
			throw new InvalidResourceDataException(
					"Invalid " + this.enumClass.getSimpleName()
					+ " enumeration value " + str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		return this.enumClass;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return PersistentValueType.STRING;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.writeValue((Enum<?>) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@SuppressWarnings("unchecked") // we know it's an enum
	@Override
	public Enum<?> readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readEnumValue(this.enumClass);
	}
}
