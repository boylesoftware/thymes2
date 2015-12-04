package org.bsworks.x2.services.persistence;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Result of a persistent resource records fetch. The class is a resource that
 * can be serialized and contains properties "records", "refs" and "totalCount".
 *
 * @param <R> Persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceFetchResult<R> {

	/**
	 * Persistent resource records.
	 */
	private final List<R> records;

	/**
	 * Total count of persistent resource records if the fetch request range was
	 * limited and the count requested.
	 */
	private final long totalCount;

	/**
	 * Fetched referred persistent resource records by string representations of
	 * their corresponding references.
	 */
	private final Map<String, Object> refs;


	/**
	 * Create and initialize new result object.
	 *
	 * @param records Fetched persistent resource records. Cannot be
	 * {@code null}, but can be empty.
	 * @param totalCount Total count of persistent resource records if the fetch
	 * request range was limited and the count requested.
	 * @param refs Fetched referred persistent resource records by string
	 * representations of their corresponding references. May be {@code null}.
	 */
	public PersistentResourceFetchResult(final List<R> records,
			final long totalCount, final Map<String, Object> refs) {

		this.records = records;
		this.totalCount = totalCount;
		this.refs = refs;
	}


	/**
	 * Get fetched persistent resource records.
	 *
	 * @return The records. Cannot be {@code null} but can be empty.
	 */
	@Property
	public List<R> getRecords() {

		return this.records;
	}

	/**
	 * Get total count of persistent resource records if the fetch request range
	 * was limited and the count requested.
	 *
	 * @return Persistent resource records count, or -1 if count was not
	 * requested or the fetch was unlimited (without a range).
	 */
	@Property
	public long getTotalCount() {

		return this.totalCount;
	}

	/**
	 * Get fetched referred persistent resource records.
	 *
	 * @return Referred persistent resource records by string representations of
	 * their corresponding references, or {@code null} if no referred resource
	 * records fetch was requested.
	 */
	@Property
	public Map<String, Object> getRefs() {

		return this.refs;
	}
}
