package org.bsworks.x2.resource;


/**
 * Builder of a specification of sorting order for a persistent resource records
 * fetch.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface OrderSpecBuilder<R>
	extends OrderSpec<R> {

	/**
	 * Add ordering by the specified property's plain value. This is equivalent
	 * to calling {@code add(dir, propPath, PropertyValueFunction.PLAIN)}.
	 *
	 * @param dir Sort direction.
	 * @param propPath Property path.
	 *
	 * @return This order specification object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	OrderSpecBuilder<R> add(SortDirection dir, String propPath);

	/**
	 * Add ordering by the specified property.
	 *
	 * @param dir Sort direction.
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The path may not contain any
	 * collection or map properties. The property at the end of the path may not
	 * be a nested object. It also may not be a reference, unless it is suffixed
	 * with "/id", in which case the referred record id is used for ordering.
	 * The path may contain several intermediate references.
	 * @param func Property value transformation function.
	 * @param funcParams Parameters for the value transformation function.
	 *
	 * @return This order specification object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	OrderSpecBuilder<R> add(SortDirection dir, String propPath,
			PropertyValueFunction func, Object... funcParams);

	/**
	 * Add segmentation by the specified condition. Adding a segment splits the
	 * result list in two: all records, for which the condition is false appear
	 * first, followed by all records, for which the condition is true. This is
	 * achieved by including ordering by the result of the condition evaluation.
	 * Multiple segments can be added to create sub-segmentation.
	 *
	 * <p>Note, that segmentation is always performed before sorting specified
	 * by the {@link #add(SortDirection, String)} method calls.
	 *
	 * @param split Filter specification that expresses the segment split
	 * condition. Records included by the filter will appear after the records
	 * excluded by the filter.
	 *
	 * @return This order specification object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	OrderSpecBuilder<R> addSegment(FilterSpec<R> split);
}
