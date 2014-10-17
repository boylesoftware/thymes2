package org.bsworks.x2.resource;

import java.util.Collection;

import org.bsworks.x2.Actor;


/**
 * Persistent application resource handler.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PersistentResourceHandler<R>
	extends ResourceHandler<R> {

	/**
	 * Get name of the persistent collection (e.g. table for an RDBMS)
	 * associated with the resource.
	 *
	 * @return Persistent collection name.
	 */
	String getPersistentCollectionName();

	/**
	 * Tell if the specified actor has specified access to the persistent
	 * resource.
	 *
	 * @param access The access type.
	 * @param actor The actor, or {@code null} if unauthenticated.
	 *
	 * @return {@code true} if access is allowed.
	 */
	boolean isAllowed(ResourcePropertyAccess access, Actor actor);

	/**
	 * Get handler for the resource record meta-property.
	 *
	 * @param type Meta-property type.
	 *
	 * @return Meta-property handler, or {@code null} if undefined for the
	 * resource.
	 */
	MetaPropertyHandler getMetaProperty(MetaPropertyType type);

	/**
	 * Get handlers for all dependent persistent resource reference properties.
	 *
	 * @return Unmodifiable collection of property handlers.
	 */
	Collection<? extends DependentRefPropertyHandler>
	getDependentRefProperties();
}
