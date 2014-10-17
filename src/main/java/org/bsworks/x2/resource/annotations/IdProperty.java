package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.IdHandling;


/**
 * Persistent record id property. Every persistent resource must have an id
 * property. Also, nested objects used in collections that are stored in their
 * own persistent collections must have an id property (not mandatory for maps,
 * as maps have keys).
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdProperty {

	/**
	 * Id handling.
	 *
	 * @return Id handling.
	 */
	IdHandling handling();

	/**
	 * Name of the persistent field (such as a table column name for an RDBMS)
	 * used to store the id value. The field belongs to the persistent
	 * collection associated with the object that contains the property. If not
	 * specified, the persistent field name is assumed to be the same as the
	 * property name.
	 *
	 * @return The id persistent field name.
	 */
	String persistentField() default "";
}
