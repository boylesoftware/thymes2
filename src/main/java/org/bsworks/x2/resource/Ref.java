package org.bsworks.x2.resource;


/**
 * Reference to a persistent application resource record.
 *
 * @param <R> Referred resource type.
 *
 * @author Lev Himmelfarb
 */
public interface Ref<R>
	extends Comparable<Ref<R>> {

	/**
	 * Get referred resource class.
	 *
	 * @return The referred resource class.
	 */
	Class<R> getResourceClass();

	/**
	 * Get referred resource record id.
	 *
	 * @return The referred resource record id.
	 */
	Object getId();

	/**
	 * Get string representation of the reference. The string representation is
	 * used in the serialized resource record data to represent the reference.
	 *
	 * @return The reference string.
	 */
	@Override
	String toString();
}
