package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.Ref;


/**
 * Persistent resource reference implementation.
 *
 * @param <R> Referred resource type.
 *
 * @author Lev Himmelfarb
 */
class RefImpl<R>
	implements Ref<R> {

	/**
	 * Persistent resource class.
	 */
	private final Class<R> rsrcClass;

	/**
	 * Resource record id.
	 */
	private final Object recordId;

	/**
	 * Reference string representation.
	 */
	private final String refString;


	/**
	 * Create new reference.
	 *
	 * @param rsrcClass Persistent resource class.
	 * @param recordId Resource record id.
	 * @param refString Reference string representation.
	 */
	RefImpl(final Class<R> rsrcClass, final Object recordId,
			final String refString) {

		this.rsrcClass = rsrcClass;
		this.recordId = recordId;
		this.refString = refString;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<R> getResourceClass() {

		return this.rsrcClass;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object getId() {

		return this.recordId;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public int compareTo(final Ref<R> o) {

		return ((Comparable) this.recordId).compareTo(o.getId());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString() {

		return this.refString;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public int hashCode() {

		return this.refString.hashCode();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean equals(final Object obj) {

		if (obj == this)
			return true;

		if ((obj == null) || !(obj instanceof Ref))
			return false;

		return obj.toString().equals(this.refString);
	}
}
