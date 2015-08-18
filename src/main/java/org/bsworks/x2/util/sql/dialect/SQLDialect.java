package org.bsworks.x2.util.sql.dialect;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.services.persistence.ParameterValue;
import org.bsworks.x2.services.persistence.ParameterValuesFactory;


/**
 * SQL dialect.
 *
 * @author Lev Himmelfarb
 */
public interface SQLDialect {

	/**
	 * Query parameter name used for the range offset.
	 */
	static final String RANGE_OFFSET_PARAM = "rangeOffset";

	/**
	 * Query parameter name used for the range limit.
	 */
	static final String RANGE_LIMIT_PARAM = "rangeLimit";


	/**
	 * Tell if working with temporary tables require read-write transaction
	 * mode.
	 *
	 * @return {@code true} if read-write transaction mode is required.
	 */
	boolean tempTablesRequireReadWrite();

	/**
	 * Quote column label in the select list.
	 *
	 * @param colLabel The label.
	 *
	 * @return Quoted label.
	 */
	String quoteColumnLabel(String colLabel);

	/**
	 * Create "SELECT" clause column expression that concatenates the specified
	 * "SELECT" clause column expression to the specified string literal, but if
	 * the column expression evaluates to SQL "NULL", the result of the whole
	 * concatenation is SQL "NULL".
	 *
	 * @param stringLiteral The string literal.
	 * @param selectExpr Nullable column expression.
	 *
	 * @return The concatenation expression.
	 */
	String nullableConcat(String stringLiteral, String selectExpr);

	/**
	 * Create new SQL value expression from the specified SQL value expression
	 * cast to SQL type that can be used with string SQL functions.
	 *
	 * @param valExpr Value expression.
	 *
	 * @return String value expression.
	 */
	String castToString(String valExpr);

	/**
	 * Get SQL expression for getting the length of the specified string
	 * expression result.
	 *
	 * @param valExpr Value expression, must be usable by SQL string length
	 * function.
	 *
	 * @return SQL expression for the value expression's length.
	 */
	String stringLength(String valExpr);

	/**
	 * Get SQL expression that pads the result of the specified expression on
	 * the left with the specified character to make it at least the specified
	 * length.
	 *
	 * @param valExpr Value expression, must be usable by SQL string padding
	 * function.
	 * @param width Minimum result width.
	 * @param paddingChar Padding character.
	 *
	 * @return SQL expression for the value expression's length.
	 */
	String stringLeftPad(String valExpr, int width, char paddingChar);

	/**
	 * Get SQL expression that extracts a substring from the result of the
	 * specified string expression.
	 *
	 * @param valExpr Value expression, must be usable by SQL string substring
	 * function.
	 * @param from Zero-based index of the first included substring character.
	 * @param length Maximum substring length, or zero or negative for the rest
	 * of the string.
	 *
	 * @return SQL expression for the substring.
	 */
	String stringSubstring(String valExpr, int from, int length);

	/**
	 * Create Boolean expression that tests if a value contains a substring that
	 * matches a regular expression.
	 *
	 * @param valExpr SQL expression for the value to test.
	 * @param reExpr SQL expression for the regular expression.
	 * @param negate {@code true} to test if does not match the regular
	 * expression.
	 * @param caseSensitive {@code true} if case sensitive.
	 *
	 * @return SQL expression that evaluates to a Boolean.
	 */
	String regularExpressionMatch(String valExpr, String reExpr,
			boolean negate, boolean caseSensitive);

	/**
	 * Create Boolean expression that tests a value contains a substring.
	 *
	 * @param valExpr SQL expression for the value to test.
	 * @param substringExpr SQL expression for the substring value.
	 * @param negate {@code true} to test if does not contain the substring.
	 * @param caseSensitive {@code true} if case sensitive.
	 *
	 * @return SQL expression that evaluates to a Boolean.
	 */
	String substringMatch(String valExpr, String substringExpr, boolean negate,
			boolean caseSensitive);

	/**
	 * Create Boolean expression that tests a value starts with a prefix.
	 *
	 * @param valExpr SQL expression for the value to test.
	 * @param prefixExpr SQL expression for the prefix value.
	 * @param negate {@code true} to test if does not start with the prefix.
	 * @param caseSensitive {@code true} if case sensitive.
	 *
	 * @return SQL expression that evaluates to a Boolean.
	 */
	String prefixMatch(String valExpr, String prefixExpr, boolean negate,
			boolean caseSensitive);

	/**
	 * Make the specified "SELECT" query ranged.
	 *
	 * @param <V> Parameter value type.
	 * @param selectQuery The "SELECT" query.
	 * @param range Range specification.
	 * @param paramsFactory Query parameter values factory to use.
	 * @param params Map, to which to add range parameters. The parameters
	 * added, if any, have names {@value #RANGE_OFFSET_PARAM} and
	 * {@value #RANGE_LIMIT_PARAM}.
	 *
	 * @return Ranged "SELECT" query.
	 */
	<V extends ParameterValue> String makeRangedSelect(String selectQuery,
			RangeSpec range, ParameterValuesFactory<V> paramsFactory,
			Map<String, V> params);

