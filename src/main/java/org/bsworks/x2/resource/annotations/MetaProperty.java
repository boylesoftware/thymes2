package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.MetaPropertyType;


/**
 * Persistent resource record meta-property.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MetaProperty {

	/**
	 * Meta-property type.
	 *
	 * @return Meta-property type.
	 */
	MetaPropertyType type();

	/**
	 * Name of the persistent field (such as a table column name for an RDBMS)
	 * used to store the meta-property value. The field belongs to the
	 * persistent collection associated with the persistent resource. If not
	 * specified, the persistent field name is assumed to be the same as the
	 * property name.
	 *
	 * @return The meta-property persistent field name.
	 */
	String persistentField() default "";
}
