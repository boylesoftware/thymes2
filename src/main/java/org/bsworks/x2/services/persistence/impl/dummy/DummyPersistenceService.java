package org.bsworks.x2.services.persistence.impl.dummy;

import org.bsworks.x2.Actor;
import org.bsworks.x2.services.persistence.PersistenceService;
import org.bsworks.x2.services.persistence.PersistenceTransactionHandler;


/**
 * "Dummy" persistence service implementation.
 *
 * @author Lev Himmelfarb
 */
class DummyPersistenceService
	implements PersistenceService {

	/**
	 * Throws {@link UnsupportedOperationException}.
	 */
	@Override
	public PersistenceTransactionHandler createPersistenceTransaction(
			final Actor actor, final boolean readOnly) {

		throw new UnsupportedOperationException(
				"Persistence is not supported by the application.");
	}

	/**
	 * Throws {@link UnsupportedOperationException}.
	 */
	@Override
	public boolean supportsEmbeddedCollections() {

		throw new UnsupportedOperationException(
				"Persistence is not supported by the application.");
	}
}
