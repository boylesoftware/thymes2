package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.SQLWarning;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.bsworks.x2.services.persistence.PersistenceException;


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
			"\\?\\??([a-zA-Z]\\w+)|(')");

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
	 * Convert named parameter placeholders in the specified SQL to positional
	 * parameters directly supported by JDBC.
	 *
	 * @param sql SQL with named parameter placeholders.
	 * @param namedParams Named parameter values by parameter names.
	 * @param paramsList Output list for positional parameter values.
	 *
	 * @return SQL text with positional parameter placeholders.
	 */
	static String convertNamedParams(final String sql,
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
			} else if (m.group(2) != null) {
				m.appendReplacement(resBuf, m.group());
				m.usePattern(LITERAL_END_PATTERN);
				inLiteral = true;
			} else {
				final String paramName = m.group(1);
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
			}
		}
		if (inLiteral)
			throw new PersistenceException("String literal in the query text is"
					+ " not closed.");
		m.appendTail(resBuf);

		return resBuf.toString();
	}
}
