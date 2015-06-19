package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link String} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class StringResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * Tells if empty strings should be read as {@code null}.
	 */
	private final boolean readEmptyAsNull;


	/**
	 * Single stateless instance.
	 *
	 * @param readEmptyAsNull {@code true} if empty strings should be read as
	 * {@code null}.
	 */
	StringResourcePropertyValueHandler(final boolean readEmptyAsNull) {
		super(ResourcePropertyValueType.STRING, false);

		this.readEmptyAsNull = readEmptyAsNull;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str) {

		return str;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<String> getValueClass() {

		return String.class;
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

		out.writeValue((String) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		final String val = in.readStringValue();

		if (val == null)
			return null;

		return (this.readEmptyAsNull && val.isEmpty() ? null : val);
	}
}
