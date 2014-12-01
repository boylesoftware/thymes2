package org.bsworks.x2.core;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.services.auth.ActorAuthenticationService;


/**
 * Actor resolver with internal caching.
 *
 * @author Lev Himmelfarb
 */
class CachingAuthResolver {

	/**
	 * Cache element.
	 */
	private class CacheElement {

		/**
		 * Cache element creation timestamp.
		 */
		private final long ts = System.currentTimeMillis();

		/**
		 * Requested actor id.
		 */
		private final String actorId;

		/**
		 * Requested actor opaque value.
		 */
		private final String opaque;

		/**
		 * Tells if actor has been loaded.
		 */
		private volatile boolean loaded;

		/**
		 * The cached actor.
		 */
		private Actor actor;

		/**
		 * Tells if refresh has been requested.
		 */
		private AtomicBoolean refreshing = new AtomicBoolean(false);


		/**
		 * Create initialized cache element with the specified actor.
		 *
		 * @param actor The actor.
		 */
		CacheElement(final Actor actor) {

			this.actorId = null;
			this.opaque = null;
			this.loaded = true;
			this.actor = actor;
		}

		/**
		 * Create uninitialized cache element that loads the actor upon get.
		 *
		 * @param actorId Requested actor id.
		 * @param opaque Requested actor opaque value.
		 */
		CacheElement(final String actorId, final String opaque) {

			this.actorId = actorId;
			this.opaque = opaque;
			this.loaded = false;
		}


		/**
		 * Get cache element age.
		 *
		 * @param now Now.
		 *
		 * @return Age in milliseconds.
		 */
		long getAge(final long now) {

			return now - this.ts;
		}

		/**
		 * Notify the element that refresh has been requested.
		 *
		 * @return {@code false} if already requested earlier.
		 */
		boolean startRefresh() {

			return !this.refreshing.getAndSet(true);
		}

		/**
		 * Get cached actor. If not initialized, block and load the actor.
		 *
		 * @return The actor.
		 */
		Actor getActor() {

			if (!this.loaded) {
				synchronized (this) {
					if (!this.loaded) {
						if (CachingAuthResolver.this.log.isDebugEnabled())
							CachingAuthResolver.this.log.debug(
									"synchronously loading actor");
						this.actor = CachingAuthResolver.this.getAuthService()
								.getActor(this.actorId, this.opaque);
						this.loaded = true;
					} else {
						if (CachingAuthResolver.this.log.isDebugEnabled())
							CachingAuthResolver.this.log.debug(
									"another thread synchronously loaded"
									+ " actor");
					}
				}
			}

			return this.actor;
		}
	}


	/**
	 * The log.
	 */
	final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Runtime context.
	 */
	final RuntimeContextImpl runtimeCtx;

	/**
	 * Maximum cache size.
	 */
	private final int maxCacheSize;

	/**
	 * Discard cached actor after this number of milliseconds.
	 */
	private final long discardAfter;

	/**
	 * Refresh cached actor after this number of milliseconds.
	 */
	private final long refreshAfter;

	/**
	 * Lock used to access the cache.
	 */
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Cached actors by cache keys.
	 */
	final LinkedHashMap<String, CacheElement> cache = new LinkedHashMap<>();


	/**
	 * Create new resolver.
	 *
	 * @param sc Servlet context.
	 * @param runtimeCtx Runtime context.
	 *
	 * @throws InitializationException If not configured correctly.
	 */
	CachingAuthResolver(final ServletContext sc,
			final RuntimeContextImpl runtimeCtx)
		throws InitializationException {

		this.runtimeCtx = runtimeCtx;

		this.maxCacheSize = Integer.parseInt(sc.getInitParameter(
				"x2.auth.cache.maxSize"));
		if (this.maxCacheSize <= 0)
			throw new InitializationException(
					"Invalid maximum authentication cache size.");
		this.discardAfter = Long.parseLong(sc.getInitParameter(
				"x2.auth.cache.discardAfter"));
		if (this.discardAfter < 0)
			throw new InitializationException(
					"Invalid authentication cache expiration timeout.");
		this.refreshAfter = Long.parseLong(sc.getInitParameter(
				"x2.auth.cache.refreshAfter"));
		if ((this.refreshAfter < 0) || ((this.discardAfter > 0)
				&& (this.refreshAfter >= this.discardAfter)))
			throw new InitializationException(
					"Invalid authentication cache refresh timeout.");
	}


