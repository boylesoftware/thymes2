package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.bsworks.x2.services.persistence.ParameterValuesFactory;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * QuerypParameter value handlers factory implementation.
 *
 * @author Lev Himmelfarb
 */
class ParameterValuesFactoryImpl
	implements ParameterValuesFactory<JDBCParameterValue> {

	/**
	 * String parameter value handler.
	 */
	private static final class StringParameterValue
		implements JDBCParameterValue {

		/**
		 * Parameter value.
		 */
		private final String value;


		/**
		 * Create new value handler.
		 *
		 * @param value The value.
		 */
		StringParameterValue(final Object value) {

			if ((value != null) && !(value instanceof String)
					&& !value.getClass().isEnum())
				throw new IllegalArgumentException("Specified string parameter"
						+ " value is not a string.");

			this.value = (value == null ? null : value.toString());
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int getNumPlaceholders() {

			return 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int set(final PreparedStatement pstmt, final int ind)
			throws SQLException {

			if (this.value == null)
				pstmt.setNull(ind, Types.CHAR);
			else
				pstmt.setString(ind, this.value);

			return ind + 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public String toString() {

			return (this.value != null ? this.value.toString() : "null");
		}
	}

	/**
	 * Number parameter value handler.
	 */
	private static final class NumberParameterValue
		implements JDBCParameterValue {

		/**
		 * Parameter value.
		 */
		private final Number value;


		/**
		 * Create new value handler.
		 *
		 * @param value The value.
		 */
		NumberParameterValue(final Object value) {

			if ((value != null) && !(value instanceof Number))
				throw new IllegalArgumentException("Specified numeric parameter"
						+ " value is not a number.");

			this.value = (Number) value;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int getNumPlaceholders() {

			return 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int set(final PreparedStatement pstmt, final int ind)
			throws SQLException {

			if (this.value == null)
				pstmt.setNull(ind, Types.NUMERIC);
			else
				pstmt.setObject(ind, this.value, Types.NUMERIC);

			return ind + 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public String toString() {

			return (this.value != null ? this.value.toString() : "null");
		}
	}

	/**
	 * Boolean parameter value handler.
	 */
	private static final class BooleanParameterValue
		implements JDBCParameterValue {

		/**
		 * Parameter value.
		 */
		private final Boolean value;


		/**
		 * Create new value handler.
		 *
		 * @param value The value.
		 */
		BooleanParameterValue(final Object value) {

			if ((value != null) && !(value instanceof Boolean))
				throw new IllegalArgumentException("Specified Boolean parameter"
						+ " value is not a Boolean.");

			this.value = (Boolean) value;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int getNumPlaceholders() {

			return 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int set(final PreparedStatement pstmt, final int ind)
			throws SQLException {

			if (this.value == null)
				pstmt.setNull(ind, Types.BOOLEAN);
			else
				pstmt.setBoolean(ind, this.value.booleanValue());

			return ind + 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public String toString() {

			return (this.value != null ? this.value.toString() : "null");
		}
	}

	/**
	 * Date parameter value handler.
	 */
	private static final class DateParameterValue
		implements JDBCParameterValue {

		/**
		 * Parameter value.
		 */
		private final Timestamp value;


		/**
		 * Create new value handler.
		 *
		 * @param value The value.
		 */
		DateParameterValue(final Object value) {

			if ((value != null) && !(value instanceof Date))
				throw new IllegalArgumentException("Specified date parameter"
						+ " value is not a date.");

			this.value = (value != null ?
					new Timestamp(((Date) value).getTime()) : null);
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int getNumPlaceholders() {

			return 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int set(final PreparedStatement pstmt, final int ind)
			throws SQLException {

			if (this.value == null)
				pstmt.setNull(ind, Types.TIMESTAMP);
			else
				pstmt.setTimestamp(ind, this.value);

			return ind + 1;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public String toString() {

			return (this.value != null ? this.value.toString() : "null");
		}
	}

	/**
	 * Collection value handler.
	 */
	private static final class CollectionParameterValue
		implements JDBCParameterValue {

		/**
		 * Element value handlers.
		 */
		private final Collection<JDBCParameterValue> values;


		/**
		 * Create new value handler.
		 *
		 * @param values Element value handlers.
		 */
		CollectionParameterValue(final Collection<JDBCParameterValue> values) {

			this.values = values;
		}


		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int getNumPlaceholders() {

			return this.values.size();
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public int set(final PreparedStatement pstmt, final int ind)
			throws SQLException {

			int i = ind;
			for (final JDBCParameterValue v : this.values)
				v.set(pstmt, i++);

			return i;
		}

		/* (non-Javadoc)
		 * See overridden method.
		 */
		@Override
		public String toString() {

			return (this.values != null ? this.values.toString() : "null");
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public JDBCParameterValue getParameterValue(final PersistentValueType type,
			final Object value) {

		if (value instanceof Collection) {
			final Collection<?> values = (Collection<?>) value;
			final int numEls = values.size();
			if (numEls == 0)
				throw new IllegalArgumentException(
						"Collection parameter cannot be empty.");
			final Collection<JDBCParameterValue> valueHandlers =
				new ArrayList<>(numEls > 10 ? numEls : 10);
			for (final Object v : values)
				valueHandlers.add(getSimpleParameterValue(type, v));
			return new CollectionParameterValue(valueHandlers);
		}

		return getSimpleParameterValue(type, value);
	}

	/**
	 * Get non-collection value handler.
	 *
	 * @param type Value type.
	 * @param value The value.
	 *
	 * @return The value handler.
	 */
	private static JDBCParameterValue getSimpleParameterValue(
			final PersistentValueType type, final Object value) {

		switch (type) {
		case STRING:
			return new StringParameterValue(value);
		case NUMERIC:
			return new NumberParameterValue(value);
		case BOOLEAN:
			return new BooleanParameterValue(value);
		case DATE:
			return new DateParameterValue(value);
		default:
			throw new IllegalArgumentException(
					"Unsupported parameter value type " + type + ".");
		}
	}
}
