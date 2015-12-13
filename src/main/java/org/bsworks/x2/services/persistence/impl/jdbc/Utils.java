package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.SQLWarning;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.util.sql.dialect.SQLDialect;


/**
 * Collection of common utility methods used by the service implementation.
 *
 * @author Lev Himmelfarb
 */
final class Utils {

	/**
	 * Pattern used to process SQL to find named parameter placeholders and
	 * string literals.
	 */
	private static final Pattern TOKEN_PATTERN = Pattern.compile(
			"\\{([^}]+)\\}|\\?\\??([a-zA-Z]\\w+)|(')");

	/**
	 * Pattern used to find end of string literal in SQL.
	 */
	private static final Pattern LITERAL_END_PATTERN = Pattern.compile(
			"(?<!')'");


	/**
	 * All methods are static.
	 */
	private Utils() {}


	/**
	 * Log SQL warnings.
	 *
	 * @param log The log.
	 * @param firstWarning First warning in the chain, or {@code null} for no
	 * warnings.
	 */
	static void logWarnings(final Log log, final SQLWarning firstWarning) {

		for (SQLWarning warn = firstWarning; warn != null;
				warn = warn.getNextWarning()) {
			final String msg = "database warning #" + warn.getErrorCode()
					+ " (" + warn.getSQLState() + "): " + warn.getMessage();
			if (warn.getCause() != null)
				log.warn(msg, warn.getCause());
			else
				log.warn(msg);
		}
	}

	/**
	 * Process specified SQL statement and replace all macro-placeholders with
	 * corresponding values. That includes replacing resource and resource
	 * property macros with table and column names and converting named
	 * parameter placeholders to positional parameters directly supported by
	 * JDBC.
	 *
	 * @param resources Application resources manager.
	 * @param sql SQL with resource macros and named parameter placeholders.
	 * @param namedParams Named parameter values by parameter names.
	 * @param paramsList Output list for positional parameter values.
	 *
	 * @return SQL text ready to be submitted to JDBC.
	 */
	static String processSQL(final Resources resources, final String sql,
			final Map<String, JDBCParameterValue> namedParams,
			final List<JDBCParameterValue> paramsList) {

		final StringBuffer resBuf = new StringBuffer(sql.length() + 64);
		final Matcher m = TOKEN_PATTERN.matcher(sql);
		boolean inLiteral = false;
		final StringBuilder colParamBuf = new StringBuilder(128);
		while (m.find()) {
			if (inLiteral) {
				m.appendReplacement(resBuf, m.group());
				m.usePattern(TOKEN_PATTERN);
				inLiteral = false;
			} else if (m.group(3) != null) {
				m.appendReplacement(resBuf, m.group());
				m.usePattern(LITERAL_END_PATTERN);
				inLiteral = true;
			} else if (m.group(2) != null) {
				final String paramName = m.group(2);
				final JDBCParameterValue paramHandler =
					namedParams.get(paramName);
				if (paramHandler == null)
					throw new PersistenceException("Parameter \"" + paramName
							+ "\" is undefined.");
				final int numPlaceholders = paramHandler.getNumPlaceholders();
				if (m.group().charAt(1) == '?') {
					colParamBuf.setLength(0);
					for (int i = 0; i < numPlaceholders; i++) {
						if (i > 0)
							colParamBuf.append(", ?");
						else
							colParamBuf.append("?");
					}
					m.appendReplacement(resBuf, colParamBuf.toString());
				} else {
					if (numPlaceholders > 1)
						throw new PersistenceException("Parameter \""
								+ paramName + "\" cannot be a collection.");
					m.appendReplacement(resBuf, "?");
				}
				paramsList.add(paramHandler);
			} else {
				final String expr = m.group(1);
				final int dotInd = expr.indexOf('.');
				if (dotInd < 0) {
					m.appendReplacement(resBuf,
							resources.getPersistentResourceHandler(expr)
								.getPersistentCollectionName());
				} else {
					final PersistentResourceHandler<?> prsrcHandler =
						resources.getPersistentResourceHandler(
								expr.substring(0, dotInd));
					m.appendReplacement(resBuf,
							prsrcHandler
								.getPersistentPropertyChain(
										expr.substring(dotInd + 1))
								.getLast()
								.getPersistence()
								.getFieldName());
				}
			}
		}
		if (inLiteral)
			throw new PersistenceException("String literal in the query text is"
					+ " not closed.");
		m.appendTail(resBuf);

		return resBuf.toString();
	}

	/**
	 * Apply transformation function to the specified value expression.
	 *
	 * @param dialect SQL dialect.
	 * @param valueExpr Value expression.
	 * @param valueType Value expression type.
	 * @param func Transformation function.
	 * @param funcParams Transformation function parameters.
	 *
	 * @return Transformed value expression.
	 */
	static String getTransformedValueExpression(final SQLDialect dialect,
			final String valueExpr, final PersistentValueType valueType,
			final PropertyValueFunction func, final Object[] funcParams) {

		switch (func) {
		case LENGTH:
			return dialect.stringLength(
					valueType == PersistentValueType.STRING ?
					valueExpr : dialect.castToString(valueExpr)
			);
		case LOWERCASE:
			return dialect.stringLowercase(
					valueType == PersistentValueType.STRING ?
					valueExpr : dialect.castToString(valueExpr)
			);
		case SUBSTRING:
			return dialect.stringSubstring(
					(valueType == PersistentValueType.STRING ?
						valueExpr : dialect.castToString(valueExpr)),
					((Integer) funcParams[0]).intValue(),
					((Integer) funcParams[1]).intValue());
		case LPAD:
			return dialect.stringLeftPad(
					(valueType == PersistentValueType.STRING ?
						valueExpr : dialect.castToString(valueExpr)),
					((Integer) funcParams[0]).intValue(),
					((Character) funcParams[1]).charValue());
		default:
			return valueExpr;
		}
	}

	/**
	 * Get resulting persistent value type after applying the specified value
	 * transformation function to the input of the specified type.
	 *
	 * @param valueType The input type.
	 * @param func The function.
	 *
	 * @return The result type.
	 */
	static PersistentValueType getTransformedValueType(
			final PersistentValueType valueType,
			final PropertyValueFunction func) {

		switch (func) {
		case LENGTH:
			return PersistentValueType.NUMERIC;
		case LOWERCASE:
		case SUBSTRING:
		case LPAD:
			return PersistentValueType.STRING;
		default:
			return valueType;
		}
	}
}
