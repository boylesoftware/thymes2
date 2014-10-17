package org.bsworks.x2.services.versioning.impl.memory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bsworks.x2.services.persistence.PersistenceTransaction;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;
import org.bsworks.x2.services.versioning.impl.PersistentResourceVersionInfoImpl;


/**
 * Memory-based persistent resource versioning service implementation.
 *
 * @author Lev Himmelfarb
 */
class MemoryPersistentResourceVersioningService
	implements PersistentResourceVersioningService {

	/**
	 * Persistent resource collection descriptor.
	 *
	 * @author Lev Himmelfarb
	 */
	private static final class PRsrcColDesc {

		/**
		 * Collection version.
		 */
		int version = 1;

		/**
		 * Collection last modification timestamp.
		 */
		long lastModTS = System.currentTimeMillis();


		/**
		 * Create and initialize new descriptor.
		 */
		PRsrcColDesc() {}
	}


	/**
	 * Instance startup timestamp.
	 */
	private final long startupTS;

	/**
	 * Collection descriptors by persistent resource classes.
	 */
	private final Map<Class<?>, PRsrcColDesc> collections = new HashMap<>();

	/**
	 * Lock for accessing the collections data.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();


	/**
	 * Create new service instance.
	 */
	MemoryPersistentResourceVersioningService() {

		this.startupTS = System.currentTimeMillis();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceVersionInfo getCollectionsVersionInfo(
			final PersistenceTransaction tx, final Set<Class<?>> prsrcClasses) {

		// gather information from descriptors
		long sumVersion = this.startupTS;
		long lastModTS = 0;
		final Lock rl = this.lock.readLock();
		rl.lock();
		boolean needsUnlock = true;
		try {
			for (final Class<?> prsrcClass : prsrcClasses) {
				PRsrcColDesc colDesc = this.collections.get(prsrcClass);
				if (colDesc == null) {
					rl.unlock();
					needsUnlock = false;
					final Lock wl = this.lock.writeLock();
					wl.lock();
					try {
						colDesc = this.collections.get(prsrcClass);
						if (colDesc == null) {
							colDesc = new PRsrcColDesc();
							this.collections.put(prsrcClass, colDesc);
						}
						rl.lock();
						needsUnlock = true;
					} finally {
						wl.unlock();
					}
				}
				synchronized (colDesc) {
					sumVersion += colDesc.version;
					if (colDesc.lastModTS > lastModTS)
						lastModTS = colDesc.lastModTS;
				}
			}
		} finally {
			if (needsUnlock)
				rl.unlock();
		}

		// return the result
		return new PersistentResourceVersionInfoImpl(sumVersion,
				new Date(lastModTS));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void registerCollectionsModification(final PersistenceTransaction tx,
			final Set<Class<?>> prsrcClasses) {

		// update descriptors
		final long now = System.currentTimeMillis();
		final Lock rl = this.lock.readLock();
		rl.lock();
		boolean needsUnlock = true;
		try {
			for (final Class<?> prsrcClass : prsrcClasses) {
				PRsrcColDesc colDesc = this.collections.get(prsrcClass);
				if (colDesc == null) {
					rl.unlock();
					needsUnlock = false;
					final Lock wl = this.lock.writeLock();
					wl.lock();
					try {
						colDesc = this.collections.get(prsrcClass);
						if (colDesc == null) {
							colDesc = new PRsrcColDesc();
							this.collections.put(prsrcClass, colDesc);
						}
						rl.lock();
						needsUnlock = true;
					} finally {
						wl.unlock();
					}
				}
				synchronized (colDesc) {
					colDesc.version++;
					colDesc.lastModTS = now;
				}
			}
		} finally {
			if (needsUnlock)
				rl.unlock();
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void lockCollections(final PersistenceTransaction tx,
			final Set<Class<?>> prsrcClasses) {

		// does not do anything at the moment
	}
}
