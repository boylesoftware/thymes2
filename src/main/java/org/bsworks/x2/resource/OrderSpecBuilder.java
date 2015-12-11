package org.bsworks.x2.resource;


/**
 * Builder for an {@link OrderSpec}. The builder extends the {@link OrderSpec}
 * and can be used as such.
 *
 * <p><em>Note, that builder instances must not be assumed to be
 * thread-safe!</em>
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
	 * Add segmentation by the specified condition. Adding a segment sorts the
	 * records around the filter split point: in ascending order, records that
	 * are not included by the filter appear before the records that are
	 * included.
	 *
	 * @param dir Sort direction.
	 * @param split Filter specification that defines the segment split
	 * condition.
	 *
	 * @return This order specification object (for chaining).
	 */
	OrderSpecBuilder<R> addSegment(SortDirection dir, FilterSpec<R> split);
}
