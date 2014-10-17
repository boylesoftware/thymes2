package org.bsworks.x2.services.versioning;

import java.util.Date;


/**
 * Information about version of a persistent resource record, collection or a
 * combination of collections.
 *
 * @author Lev himmelfarb
 */
public interface PersistentResourceVersionInfo {

	/**
	 * Get version number.
	 *
	 * @return Version number.
	 */
	long getVersion();

	/**
	 * Get timestamp of the last modification.
	 *
	 * @return Last modification timestamp.
	 */
	Date getLastModificationTimestamp();
}
