package org.bsworks.x2.resource;


/**
 * Specification of a persistent resource records collection range.
 *
 * @author Lev Himmelfarb
 */
public class RangeSpec {

	/**
	 * First record.
	 */
	private final int firstRecord;

	/**
	 * Maximum number of records.
	 */
	private final int maxRecords;


	/**
	 * Create new range specification.
	 *
	 * @param firstRecord Index of the first record in the range, starting from
	 * zero.
	 * @param maxRecords Maximum number of records in the range.
	 */
	public RangeSpec(final int firstRecord, final int maxRecords) {

		if (firstRecord < 0)
			throw new InvalidSpecificationException(
					"First record index must not be negative.");
		if (maxRecords <= 0)
			throw new InvalidSpecificationException(
					"Maximum number of records must be greater than zero.");

		this.firstRecord = firstRecord;
		this.maxRecords = maxRecords;
	}


	/**
	 * Get index of the first record in the range.
	 *
	 * @return First record index, starting from zero.
	 */
	public int getFirstRecord() {

		return this.firstRecord;
	}

	/**
	 * Get maximum number of records in the range.
	 *
	 * @return Maximum number of records.
	 */
	public int getMaxRecords() {

		return this.maxRecords;
	}
}
