package org.bsworks.x2.app;

import org.bsworks.x2.resource.annotations.Property;


/**
 * Wrapper resource for an existing persistent resource record.
 *
 * @author Lev Himmelfarb
 */
public class ExistingPersistentResourceRecord {

	/**
	 * The existing record.
	 */
	private final Object existingRecord;


	/**
	 * Create new wrapper resource.
	 *
	 * @param existingRecord The existing record.
	 */
	public ExistingPersistentResourceRecord(final Object existingRecord) {

		this.existingRecord = existingRecord;
	}


	/**
	 * Get the wrapped record.
	 *
	 * @return The wrapped record.
	 */
	@Property
	public Object getExistingRecord() {

		return this.existingRecord;
	}
}
