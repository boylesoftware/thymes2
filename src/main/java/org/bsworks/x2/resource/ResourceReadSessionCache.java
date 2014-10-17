package org.bsworks.x2.resource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;


/**
 * Resource read session cache.
 *
 * @author Lev Himmelfarb
 */
public class ResourceReadSessionCache {

	/**
	 * Context cache key prefix builder.
	 */
	private final StringBuilder contextBuilder = new StringBuilder(64);

	/**
	 * Stack of context cache key lengths.
	 */
	private final Deque<Integer> contextLens = new ArrayDeque<>();

	/**
	 * Current context cache key.
	 */
	private String context = "";

	/**
	 * The cache.
	 */
	private final Map<String, Object> cache = new HashMap<>();


	/**
	 * Enter new cache context.
	 *
	 * @param contextKey The context cache key.
	 */
	public void enterCacheContext(final String contextKey) {

		this.contextLens.push(Integer.valueOf(this.context.length()));
		this.contextBuilder.append(contextKey).append(".");
		this.context = this.contextBuilder.toString();
	}

	/**
	 * Leave current cache context.
	 */
	public void leaveCacheContext() {

		this.contextBuilder.setLength(this.contextLens.pop().intValue());
		this.context = this.contextBuilder.toString();
	}

	/**
	 * Get current context cache key.
	 *
	 * @return The key, empty string for the top-level context.
	 */
	public String getContext() {

		return this.context;
	}

	/**
	 * Get cached object.
	 *
	 * @param cacheKey Cache key.
	 *
	 * @return Cached object, or {@code null} if not in the cache.
	 */
	public Object get(final String cacheKey) {

		return this.cache.get(cacheKey);
	}

	/**
	 * Add object to the cache.
	 *
	 * @param cacheKey Cache key.
	 * @param obj The object.
	 */
	public void put(final String cacheKey, final Object obj) {

		this.cache.put(cacheKey, obj);
	}
}
