package org.bsworks.x2.resource;


/**
 * Central manager for the application resources.
 *
 * @author Lev Himmelfarb
 */
public interface Resources {

	/**
	 * Name of web-application context initialization parameter used to list
	 * Java packages that contain application persistent resources.
	 */
	static final String PERSISTENT_RESOURCE_PACKAGES_INITPARAM =
		"x2.app.persistentResources.packages";

	/**
	 * Name of web-application context initialization parameter used to specify
	 * pattern for JAR file names to scan for application persistent resources.
	 */
	static final String PERSISTENT_RESOURCE_JARS_PATTERN_INITPARAM =
		"x2.app.persistentResources.jarsPattern";


	/**
	 * Get application resource handler. This includes any resource handler that
	 * may be returned by {@link #getPersistentResourceHandler(Class)}, plus any
	 * transient resource.
	 *
	 * @param rsrcClass Application resource class.
	 *
	 * @return Application resource handler.
	 *
	 * @throws IllegalArgumentException If something is wrong with the specified
	 * class and it does represent a valid application resource.
	 */
	<R> ResourceHandler<R> getResourceHandler(Class<R> rsrcClass);

	/**
	 * Tell if the specified class is a valid persistent resource class known to
	 * the application.
	 *
	 * @param prsrcClass Class to test.
	 *
	 * @return {@code true} if specified class is a persistent resource class.
	 */
	boolean isPersistentResource(Class<?> prsrcClass);

	/**
	 * Get persistent application resource handler.
	 *
	 * @param prsrcClass Persistent application resource class.
	 *
	 * @return Persistent application resource handler.
	 *
	 * @throws IllegalArgumentException If the specified class does not
	 * represent a persistent application resource. Use
	 * {@link #isPersistentResource(Class)} to test if a class is a valid
	 * persistent application resource class.
	 */
	<R> PersistentResourceHandler<R> getPersistentResourceHandler(
			Class<R> prsrcClass);

	/**
	 * Get empty referred resources fetch specification object.
	 *
	 * @param prsrcClass Persistent application resource class, whose reference
	 * properties need to be fetched.
	 *
	 * @return Referred resources fetch specification object.
	 */
	<R> RefsFetchSpec<R> getRefsFetchSpec(Class<R> prsrcClass);

	/**
	 * Get empty properties fetch specification object.
	 *
	 * @param prsrcClass Persistent application resource class, whose properties
	 * need to be included or excluded in a fetch.
	 *
	 * @return Properties fetch specification object initially in "exclude by
	 * default" mode.
	 */
	<R> PropertiesFetchSpec<R> getPropertiesFetchSpec(Class<R> prsrcClass);

	/**
	 * Get empty filter specification object.
	 *
	 * @param prsrcClass Persistent application resource class at the top of the
	 * fetch, for which the filter is intended.
	 *
	 * @return Filter specification. The returned filter specification is always
	 * a conjunction (logical "AND").
	 */
	<R> FilterSpec<R> getFilterSpec(Class<R> prsrcClass);

	/**
	 * Get empty order specification object.
	 *
	 * @param prsrcClass Persistent application resource class at the top of the
	 * fetch, for which the order specification is intended.
	 *
	 * @return Order specification object.
	 */
	<R> OrderSpec<R> getOrderSpec(Class<R> prsrcClass);

	/**
	 * Parse reference string.
	 *
	 * @param refStr Reference string as returned by the {@link Ref#toString()}.
	 *
	 * @return Parsed reference.
	 *
	 * @throws IllegalArgumentException If the specified reference is invalid
	 * (syntactically or refers to an unknown resource type).
	 */
	Ref<?> parseRef(String refStr);

	/**
	 * Create reference for the specified persistent resource record.
	 *
	 * @param prsrcClass Persistent application resource class.
	 * @param rec The record. Must be an instance of the specified resource
	 * class or its subclass.
	 *
	 * @return Record reference.
	 *
	 * @throws NullPointerException If specified record is {@code null}.
	 * @throws IllegalArgumentException If specified class is not an
	 * application persistent resource, or the specified object is not its
	 * record.
	 */
	<R> Ref<R> createRef(Class<R> prsrcClass, Object rec);
}
