package org.bsworks.x2.resource;


/**
 * Result set segmentation order specification element.
 *
 * @author Lev Himmelfarb
 */
public interface SegmentOrderSpecElement
	extends OrderSpecElement {

	/**
	 * Get filter used for the segmentation. In natural order (ascending),
	 * records that are <em>not</em> selected by the filter appear first in the
	 * result set followed by the records selected by the filter.
	 *
	 * @return The filter specification.
	 */
	FilterSpec<?> getSegmentFilter();
}
