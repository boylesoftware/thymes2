package org.bsworks.x2.services.persistence;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EssentialService;


/**
 * Interface for the service that provides persistent storage to the
 * application. The persistent storage is used to store persistent resource
 * records.
 *
 * @author Lev Himmelfarb
 */
public interface PersistenceService
	extends EssentialService {

	/**
	 * Create and begin a persistence transaction.
	 *
	 * <p>This method can be used to access the application database from
	 * outside of an endpoint call context (for example, in a side task). The
	 * transaction has to be committed and closed by the caller using the
	 * returned by this method transaction handler.
	 *
	 * @param actor Actor performing the transaction, or {@code null} for
	 * unauthenticated transaction.
	 * @param readOnly {@code true} if transaction is read-only (does not
	 * attempt to modify any data in the database).
	 *
	 * @return The persistence transaction handler.
	 */
	PersistenceTransactionHandler createPersistenceTransaction(
			Actor actor, boolean readOnly);

	/**
	 * Tells whether the backing persistent storage technology supports
	 * collection fields inside persistent records. If not supported (e.g.
	 * SQL-based RDBMS), collection properties must be stored in their own
	 * persistent collections. If supported (e.g. many NoSQL databases), such
	 * properties may be stored in the same collection as the containing object.
	 *
	 * @return {@code true} if embedded collections are supported.
	 */
	boolean supportsEmbeddedCollections();
}
