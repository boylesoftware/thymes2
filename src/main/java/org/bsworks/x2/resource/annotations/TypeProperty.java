package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Polymorphic object type property.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeProperty {

	/**
	 * Tells if the type property is stored in the persistent storage. For a
	 * persistent polymorphic nested object property, the type property must be
	 * persistent if any of the concrete types are not stored in their own
	 * persistent collection. If all the concrete types are stored in their own
	 * collections, the type property does not have to be persistent. However,
	 * for certain types of queries, it may be more efficient to stored the type
	 * property, because it allows the query logic not to query each concrete
	 * type persistent collection in order to find out the type of a given
	 * record.
	 *
	 * @return {@code true} if persistent.
	 */
	boolean persistent();

	/**
	 * Name of the persistent field (such as a table column name for an RDBMS)
	 * used to store the type value. The field belongs to the persistent
	 * collection associated with the object that contains the property. If not
	 * specified, the persistent field name is assumed to be the same as the
	 * property name.
	 *
	 * @return The type persistent field name.
	 */
	String persistentField() default "";
}
