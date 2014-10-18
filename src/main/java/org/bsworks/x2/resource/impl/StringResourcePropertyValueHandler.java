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
	 * The instance.
	 */
	static final StringResourcePropertyValueHandler INSTANCE =
		new StringResourcePropertyValueHandler();


	/**
	 * Single stateless instance.
	 */
	private StringResourcePropertyValueHandler() {
		super(ResourcePropertyValueType.STRING, false);
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

		return in.readStringValue();
	}
}
