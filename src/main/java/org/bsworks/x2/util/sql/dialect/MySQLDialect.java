package org.bsworks.x2.util.sql.dialect;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.services.persistence.ParameterValue;
import org.bsworks.x2.services.persistence.ParameterValuesFactory;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * SQL dialect implementation for <i>MySQL</i>.
 *
 * @author Lev Himmelfarb
 */
class MySQLDialect
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

		return "CONCAT('" + stringLiteral.replace("'", "''") + "', "
				+ selectExpr + ")";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String stringLength(final String valExpr) {

		return "LENGTH(CAST(" + valExpr + " AS CHAR))";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String stringLeftPad(final String valExpr, final int width,
			final char paddingChar) {

		return "LPAD(CAST(" + valExpr + " AS CHAR), " + width + ", '"
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
				resExpr = valExpr + " COLLATE utf8_bin"
					+ " NOT REGEXP " + reExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
					+ " NOT REGEXP " + reExpr;
		} else {
			if (caseSensitive)
				resExpr = valExpr + " COLLATE utf8_bin"
					+ " REGEXP " + reExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
					+ " REGEXP " + reExpr;
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
			"CONCAT('%', REPLACE(REPLACE(REPLACE(" + substringExpr
			+ ", '\\\\', '\\\\\\\\'), '%', '\\%'), '_', '\\_'), '%')";

		final String resExpr;
		if (negate) {
			if (caseSensitive)
				resExpr = valExpr + " COLLATE utf8_bin"
						+ " NOT LIKE " + patternExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
						+ " NOT LIKE " + patternExpr;
		} else {
			if (caseSensitive)
				resExpr = valExpr + " COLLATE utf8_bin"
						+ " LIKE " + patternExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
						+ " LIKE " + patternExpr;
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
			"CONCAT(REPLACE(REPLACE(REPLACE(" + prefixExpr
			+ ", '\\\\', '\\\\\\\\'), '%', '\\%'), '_', '\\_'), '%')";

		final String resExpr;
		if (negate) {
			if (caseSensitive)
				resExpr = valExpr + " COLLATE utf8_bin"
						+ " NOT LIKE " + patternExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
						+ " NOT LIKE " + patternExpr;
		} else {
			if (caseSensitive)
				resExpr = valExpr + " COLLATE utf8_bin"
						+ " LIKE " + patternExpr;
			else
				resExpr = valExpr + " COLLATE utf8_general_ci"
						+ " LIKE " + patternExpr;
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
				+ (" LIMIT ?" + RANGE_OFFSET_PARAM
						+ ", ?" + RANGE_LIMIT_PARAM).length());
		res.append(selectQuery);

		res.append(" LIMIT ");
		if (range.getFirstRecord() > 0) {
			res.append("?").append(RANGE_OFFSET_PARAM).append(", ");
			params.put(RANGE_OFFSET_PARAM,
					paramsFactory.getParameterValue(
							PersistentValueType.NUMERIC,
							Integer.valueOf(range.getFirstRecord())));
		}

		res.append("?").append(RANGE_LIMIT_PARAM);
		params.put(RANGE_LIMIT_PARAM,
				paramsFactory.getParameterValue(PersistentValueType.NUMERIC,
						Integer.valueOf(range.getMaxRecords())));

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

		preStatements.add("CREATE TEMPORARY TABLE "
				+ tempTableName + " " + selectQuery);

		postStatements.add("DROP TABLE " + tempTableName);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String makeSelectWithShareLock(final String selectQuery,
			final String... lockTables) {

		return selectQuery + " LOCK IN SHARE MODE";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String makeSelectWithExclusiveLock(final String selectQuery,
			final String... lockTables) {

		return selectQuery + " FOR UPDATE";
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createDeleteFromAliasedTable(final String fromTable,
			final String fromTableAlias, final String whereClause) {

		return "DELETE " + fromTableAlias + " FROM " + fromTable + " AS "
				+ fromTableAlias
				+ (whereClause == null ? "" : " WHERE " + whereClause);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String createDeleteWithJoins(final String fromTable,
			final String fromTableAlias, final String joinedTables,
			final String joinConditions, final String whereClause) {

		return "DELETE " + fromTableAlias + " FROM " + fromTable + " AS "
				+ fromTableAlias + ", " + joinedTables + " WHERE "
				+ joinConditions
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

		return "UPDATE " + table + " AS " + tableAlias + ", " + joinedTables
				+ " SET "
				+ setClause.replaceAll("(?<=^|,)(\\s*)(.)",
						"$1" + tableAlias + ".$2")
				+ " WHERE " + joinConditions
				+ (whereClause == null ? "" : " AND (" + whereClause + ")");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String lockTablesInShareMode(final String... tables) {

		final StringBuilder stmt = new StringBuilder(256);
		stmt.append("LOCK TABLES ");
		for (int i = 0; i < tables.length; i++) {
			if (i > 0)
				stmt.append(", ");
			stmt.append(tables[i]);
		}
		stmt.append(" READ");

		return stmt.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String lockTablesInExclusiveMode(final String... tables) {

		final StringBuilder stmt = new StringBuilder(256);
		stmt.append("LOCK TABLES ");
		for (int i = 0; i < tables.length; i++) {
			if (i > 0)
				stmt.append(", ");
			stmt.append(tables[i]);
		}
		stmt.append(" WRITE");

		return stmt.toString();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String unlockTables(final String... tables) {

		return "UNLOCK TABLES";
	}
}
