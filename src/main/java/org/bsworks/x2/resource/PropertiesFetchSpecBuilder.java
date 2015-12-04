package org.bsworks.x2.resource;


/**
 * Builder of a specification of what persistent resource properties to include
 * in a fetch.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PropertiesFetchSpecBuilder<R>
	extends PropertiesFetchSpec<R> {

	/**
	 * Include specified property.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The path may contain several
	 * intermediate references in it, in which case the references must be
	 * fetched using the corresponding {@link RefsFetchSpec} object in order for
	 * the property to be included. The property at the end of the path must not
	 * be a nested object property. All intermediate properties in the path
	 * automatically become included as well.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> include(String propPath);

	/**
	 * Switch the specification to "include by default" mode. Unless called,
	 * the specification initially is in "exclude by default" mode, in which all
	 * properties are assumed to be excluded unless explicitly included by
	 * calling the {@link #include(String)} method. In "include by default"
	 * mode, all properties are assumed to be included unless explicitly
	 * excluded by calling the {@link #exclude(String)} method or marked as not
	 * to be fetched by default
	 * ({@link ResourcePropertyHandler#isFetchedByDefault()} returns
	 * {@code false} for the property). A property that is not fetched by
	 * default can be added to the fetch with the {@link #include(String)}
	 * method.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws IllegalStateException If {@link #includeByDefault()} has been
	 * already called.
	 */
	PropertiesFetchSpecBuilder<R> includeByDefault();

	/**
	 * Exclude specified property.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The path may contain several
	 * intermediate references in it, in which case the exclusion is effective
	 * only if the references are fetched using the corresponding
	 * {@link RefsFetchSpec} object. If the property at the end of the path is a
	 * nested object property or a reference, the exclusion applies to all of
	 * the object's or resource's properties.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 * @throws IllegalStateException If {@link #includeByDefault()} has not been
	 * called.
	 */
	PropertiesFetchSpecBuilder<R> exclude(String propPath);

	/**
	 * Add reference property to the fetch.
	 *
	 * @param propPath Property path with nested properties separated with dots.
	 * The path starts at the class returned by
	 * {@link #getPersistentResourceClass()}. The path may contain several
	 * references in it, in which case the intermediate references are also
	 * added to the fetch. The last property in the path must be a reference.
	 *
	 * @return This object (for chaining).
	 *
	 * @throws InvalidSpecificationException If the specified property path is
	 * invalid.
	 */
	PropertiesFetchSpecBuilder<R> fetch(String propPath);
}
