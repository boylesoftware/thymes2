package org.bsworks.x2.services.persistence;


/**
 * Represents a statement used to update data in the persistent storage. The
 * update statement may create new persistent records, update existing ones, or
 * delete them.
 *
 * @author Lev Himmelfarb
 */
public interface PersistenceUpdate {

	/**
	 * Set statement parameter.
	 *
	 * @param paramName Parameter name.
	 * @param paramType Parameter value type.
	 * @param paramValue Parameter value. May be {@code null}.
	 *
	 * @return This statement.
	 */
	PersistenceUpdate setParameter(String paramName,
			PersistentValueType paramType, Object paramValue);

	/**
	 * Execute the statement.
	 *
	 * @return Number of affected persistent records (e.g. database table rows
	 * in case of an RDBMS).
	 */
	long execute();
}
