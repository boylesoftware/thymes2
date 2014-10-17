package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Date} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class DateResourcePropertyValueHandler
	extends SimpleResourcePropertyValueHandler {

	/**
	 * The instance.
	 */
	static final DateResourcePropertyValueHandler INSTANCE =
		new DateResourcePropertyValueHandler();

	/**
	 * Format for {@link Date} fields.
	 */
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/**
	 * Date formatter instance.
	 */
	private static final ThreadLocal<DateFormat> DF = new ThreadLocal<>();


	/**
	 * Single stateless instance.
	 */
	private DateResourcePropertyValueHandler() {
		super(ResourcePropertyValueType.DATE, false);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString(final Object val) {

		if (val == null)
			return null;
		return getDateFormat().format((Date) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		try {
			return getDateFormat().parse(str);
		} catch (final ParseException e) {
			throw new InvalidResourceDataException("Invalid date value "
					+ str + ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return PersistentValueType.DATE;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.writeValue((Date) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Date readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		return in.readDateValue();
	}


	/**
	 * Get date format.
	 *
	 * @return Date format.
	 */
	private static DateFormat getDateFormat() {

		DateFormat df = DF.get();
		if (df == null) {
			df = new SimpleDateFormat(DATE_FORMAT);
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			DF.set(df);
		}

		return df;
	}
}