	/**
	 * Get statements for selecting the specified query's data into a
	 * transaction scope temporary table.
	 *
	 * @param tempTableName The temporary table name.
	 * @param create Tells if the temporary table has not been created yet in
	 * the current transaction.
	 * @param selectQuery The "SELECT" query.
	 * @param preStatements List, to which this method adds statements to be
	 * executed to create and populate the temporary table.
	 * @param postStatements List, to which this method adds statements to be
	 * executed after the temporary tables is no longer needed.
	 */
	void makeSelectIntoTempTable(String tempTableName, boolean create,
			String selectQuery, List<String> preStatements,
			List<String> postStatements);

	/**
	 * Make the specified "SELECT" query lock the rows in share mode.
	 *
	 * @param selectQuery The "SELECT" query.
	 * @param lockTables List of tables that participate in the query that need
	 * to be locked.
	 *
	 * @return The "SELECT" query that also locks the specified tables.
	 */
	String makeSelectWithShareLock(String selectQuery, String... lockTables);

	/**
	 * Make the specified "SELECT" query lock the rows in exclusive mode.
	 *
	 * @param selectQuery The "SELECT" query.
	 * @param lockTables List of tables that participate in the query that need
	 * to be locked.
	 *
	 * @return The "SELECT" query that also locks the specified tables.
	 */
	String makeSelectWithExclusiveLock(String selectQuery,
			String... lockTables);

	/**
	 * Create "DELETE" statement that deletes rows from the specified table with
	 * the specified "WHERE" clause, in which the table is addressed using an
	 * alias.
	 *
	 * @param fromTable Name of the table to delete from.
	 * @param fromTableAlias Alias for the "FROM" table used in the the "WHERE"
	 * clause.
	 * @param whereClause Body of the "WHERE" clause. May be {@code null} for
	 * nothing.
	 *
	 * @return The constructed "DELETE" statement.
	 */
	String createDeleteFromAliasedTable(String fromTable, String fromTableAlias,
			String whereClause);

	/**
	 * Create "DELETE" statement that deletes rows from the specified table and
	 * uses specified joined reference tables in the "WHERE" clause.
	 *
	 * @param fromTable Name of the table to delete from.
	 * @param fromTableAlias Alias for the "FROM" table used in the join
	 * conditions and the "WHERE" clause.
	 * @param joinedTables Comma-separated list of joined reference tables.
	 * @param joinConditions Boolean expression containing the conditions for
	 * the reference table joins.
	 * @param whereClause Body of the "WHERE" clause, which may use tables from
	 * the joined reference tables list. May be {@code null} for nothing.
	 *
	 * @return The constructed "DELETE" statement.
	 */
	String createDeleteWithJoins(String fromTable, String fromTableAlias,
			String joinedTables, String joinConditions, String whereClause);

	/**
	 * Create "UPDATE" statement that updates specified table and uses specified
	 * joined reference tables in the "WHERE" clause.
	 *
	 * @param table Name of the table to update.
	 * @param tableAlias Alias of the updated table used in the join conditions
	 * and the "WHERE" clause.
	 * @param setClause Body of the "SET" clause.
	 * @param joinedTables Comma-separated list of joined reference tables.
	 * @param joinConditions Boolean expression containing the conditions for
	 * the reference table joins.
	 * @param whereClause Body of the "WHERE" clause, which may use tables from
	 * the joined reference tables list. May be {@code null} for nothing.
	 *
	 * @return The constructed "UPDATE" statement.
	 */
	String createUpdateWithJoins(String table, String tableAlias,
			String setClause, String joinedTables, String joinConditions,
			String whereClause);

	/**
	 * Create statement for placing a share lock on the specified tables.
	 *
	 * @param tables The table names.
	 *
	 * @return The lock statement.
	 */
	String lockTablesInShareMode(String... tables);

	/**
	 * Create statement for placing an exclusive lock on the specified tables.
	 *
	 * @param tables The table names.
	 *
	 * @return The lock statement.
	 */
	String lockTablesInExclusiveMode(String... tables);

	/**
	 * Create statement for unlocking all previously locked tables.
	 *
	 * @param tables Name of previously locked tables. This is only a hint to
	 * the dialect - depending on the database implementation, the resulting
	 * statement may be releasing all locks regardless of the specified list.
	 *
	 * @return The unlock statement, or {@code null} if locks are released by
	 * the database implementation automatically upon transaction end.
	 */
	String unlockTables(String... tables);
}
