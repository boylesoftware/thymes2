package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.SegmentOrderSpecElement;
import org.bsworks.x2.resource.SortDirection;


/**
 * Segment order specification element implementation.
 *
 * @author Lev Himmelfarb
 */
class SegmentOrderSpecElementImpl
	implements SegmentOrderSpecElement {

	/**
	 * Sort direction.
	 */
	private final SortDirection dir;

	/**
	 * The segment split point filter.
	 */
	private final FilterSpec<?> segmentFilter;


	/**
	 * Create new element.
	 *
	 * @param dir Sort direction.
	 * @param segmentFilter Segment split point filter.
	 */
	SegmentOrderSpecElementImpl(final SortDirection dir,
			final FilterSpec<?> segmentFilter) {

		this.dir = dir;
		this.segmentFilter = segmentFilter;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SortDirection getSortDirection() {

		return this.dir;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpec<?> getSegmentFilter() {

		return this.segmentFilter;
	}
}
