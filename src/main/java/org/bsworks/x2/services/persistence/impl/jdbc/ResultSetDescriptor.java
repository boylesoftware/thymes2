package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceException;


/**
 * Result set descriptor.
 *
 * @author Lev Himmelfarb
 */
class ResultSetDescriptor {

	/**
	 * Resources manager.
	 */
	private final Resources resources;

	/**
	 * The result set.
	 */
	private final ResultSet rs;

	/**
	 * Session cache.
	 */
	private final ResourceReadSessionCache sessionCache;

	/**
	 * The data consumer.
	 */
	private final Actor actor;

	/**
	 * References fetch result, or {@code null}.
	 */
	private final Map<String, Object> refsFetchResult;

	/**
	 * Property names corresponding to the result set columns.
	 */
	private final String[] propNames;

	/**
	 * For each result set column, index of the next column that belongs to the
	 * same object.
	 */
	private final int[] nextColInds;

	/**
	 * For each result set column, tells if the column is a reference followed
	 * by a range of columns with the referred record data.
	 */
	private final boolean[] fetchedRefs;

	/**
	 * For each fetched reference result set column, a sub-result set parser.
	 */
	private final ResultSetParser[] fetchedRefParsers;


	/**
	 * Create new descriptor.
	 *
	 * @param resources Application resources manager.
	 * @param rs The result set.
	 * @param sessionCache Session cache, may be {@code null}.
	 * @param actor The parsed data consumer.
	 * @param refsFetchResult References fetch result, or {@code null}.
	 *
	 * @throws SQLException If a database error happens.
	 */
	ResultSetDescriptor(final Resources resources, final ResultSet rs,
			final ResourceReadSessionCache sessionCache,
			final Actor actor, final Map<String, Object> refsFetchResult)
		throws SQLException {

		this.resources = resources;
		this.rs = rs;
		this.sessionCache = sessionCache;
		this.actor = actor;
		this.refsFetchResult = refsFetchResult;

		final ResultSetMetaData rsmd = rs.getMetaData();
		final int numCols = rsmd.getColumnCount();
		this.propNames = new String[numCols + 1];
		this.nextColInds = new int[numCols + 1];
		this.fetchedRefs = new boolean[numCols + 1];
		this.fetchedRefParsers = new ResultSetParser[numCols + 1];
		this.propNames[0] = null;
		String lastPrefix = null;
		for (int colInd = 1; colInd <= numCols; colInd++) {
			final String colLabel = rsmd.getColumnLabel(colInd);
			final String prefix;
			try {
				final int sepInd = colLabel.lastIndexOf('$');
				final String propName;
				if (sepInd < 0) {
					prefix = "";
					propName = colLabel;
				} else {
					prefix = colLabel.substring(0, sepInd + 1);
					propName = colLabel.substring(sepInd + 1);
				}
				if (propName.endsWith(":")) {
					if (this.refsFetchResult == null)
						throw new IllegalArgumentException("Cannot have fetched"
								+ " references in the result set with no"
								+ " references fetch result map specified.");
					this.fetchedRefs[colInd] = true;
					this.propNames[colInd] =
						propName.substring(0, propName.length() - 1);
				} else {
					this.fetchedRefs[colInd] = false;
					this.propNames[colInd] = propName;
				}
			} catch (final IndexOutOfBoundsException e) {
				throw new PersistenceException(
						"Invalid result set column label \"" + colLabel + "\".",
						e);
			}
			if (colInd == 1) {
				this.nextColInds[0] = 1;
				lastPrefix = prefix;
			} if (prefix.equals(lastPrefix)) {
				this.nextColInds[colInd - 1] = colInd;
			} else if (this.nextColInds[colInd] == 0) {
				int nextColInd;
				for (nextColInd = colInd + 1; nextColInd <= numCols;
						nextColInd++) {
					final String l = rsmd.getColumnLabel(nextColInd);
					if (!l.startsWith(prefix) && l.startsWith(lastPrefix))
						break;
				}
				if (nextColInd > numCols)
					nextColInd = 0;
				else
					this.nextColInds[nextColInd] = -1;
				this.nextColInds[colInd - 1] = nextColInd;
				lastPrefix = prefix;
			} else {
				this.nextColInds[colInd - 1] = 0;
				lastPrefix = prefix;
			}
		}
		this.nextColInds[numCols] = 0;
	}


	/**
	 * Get the result set.
	 *
	 * @return The result set.
	 */
	ResultSet getResultSet() {

		return this.rs;
	}

	/**
	 * Get session cache.
	 *
	 * @return Session cache, or {@code null} if not used.
	 */
	ResourceReadSessionCache getSessionCache() {

		return this.sessionCache;
	}

	/**
	 * Get the parsed data consumer.
	 *
	 * @return The consumer actor, or {@code null} if unauthenticated.
	 */
	Actor getActor() {

		return this.actor;
	}

	/**
	 * Get name of the property corresponding to the specified result set
	 * column.
	 *
	 * @param colInd The column index.
	 *
	 * @return The property name. For index {@code 0} always returns
	 * {@code null}.
	 */
	String getPropertyName(final int colInd) {

		return this.propNames[colInd];
	}

	/**
	 * Get index of the next column that corresponds to a property belonging to
	 * the same object as the specified column.
	 *
	 * @param colInd The column index.
	 *
	 * @return The next column index, or {@code 0} if no more properties in the
	 * result set belong to the same object.
	 */
	int getNextColumnIndex(final int colInd) {

		return this.nextColInds[colInd];
	}

	/**
	 * Get sub-result set parser for the specified fetched reference column.
	 *
	 * @param colInd The column index.
	 * @param ctx Current parser context to become parent context for the
	 * sub-parser.
	 *
	 * @return The parser to use, or {@code null} if not a fetched reference
	 * column.
	 */
	ResultSetParser getFetchedReferenceRSParser(final int colInd,
			final ResultSetParserContext ctx) {

		if (!this.fetchedRefs[colInd])
			return null;

		ResultSetParser parser = this.fetchedRefParsers[colInd];
		if (parser == null)
			parser = this.fetchedRefParsers[colInd] =
				new ResultSetParser(this.resources, this, ctx, colInd + 1,
						colInd + 1, this.refsFetchResult);
		else
			parser.reset();

		return parser;
	}
}
