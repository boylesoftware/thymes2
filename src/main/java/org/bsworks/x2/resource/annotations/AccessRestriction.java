package org.bsworks.x2.resource.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.ResourcePropertyAccess;


/**
 * Restriction of access to a resource property value.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessRestriction {

	/**
	 * Access type.
	 *
	 * @return Access type.
	 */
	ResourcePropertyAccess value();

	/**
	 * List of actor roles that are allowed this type of access. If empty, which
	 * is the default, no one is allowed access. If consists of a single
	 * {@link Special#AUTHED_ONLY} value, any authenticated actor is allowed
	 * access.
	 *
	 * @return List of roles.
	 */
	String[] allowTo() default {};
}
