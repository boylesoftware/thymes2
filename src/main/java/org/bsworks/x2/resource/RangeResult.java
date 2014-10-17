package org.bsworks.x2.resource;


/**
 * Object that receives requested persistent resource records collection range
 * meta-data.
 *
 * @author Lev Himmelfarb
 */
public class RangeResult {

	/**
	 * Total count of records if range was unlimited.
	 */
	private long totalCount;


	/**
	 * Get total count of records if range of unlimited.
	 *
	 * @return The records count.
	 */
	public long getTotalCount() {

		return this.totalCount;
	}

	/**
	 * Set total count of records if range of unlimited.
	 *
	 * @param totalCount The records count.
	 */
	public void setTotalCount(final long totalCount) {

		this.totalCount = totalCount;
	}
}
