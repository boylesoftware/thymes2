package org.bsworks.x2.resource.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bsworks.x2.resource.annotations.AccessRestriction;


/**
 * Access restrictions.
 *
 * @author Lev Himmelfarb
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@interface AccessRestrictions {

	/**
	 * The restrictions.
	 *
	 * @return The restrictions.
	 */
	AccessRestriction[] value();
}
