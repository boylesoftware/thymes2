package org.bsworks.x2.services.persistence.impl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.persistence.PersistenceException;
import org.bsworks.x2.services.persistence.PersistenceUpdate;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Persistence update statement implementation.
 *
 * @author Lev Himmelfarb
 */
class PersistenceUpdateImpl
	extends AbstractPersistenceStatement
	implements PersistenceUpdate {

	/**
	 * Create new statement.
	 *
	 * @param resources Application resources manager.
	 * @param con Database connection.
	 * @param paramsFactory Query parameter value handlers factory.
	 * @param stmtText The statement text.
	 * @param params Initial parameters. May be {@code null} for none.
	 */
	PersistenceUpdateImpl(final Resources resources, final Connection con,
			final ParameterValuesFactoryImpl paramsFactory,
			final String stmtText,
			final Map<String, JDBCParameterValue> params) {
		super(resources, con, paramsFactory, params);

		this.setStatementText(stmtText);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceUpdate setParameter(final String paramName,
			final PersistentValueType paramType, final Object paramValue) {

		this.setStatementParameter(paramName, paramType, paramValue);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public long execute() {

		final int count;
		try (final PreparedStatement pstmt = this.getPreparedStatement()) {
			count = pstmt.executeUpdate();
			Utils.logWarnings(this.log, pstmt.getWarnings());
		} catch (final SQLException e) {
			throw new PersistenceException(e);
		}

		return count;
	}
}