	/**
	 * Get actor.
	 *
	 * @param actorId Actor id.
	 * @param opaque Actor opaque value, or {@code null} if not used.
	 *
	 * @return The actor, or {@code null} if not found.
	 */
	Actor getActor(final String actorId, final String opaque) {

		final boolean debug = this.log.isDebugEnabled();

		// caching disabled?
		if (this.discardAfter == 0)
			return this.getAuthService().getActor(actorId, opaque);

		// get the key
		final String key = createKey(actorId, opaque);

		// look up cached element
		final Lock readLock = this.lock.readLock();
		final Lock writeLock = this.lock.writeLock();
		readLock.lock();
		CacheElement cacheEl = this.cache.get(key);
		if (cacheEl == null) {
			readLock.unlock();
			if (debug)
				this.log.debug("no cached element, will perform synchronous"
						+ " lookup");
			writeLock.lock();
			try {
				cacheEl = this.cache.get(key);
				if (cacheEl == null) {
					this.purgeExpired();
					cacheEl = new CacheElement(actorId, opaque);
					this.cache.put(key, cacheEl);
				} else {
					if (debug)
						this.log.debug("another thread added cached element");
				}
				readLock.lock();
			} finally {
				writeLock.unlock();
			}
		} else {
			final long age = cacheEl.getAge(System.currentTimeMillis());
			if (age >= this.discardAfter) {
				readLock.unlock();
				if (debug)
					this.log.debug("cached element expired, will perform"
							+ " synchronous lookup");
				writeLock.lock();
				try {
					cacheEl = this.cache.get(key);
					if ((cacheEl == null)
							|| (cacheEl.getAge(System.currentTimeMillis())
									>= this.discardAfter)) {
						this.purgeExpired();
						cacheEl = new CacheElement(actorId, opaque);
						this.cache.put(key, cacheEl);
					} else {
						if (debug)
							this.log.debug("another thread added cached"
									+ " element");
					}
					readLock.lock();
				} finally {
					writeLock.unlock();
				}
			} else if ((age >= this.refreshAfter) && cacheEl.startRefresh()) {
				if (debug)
					this.log.debug("will refresh cached element");
				this.runtimeCtx.submitSideTask(new Runnable() {
					@Override
					public void run() {
						if (debug)
							CachingAuthResolver.this.log.debug(
									"refreshing cached element");
						final CacheElement newCacheEl = new CacheElement(
								CachingAuthResolver.this.getAuthService()
									.getActor(actorId, opaque));
						writeLock.lock();
						CachingAuthResolver.this.cache.put(key, newCacheEl);
						writeLock.unlock();
					}
				});
			}
		}

		// get actor from the cache element
		try {
			return cacheEl.getActor();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Purge expired cache elements and make room for one new element.
	 */
	private void purgeExpired() {

		final int origSize = this.cache.size();

		final long now = System.currentTimeMillis();
		final Iterator<CacheElement> i = this.cache.values().iterator();
		while (i.hasNext()) {
			if (i.next().getAge(now) >= this.discardAfter)
				i.remove();
			else
				break;
		}

		final int numToRemove = this.cache.size() - this.maxCacheSize;
		for (int c = 0; c < numToRemove; c++)
			i.remove();

		if (this.log.isDebugEnabled())
			this.log.debug("purged " + (origSize - this.cache.size())
					+ " expired elements from the cache");
	}

	/**
	 * Get actor authentication service.
	 *
	 * @return Actor authentication service.
	 */
	ActorAuthenticationService getAuthService() {

		return this.runtimeCtx.getActorAuthenticationService();
	}

	/**
	 * Create cache key.
	 *
	 * @param actorId Actor id.
	 * @param opaque Actor opaque value, or {@code null} if not used.
	 *
	 * @return Cache key.
	 */
	private static String createKey(final String actorId,
			final String opaque) {

		if (opaque == null)
			return actorId;

		return actorId + "&" + opaque;
	}
}
