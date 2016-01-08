package org.bsworks.x2.resource;

import java.util.List;
import java.util.Map;

import org.bsworks.x2.resource.annotations.AggregateProperty;
import org.bsworks.x2.resource.annotations.AggregationFunction;
import org.bsworks.x2.resource.annotations.Property;


/**
 * Result of a persistent resource records fetch. The class is a resource that
 * can be serialized and contains properties "records", "refs" and "totalCount".
 *
 * <p>This is a basic class, which can be extended and associated with
 * persistent resources in order to include super-aggregate properties in
 * addition to "totalCount".
 *
 * @param <R> Persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceFetchResult<R> {

	/**
	 * Name of the total count super-aggregate property.
	 */
	public static final String TOTAL_COUNT_PROP = "totalCount";


	/**
	 * Fetched persistent resource records.
	 */
	@Property
	private List<R> records;

	/**
	 * Fetched referred persistent resource records by string representations of
	 * their corresponding references.
	 */
	@Property
	private Map<String, Object> refs;

	/**
	 * Total count of persistent resource records matched the fetch filter.
	 */
	@AggregateProperty(collection="records", func=AggregationFunction.COUNT)
	private Long totalCount;


	/**
	 * Get fetched persistent resource records.
	 *
	 * @return The records, or {@code null} if records are not included in the
	 * fetch (super-aggregates only fetch).
	 */
	public List<R> getRecords() {

		return this.records;
	}

	/**
	 * Set fetched persistent resource records.
	 *
	 * @param records The records.
	 */
	public void setRecords(final List<R> records) {

		this.records = records;
	}

	/**
	 * Get fetched referred persistent resource records.
	 *
	 * @return Referred persistent resource records by string representations of
	 * their corresponding references, or {@code null} if no referred resource
	 * records fetch was requested.
	 */
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

	/**
	 * Get count of persistent resource records matched the fetch filter.
	 *
	 * @return Persistent resource records count, or {@code null} if count was
	 * not requested.
	 */
	public Long getTotalCount() {

		return this.totalCount;
	}

	/**
	 * Set count of persistent resource records matched the fetch filter.
	 *
	 * @param totalCount Persistent resource records count.
	 */
	public void setTotalCount(final Long totalCount) {

		this.totalCount = totalCount;
	}
}
