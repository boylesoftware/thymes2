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
	 * calling the {@link #include(String)} method. In "include by default"
	 * mode, all properties that are specified on the resource as fetched by
	 * default (see {@link ResourcePropertyHandler#isFetchedByDefault()}) are
	 * assumed to be included unless explicitly excluded by calling the
	 * {@link #exclude(String)} method. In the "include by default" mode, a
	 * property that is not fetched by default can be added to the fetch with
	 * the {@link #include(String)}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws IllegalStateException If {@link #includeByDefault()} has been
	 * already called (to prevent programming errors).
	 */
	PropertiesFetchSpecBuilder<R> includeByDefault();

	/**
	 * Include specified property.
	 *
	 * <p>The specified property path may contain several intermediate
	 * reference properties in it, in which case the referred resources are
	 * automatically added to the fetch as if via the {@link #fetch(String)}
	 * method. Note, that if the property at the end of the path is a reference,
	 * the referred resource is <em>not</em> added to the fetch.
	 *
	 * <p>The property at the end of the path must not be a nested object
	 * property. If it is, an {@link InvalidSpecificationException} is thrown.
	 *
	 * <p>All intermediate properties in the path automatically become included
	 * as well.
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
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> fetch(String propPath);

	/**
	 * Exclude specified property.
	 *
	 * <p>Note, that since all properties are initially excluded in the default
	 * "exclude by default" mode, the method makes sense only in the "include by
	 * default" mode. If the specification is in the "exclude by default" mode,
	 * any exclusion rule specified by this method is ignored (unless, of
	 * course, there is an explicit inclusion, in which case the exclusion takes
	 * precedence and the inclusion becomes ineffective).
	 *
	 * <p>If, according to this specification, a property is both included and
	 * excluded, the exclusion takes precedence.
	 *
	 * <p>If the excluded property is a nested object or a reference, all nested
	 * properties are excluded as well.
	 *
	 * <p>The specified property path may contain several intermediate reference
	 * properties in it. Note, that in such case the exclusion is effective only
	 * if the references are fetched via explicit calls to the
	 * {@link #include(String)} and/or {@link #fetch(String)} methods.
	 * Otherwise, the exclusion is ignored because it asks the fetch to exclude
	 * a property that is not fetched anyway.
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
}
