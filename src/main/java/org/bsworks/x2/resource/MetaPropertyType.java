package org.bsworks.x2.resource;

import org.bsworks.x2.Actor;


/**
 * Persistent resource meta-property type.
 *
 * @author Lev Himmelfarb
 */
public enum MetaPropertyType {

	/**
	 * Resource record version. The value of the property may be {@link Integer}
	 * of {@link Long}.
	 */
	VERSION,

	/**
	 * Resource record creation timestamp. The value of the property is
	 * {@link java.util.Date}.
	 */
	CREATION_TIMESTAMP,

	/**
	 * Name of the actor that created the resource record (value returned by
	 * {@link Actor#getActorName()}). The value of the property is
	 * {@link String}.
	 */
	CREATION_ACTOR,

	/**
	 * Last resource record modification timestamp. The value of the property is
	 * {@link java.util.Date}.
	 */
	MODIFICATION_TIMESTAMP,

	/**
	 * Name of the actor that last time modified the resource record (value
	 * returned by {@link Actor#getActorName()}). The value of the property is
	 * {@link String}.
	 */
	MODIFICATION_ACTOR
}
