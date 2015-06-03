package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Resource property. The annotation is used with three types of resource
 * properties: simple value, such as a string or a number, nested object, or
 * reference to another persistent resource. Each can be a collection or a map.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Property {

	/**
	 * Property access restrictions. Unless explicitly restricted, all types of
	 * access are allowed.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * If {@code true}, which is the default, the property is fetched by default
	 * when the resource is loaded from the persistent storage. If
	 * {@code false}, the property is fetched only if explicitly requested by
	 * the properties fetch specification.
	 *
	 * <p>Often collection and nested object property values are not stored
	 * together with the resource record, so fetching them may require
	 * additional work in the persistent storage (for example, a join if the
	 * storage is an RDBMS). Unless such properties are needed, to improve
	 * performance it often makes sense to set this attribute to {@code false}.
	 *
	 * <p>Value of {@code false} may not be specified for a primitive property.
	 *
	 * @return {@code true} to fetch property value by default, {@code false} to
	 * require explicit fetch request.
	 */
	boolean fetchedByDefault() default true;

	/**
	 * If {@code true}, which is the default, the property value is set to
	 * {@code null} when an existing resource is being updated and in the
	 * incoming update data the property is {@code null}. If {@code false} and
	 * in the incoming update data the property is {@code null}, the property in
	 * the existing resource is left unchanged.
	 *
	 * @return {@code true} to set to {@code null}, {@code false} to leave
	 * unchanged.
	 */
	boolean updateIfNull() default true;

	/**
	 * Property persistence attributes. By default, the property is transient
	 * and is not stored in the persistent storage.
	 *
	 * @return Persistence attributes.
	 */
	Persistence persistence() default @Persistence(persistent=false);

	/**
	 * The persistent resource that owns the nested object. This attribute
	 * allows using nested object resources as properties in multiple persistent
	 * resources while having only one of the "own" it. Owning such nested
	 * object resource affects the record versioning: if a persistent resource
	 * does not own the nested object, when the persistent resource record
	 * version is calculated the version of the owning persistent resource
	 * collection is taken into account. If this attribute is not specified, the
	 * nested object resource is assumed to be owned by the persistent resource,
	 * in which it is used as a property.
	 *
	 * <p>If property is owned by a different persistent resource, it is never
	 * included in this persistent resource updates, creates and deletes. It is
	 * included in fetches by default, however, so, often it makes sense to set
	 * {@link #fetchedByDefault()} to {@code false} as well.
	 *
	 * <p>This attribute is allowed only on nested object properties.
	 *
	 * @return Owner persistent resource class.
	 */
	Class<?> ownedBy() default PersistentResource.class;

	/**
	 * Possible concrete value types for a polymorphic nested object property.
	 *
	 * @return The value type descriptors.
	 */
	ValueType[] valueTypes() default {};
}
