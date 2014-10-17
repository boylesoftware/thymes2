package org.bsworks.x2.services.persistence.impl.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;


/**
 * Result set column value reader.
 *
 * @param <Y> Value type.
 *
 * @author Lev Himmelfarb
 */
abstract class ResultSetValueReader<Y> {

	/**
	 * {@link String} value reader.
	 */
	static final ResultSetValueReader<String> STRING_VALUE_READER =
		new ResultSetValueReader<String>() {
			@Override
			String readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				return rs.getString(colInd);
			}
	};

	/**
	 * {@link Byte} value reader.
	 */
	static final ResultSetValueReader<Byte> BYTE_VALUE_READER =
		new ResultSetValueReader<Byte>() {
			@Override
			Byte readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final byte val = rs.getByte(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Byte.valueOf(val));
			}
	};

	/**
	 * {@link Short} value reader.
	 */
	static final ResultSetValueReader<Short> SHORT_VALUE_READER =
		new ResultSetValueReader<Short>() {
			@Override
			Short readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final short val = rs.getShort(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Short.valueOf(val));
			}
	};

	/**
	 * {@link Integer} value reader.
	 */
	static final ResultSetValueReader<Integer> INTEGER_VALUE_READER =
		new ResultSetValueReader<Integer>() {
			@Override
			Integer readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final int val = rs.getInt(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Integer.valueOf(val));
			}
	};

	/**
	 * {@link Long} value reader.
	 */
	static final ResultSetValueReader<Long> LONG_VALUE_READER =
		new ResultSetValueReader<Long>() {
			@Override
			Long readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final long val = rs.getLong(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Long.valueOf(val));
			}
	};

	/**
	 * {@link Boolean} value reader.
	 */
	static final ResultSetValueReader<Boolean> BOOLEAN_VALUE_READER =
		new ResultSetValueReader<Boolean>() {
			@Override
			Boolean readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final boolean val = rs.getBoolean(colInd);
				return (!val && rs.wasNull() ? null :
					Boolean.valueOf(val));
			}
	};

	/**
	 * {@link Float} value reader.
	 */
	static final ResultSetValueReader<Float> FLOAT_VALUE_READER =
		new ResultSetValueReader<Float>() {
			@Override
			Float readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final float val = rs.getFloat(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Float.valueOf(val));
			}
	};

	/**
	 * {@link Double} value reader.
	 */
	static final ResultSetValueReader<Double> DOUBLE_VALUE_READER =
		new ResultSetValueReader<Double>() {
			@Override
			Double readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final double val = rs.getDouble(colInd);
				return ((val == 0) && rs.wasNull() ? null :
					Double.valueOf(val));
			}
	};

	/**
	 * {@link BigDecimal} value reader.
	 */
	static final ResultSetValueReader<BigDecimal> BIG_DECIMAL_VALUE_READER =
		new ResultSetValueReader<BigDecimal>() {
			@Override
			BigDecimal readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				return rs.getBigDecimal(colInd);
			}
	};

	/**
	 * {@link Date} value reader.
	 */
	static final ResultSetValueReader<Date> DATE_VALUE_READER =
		new ResultSetValueReader<Date>() {
			@Override
			Date readValue(final ResultSet rs, final int colInd)
				throws SQLException {
				final Timestamp val = rs.getTimestamp(colInd);
				return (val == null ? null : new Date(val.getTime()));
			}
	};


	/**
	 * Read value.
	 *
	 * @param rs Result set.
	 * @param colInd Column index.
	 *
	 * @return The value.
	 *
	 * @throws SQLException If a database error happens.
	 */
	abstract Y readValue(ResultSet rs, int colInd)
		throws SQLException;
}
