package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Persistent resource. The annotated class must be public, concrete and have a
 * no arguments public constructor.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PersistentResource {

	/**
	 * Global persistent resource access restrictions. Unless explicitly
	 * restricted, all types of access are allowed.
	 *
	 * @return Access restrictions.
	 */
	AccessRestriction[] accessRestrictions() default {};

	/**
	 * Name of the persistent collection (such as a table name for an RDBMS)
	 * used to store the persistent resource records. If not specified, the
	 * collection name is assumed to be the same as the persistent resource
	 * class simple name.
	 *
	 * @return The persistent collection name.
	 */
	String persistentCollection() default "";
}
