package org.bsworks.x2.util.sql.dialect;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.services.persistence.ParameterValue;
import org.bsworks.x2.services.persistence.ParameterValuesFactory;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * SQL dialect implementation for <i>PostgreSQL</i>.
 *
 * @author Lev Himmelfarb
 */
class PostgreSQLDialect
	implements SQLDialect {

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean tempTablesRequireReadWrite() {

		return true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String quoteColumnLabel(final String colLabel) {

		return "\"" + colLabel.replace("\"", "\"\"") + "\"";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String nullableConcat(final String stringLiteral,
			final String selectExpr) {

		return "'" + stringLiteral.replace("'", "''") + "' || " + selectExpr;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String castToString(final String valExpr) {

		return "CAST(" + valExpr + " AS VARCHAR)";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String stringLength(final String valExpr) {

		return "LENGTH(" + valExpr + ")";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String stringLeftPad(final String valExpr, final int width,
			final char paddingChar) {

		return "LPAD(" + valExpr + ", " + width + ", '"
				+ (paddingChar == '\'' ? "''" : String.valueOf(paddingChar))
				+ "')";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String regularExpressionMatch(final String valExpr,
			final String reExpr, final boolean negate,
			final boolean caseSensitive) {

		final String resExpr;
		if (negate) {
			if (caseSensitive)
				resExpr = valExpr + " !~ " + reExpr;
			else
				resExpr = valExpr + " !~* " + reExpr;
		} else {
			if (caseSensitive)
				resExpr = valExpr + " ~ " + reExpr;
			else
				resExpr = valExpr + " ~* " + reExpr;
		}

		return resExpr;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String substringMatch(final String valExpr,
			final String substringExpr, final boolean negate,
			final boolean caseSensitive) {

		final String patternExpr =
			"REGEXP_REPLACE(" + substringExpr
			+ ", '([%_\\\\])', '\\\\\\1', 'g')";

		final String resExpr;
		if (negate) {
			if (caseSensitive)
				resExpr =
					valExpr + " NOT LIKE '%' || " + patternExpr + " || '%'";
			else
				resExpr =
					valExpr + " NOT ILIKE '%' || " + patternExpr + " || '%'";
		} else {
			if (caseSensitive)
				resExpr = valExpr + " LIKE '%' || " + patternExpr + " || '%'";
			else
				resExpr = valExpr + " ILIKE '%' || " + patternExpr + " || '%'";
		}

		return resExpr;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String prefixMatch(final String valExpr, final String prefixExpr,
			final boolean negate, final boolean caseSensitive) {

		final String patternExpr =
			"REGEXP_REPLACE(" + prefixExpr + ", '([%_\\\\])', '\\\\\\1', 'g')";

		final String resExpr;
		if (negate) {
			if (caseSensitive)
				resExpr = valExpr + " NOT LIKE " + patternExpr + " || '%'";
			else
				resExpr = valExpr + " NOT ILIKE " + patternExpr + " || '%'";
		} else {
			if (caseSensitive)
				resExpr = valExpr + " LIKE " + patternExpr + " || '%'";
			else
				resExpr = valExpr + " ILIKE " + patternExpr + " || '%'";
		}

		return resExpr;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <V extends ParameterValue> String makeRangedSelect(
			final String selectQuery, final RangeSpec range,
			final ParameterValuesFactory<V> paramsFactory,
			final Map<String, V> params) {

		final StringBuilder res = new StringBuilder(selectQuery.length()
				+ (" LIMIT ?" + RANGE_LIMIT_PARAM
						+ " OFFSET ?" + RANGE_OFFSET_PARAM).length());
		res.append(selectQuery);

		res.append(" LIMIT ?").append(RANGE_LIMIT_PARAM);
		params.put(RANGE_LIMIT_PARAM,
				paramsFactory.getParameterValue(PersistentValueType.NUMERIC,
						Integer.valueOf(range.getMaxRecords())));

		if (range.getFirstRecord() > 0) {
			res.append(" OFFSET ?").append(RANGE_OFFSET_PARAM);
			params.put(RANGE_OFFSET_PARAM,
					paramsFactory.getParameterValue(
							PersistentValueType.NUMERIC,
							Integer.valueOf(range.getFirstRecord())));
		}

		return res.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void makeSelectIntoTempTable(final String tempTableName,
			final boolean create, final String selectQuery,
			final List<String> preStatements,
			final List<String> postStatements) {

		if (create) {
			preStatements.add("CREATE TEMPORARY TABLE " + tempTableName
					+ " ON COMMIT DROP AS " + selectQuery);
		} else {
			preStatements.add("TRUNCATE TABLE " + tempTableName);
			preStatements.add("INSERT INTO " + tempTableName + " "
					+ selectQuery);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String makeSelectWithShareLock(final String selectQuery,
			final String... lockTables) {

		final StringBuilder res = new StringBuilder(selectQuery.length() + 128);

		res.append(selectQuery).append(" FOR SHARE");

		if (lockTables.length > 0) {
			res.append(" OF ");
			for (int i = 0; i < lockTables.length; i++) {
				if (i > 0)
					res.append(", ");
				res.append(lockTables[i]);
			}
		}

		return res.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String makeSelectWithExclusiveLock(final String selectQuery,
			final String... lockTables) {

		final StringBuilder res = new StringBuilder(selectQuery.length() + 128);

		res.append(selectQuery).append(" FOR UPDATE");

		if (lockTables.length > 0) {
			res.append(" OF ");
			for (int i = 0; i < lockTables.length; i++) {
				if (i > 0)
					res.append(", ");
				res.append(lockTables[i]);
			}
		}

		return res.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createDeleteFromAliasedTable(final String fromTable,
			final String fromTableAlias, final String whereClause) {

		return "DELETE FROM " + fromTable + " AS " + fromTableAlias
				+ (whereClause == null ? "" : " WHERE " + whereClause);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createDeleteWithJoins(final String fromTable,
			final String fromTableAlias, final String joinedTables,
			final String joinConditions, final String whereClause) {

		return "DELETE FROM " + fromTable + " AS " + fromTableAlias
				+ " USING " + joinedTables + " WHERE " + joinConditions
				+ (whereClause == null ? "" : " AND (" + whereClause + ")");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createUpdateWithJoins(final String table,
			final String tableAlias, final String setClause,
			final String joinedTables, final String joinConditions,
			final String whereClause) {

		return "UPDATE " + table + " AS " + tableAlias + " SET " + setClause
				+ " FROM " + joinedTables + " WHERE " + joinConditions
				+ (whereClause == null ? "" : " AND (" + whereClause + ")");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String lockTablesInShareMode(final String... tables) {

		final StringBuilder stmt = new StringBuilder(256);
		stmt.append("LOCK TABLE ");
		for (int i = 0; i < tables.length; i++) {
			if (i > 0)
				stmt.append(", ");
			stmt.append(tables[i]);
		}
		stmt.append(" IN SHARE MODE");

		return stmt.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String lockTablesInExclusiveMode(final String... tables) {

		final StringBuilder stmt = new StringBuilder(256);
		stmt.append("LOCK TABLE ");
		for (int i = 0; i < tables.length; i++) {
			if (i > 0)
				stmt.append(", ");
			stmt.append(tables[i]);
		}
		stmt.append(" IN EXCLUSIVE MODE");

		return stmt.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String unlockTables(final String... tables) {

		return null;
	}
}
