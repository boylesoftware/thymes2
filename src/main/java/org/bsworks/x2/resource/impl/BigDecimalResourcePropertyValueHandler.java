package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.math.BigDecimal;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link BigDecimal} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class BigDecimalResourcePropertyValueHandler
	extends CanBeIdResourcePropertyValueHandler {

	/**
	 * The instance.
	 */
	static final BigDecimalResourcePropertyValueHandler INSTANCE =
		new BigDecimalResourcePropertyValueHandler();


	/**
	 * Single stateless instance.
	 */
	private BigDecimalResourcePropertyValueHandler() {
		super(ResourcePropertyValueType.BIG_DECIMAL, false);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public BigDecimal valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return new BigDecimal(str);
		} catch (final NumberFormatException e) {
			throw new InvalidResourceDataException(
					"Invalid decimal number value " + str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<BigDecimal> getValueClass() {

		return BigDecimal.class;
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

		out.writeValue((BigDecimal) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public BigDecimal readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readBigDecimalValue();
	}
}
