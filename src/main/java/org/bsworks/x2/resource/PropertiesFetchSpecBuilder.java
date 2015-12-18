package org.bsworks.x2.resource;


/**
 * Builder for a {@link PropertiesFetchSpec}. The builder extends the
 * {@link PropertiesFetchSpec} and can be used as such.
 *
 * <p><em>Note, that builder instances must not be assumed to be
 * thread-safe!</em>
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PropertiesFetchSpecBuilder<R>
	extends PropertiesFetchSpec<R> {

	/**
	 * Switch the specification to "include by default" mode. Unless called,
	 * any new specification is initially in "exclude by default" mode, that is
	 * all properties are assumed to be excluded unless explicitly included by
	 * calling the {@link #include(String)} or {@link #fetch(String)} method. In
	 * "include by default" mode, all properties that are specified on the
	 * resource as fetched by default
	 * (see {@link ResourcePropertyHandler#isFetchedByDefault()}) are assumed to
	 * be included unless explicitly excluded by calling the
	 * {@link #exclude(String)} or {@link #excludeProperties(String)} method. In
	 * the "include by default" mode, a property that is not fetched by default
	 * can be added to the fetch with the {@link #include(String)} method.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws IllegalStateException If {@link #includeByDefault()} has been
	 * already called (to prevent programming errors).
	 */
	PropertiesFetchSpecBuilder<R> includeByDefault();

	/**
	 * Include specified property. Calling this method adds a rule to the
	 * specification that requests to unconditionally include the specified
	 * property and all parent properties in its path.
	 *
	 * <p>The specified property path may contain several intermediate
	 * reference properties in it, in which case the referred resources are
	 * automatically added to the fetch as if via the {@link #fetch(String)}
	 * method. Note, that if the property at the end of the path is a reference,
	 * the referred resource is <em>not</em> added to the fetch. To request
	 * fetching a referred resource record without specifying any inclusion
	 * rules for its properties see {@link #fetch(String)} method.
	 *
	 * <p>The property at the end of the path must not be a nested object
	 * property. If it is, an {@link InvalidSpecificationException} is thrown.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> include(String propPath);

	/**
	 * Add resource referred by the specified reference property to the fetch.
	 * The fetched referred resource records are returned in the fetch result
	 * alongside the main resource records. The properties in the fetched
	 * referred resource records are included or excluded according to the
	 * "include/exclude by default" mode and the explicit include and exclude
	 * rules in this specification.
	 *
	 * <p>The specified reference property path may contain several intermediate
	 * reference properties in it, in which case the intermediate references are
	 * also added to the fetch. And the last property in the path must be a
	 * reference, or an {@link InvalidSpecificationException} is thrown.
	 *
	 * <p>All intermediate properties in the path become included as if via the
	 * {@link #include(String)} method.
	 *
	 * @param propPath Reference property path with nested properties separated
	 * with dots. The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> fetch(String propPath);

	/**
	 * Include an aggregate property and attach a filter for the aggregated
	 * resource records that will participate in the aggregate property value
	 * calculation.
	 *
	 * <p>The specified filter is based at the persistent resource, but all
	 * properties in all of its conditions must be nested properties in the
	 * aggregated collection. An {@link InvalidSpecificationException} is thrown
	 * if the filter uses any property that is not nested in the aggregated
	 * collection. Note, that the specified filter is stored using its
	 * reference, so if the filter specification is not immutable, care should
	 * be taken not to modify it later on and add incompatible conditions.
	 *
	 * <p>In all other aspects, similar to the {@link #include(String)} method
	 * adding the specification property path to the include rules and any
	 * intermediate references to the fetch.
	 *
	 * @param propPath Aggregate property path with nested properties separated
	 * with dots. The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 * @param filter Filter for the aggregated resource records.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid or filter uses properties that are not nested in the aggregated
	 * collection.
	 */
	PropertiesFetchSpecBuilder<R> includeFilteredAggregate(String propPath,
			FilterSpec<R> filter);

	/**
	 * Exclude specified property. Calling this method adds a rule to the
	 * specification that requests to exclude the specified property from the
	 * fetch and, if the property is a reference or a nested object, exclude all
	 * of its nested properties, unless explicitly included using
	 * {@link #include(String)} or {@link #fetch(String)} method. The call does
	 * not create any rules for the parent properties in the specified path.
	 *
	 * <p>The specified property path may contain several intermediate reference
	 * properties in it. Note, that in such case the exclusion is effective only
	 * if the references are fetched via explicit calls to the
	 * {@link #include(String)} and/or {@link #fetch(String)} methods.
	 * Otherwise, the exclusion is ignored because it asks to exclude a
	 * property that is not fetched anyway.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> exclude(String propPath);

	/**
	 * Exclude nested properties of the specified reference property, but do not
	 * create any rules for the reference property itself. This is similar to
	 * the {@link #exclude(String)} method, but does not involve the specified
	 * property itself.
	 *
	 * <p>The method may be useful in the "include by default" mode and allows
	 * including only specific properties of fetched referred resource records.
	 *
	 * <p>The specified property path must be a reference, or an
	 * {@link InvalidSpecificationException} is thrown.
	 *
	 * @param propPath Reference property path with nested properties separated
	 * with dots. The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> excludeProperties(String propPath);
}
