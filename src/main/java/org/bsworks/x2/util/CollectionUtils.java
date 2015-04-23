package org.bsworks.x2.util;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Utility methods for working with collections.
 *
 * @author Lev Himmelfarb
 */
public final class CollectionUtils {

	/**
	 * Empty sorted map.
	 */
	private static final SortedMap<Object, Object> EMPTY_SORTED_MAP =
		Collections.unmodifiableSortedMap(new TreeMap<>());


	/**
	 * All methods are static.
	 */
	private CollectionUtils() {}


	/**
	 * Get empty sorted map.
	 *
	 * @param <K> Key type.
	 * @param <V> Value type.
	 *
	 * @return Unmodifiable empty sorted map.
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> SortedMap<K, V> emptySortedMap() {

		return (SortedMap<K, V>) EMPTY_SORTED_MAP;
	}
}
