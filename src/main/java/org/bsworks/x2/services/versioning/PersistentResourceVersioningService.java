package org.bsworks.x2.services.versioning;

import java.util.Set;

import org.bsworks.x2.EssentialService;
import org.bsworks.x2.services.persistence.PersistenceTransaction;


/**
 * Service responsible for tracking persistent resource collection versions.
 *
 * @author Lev Himmelfarb
 */
public interface PersistentResourceVersioningService
	extends EssentialService {

	/**
	 * Get combined version information about several persistent resource
	 * collections.
	 *
	 * @param tx Persistence transaction.
	 * @param prsrcClasses The persistent resource classes. Must not be empty.
	 *
	 * @return Combined version information. The version number is a sum of all
	 * individual collection version numbers. The last modification timestamp is
	 * the latest modification timestamp among the collections.
	 */
	PersistentResourceVersionInfo getCollectionsVersionInfo(
			PersistenceTransaction tx, Set<Class<?>> prsrcClasses);

	/**
	 * Register event of persistent resource collections modification.
	 *
	 * @param tx Persistence transaction.
	 * @param prsrcClasses Persistent resource classes.
	 */
	void registerCollectionsModification(PersistenceTransaction tx,
			Set<Class<?>> prsrcClasses);

	/**
	 * Lock specified collections modification until the transaction end.
	 *
	 * @param tx Persistence transaction.
	 * @param prsrcClasses Persistent resource classes.
	 */
	void lockCollections(PersistenceTransaction tx, Set<Class<?>> prsrcClasses);
}
