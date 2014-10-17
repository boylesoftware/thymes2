package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Resource property persistence descriptor.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Persistence {

	/**
	 * Tells if the property is persistent, which is the default. If
	 * {@code false}, the property is transient and is not stored in the
	 * persistent storage. There is practically no reason to explicitly specify
	 * this attribute, as the default value of the
	 * {@link Property#persistence()} attribute makes the property transient.
	 *
	 * @return {@code true} for a persistent property.
	 */
	boolean persistent() default true;

	/**
	 * Tells if the property value can be {@code null} for a single-valued
	 * property or empty for a collection or a map property. The default is
	 * {@code false}.
	 *
	 * <p>The attribute may be used by the persistence service implementation to
	 * decide how to load the property value. For example, if the persistence
	 * service is backed by an RDBMS, the value of this attribute may determine
	 * whether an inner join (if not optional) or an outer join (if optional) is
	 * used.
	 *
	 * @return {@code true} if optional.
	 */
	boolean optional() default false;

	/**
	 * Name of the persistent field (such as a table column name in case of an
	 * RDBMS) used to store the property value. If {@link #collection()}
	 * attribute is specified, the field belongs to the specified collection. If
	 * it is not specified, the field belongs to the collection used to store
	 * the object that contains the property.
	 *
	 * <p>In case the property is a nested object, the attribute value is used
	 * as a prefix for any persistent field name for any property immediately
	 * contained in the nested object. This is particularly useful for embedded
	 * nested objects ({@link #collection()} is not specified).
	 *
	 * <p>If not specified and the property is not a nested object, the
	 * persistent field name is assumed to be the same as the property name.
	 *
	 * @return The property value persistent field name.
	 */
	String field() default "";

	/**
	 * Name of the persistent collection (such as a table name in case of an
	 * RDBMS) used to store the property values. If not specified, the property
	 * values are stored in the same persistent collection as the rest of the
	 * containing object.
	 *
	 * <p>For a collection or a map property (a.k.a. multi-valued property),
	 * this attribute must be specified unless the used persistence service
	 * supports embedded collections.
	 *
	 * @return The property values persistent collection name.
	 */
	String collection() default "";

	/**
	 * Name of the persistent field in the persistent collection specified by
	 * the {@link #collection()} attribute used to store the containing object
	 * id to link the property values back to their parent objects. Must be
	 * specified if {@link #collection()} is specified.
	 *
	 * @return The parent id persistent field name.
	 */
	String parentIdField() default "";

	/**
	 * For a map property, name of the persistent field in the persistent
	 * collection specified by the {@link #collection()} attribute used to store
	 * the map key value. Must be specified if {@link #collection()} is
	 * specified and the property is a map.
	 *
	 * @return The map key persistent field name.
	 */
	String keyField() default "";
}
