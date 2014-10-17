package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Polymorphic object property value type descriptor.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValueType {

	/**
	 * Type name. If not specified, the concrete class simple name is used (see
	 * {@link Class#getSimpleName()}.
	 *
	 * @return Type name.
	 */
	String name() default "";

	/**
	 * The concrete class.
	 *
	 * @return The concrete class.
	 */
	Class<?> concreteClass();

	/**
	 * Name of the persistent collection (such as a table name for an RDBMS)
	 * used to store objects of this type. If not specified, the collection
	 * specified by the property's {@link Persistence#collection()} attribute is
	 * used.
	 *
	 * @return Persistent collection name.
	 */
	String persistentCollection() default "";

	/**
	 * Name of the persistent field in the persistent collection specified by
	 * the {@link #persistentCollection()} attribute used to store the parent
	 * record id to link the concrete object properties back to the parent
	 * record. Must be specified if {@link #persistentCollection()} is
	 * specified.
	 *
	 * @return The parent id persistent field name.
	 */
	String parentIdPersistentField() default "";

	/**
	 * Prefix to add to the persistent field names associated with the object
	 * properties.
	 *
	 * @return Persistent fields name prefix.
	 */
	String persistentFieldsPrefix() default "";
}
