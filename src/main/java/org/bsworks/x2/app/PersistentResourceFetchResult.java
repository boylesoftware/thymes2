package org.bsworks.x2.app;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Represents result data of a persistent resource records fetch.
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceFetchResult {

	/**
	 * Persistent resource records.
	 */
	private List<?> records;

	/**
	 * Total count of persistent resource records if the fetch request range was
	 * unlimited.
	 */
	private long totalCount;

	/**
	 * Fetched referred persistent resource records by string representations of
	 * their corresponding references.
	 */
	private Map<String, Object> refs;


	/**
	 * Create uninitialized result object.
	 */
	public PersistentResourceFetchResult() {}

	/**
	 * Create and initialize new result object.
	 *
	 * @param records Fetched persistent resource records.
	 * @param totalCount Total count of persistent resource records if the fetch
	 * request range was unlimited.
	 * @param refs Fetched referred persistent resource records by string
	 * representations of their corresponding references.
	 */
	public PersistentResourceFetchResult(final List<?> records,
			final long totalCount, final Map<String, Object> refs) {

		this.records = records;
		this.totalCount = totalCount;
		this.refs = refs;
	}


	/**
	 * Get fetched persistent resource records.
	 *
	 * @return The records.
	 */
	@Property
	public List<?> getRecords() {

		return this.records;
	}

	/**
	 * Set fetched persistent resource records.
	 *
	 * @param records The records.
	 */
	public void setRecords(final List<?> records) {

		this.records = records;
	}

	/**
	 * Get total count of persistent resource records if the fetch request range
	 * was unlimited.
	 *
	 * @return Persistent resource records count.
	 */
	@Property
	public long getTotalCount() {

		return this.totalCount;
	}

	/**
	 * Set total count of persistent resource records if the fetch request range
	 * was unlimited.
	 *
	 * @param totalCount Persistent resource records count.
	 */
	public void setTotalCount(final long totalCount) {

		this.totalCount = totalCount;
	}

	/**
	 * Get fetched referred persistent resource records.
	 *
	 * @return Referred persistent resource records by string representations of
	 * their corresponding references.
	 */
	@Property
	public Map<String, Object> getRefs() {

		return this.refs;
	}

	/**
	 * Set fetched referred persistent resource records.
	 *
	 * @param refs Referred persistent resource records by string
	 * representations of their corresponding references.
	 */
	public void setRefs(final Map<String, Object> refs) {

		this.refs = refs;
	}
}
