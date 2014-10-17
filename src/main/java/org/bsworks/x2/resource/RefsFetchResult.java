package org.bsworks.x2.resource;

import java.util.Map;


/**
 * Object that receives the result of referred persistent resource records
 * fetch.
 *
 * @author Lev Himmelfarb
 */
public class RefsFetchResult {

	/**
	 * Resolved persistent resource records by references.
	 */
	private Map<String, Object> refs;


	/**
	 * Get resolved persistent resource records.
	 *
	 * @return Persistent resource records by string representation of the
	 * corresponding references.
	 */
	public Map<String, Object> getRefs() {

		return this.refs;
	}

	/**
	 * Set resolved persistent resource records.
	 *
	 * @param refs Persistent resource records by string representation of the
	 * corresponding references.
	 */
	public void setRefs(final Map<String, Object> refs) {

		this.refs = refs;
	}
}
