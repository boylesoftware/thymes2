package org.bsworks.x2.resource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	 * Get new instance of the transient resource used as the persistent
	 * resource fetch result.
	 *
	 * @param <C> Fetch result type.
	 * @param records Fetched persistent resource records, or {@code null} if
	 * records are not included in the fetch (super-aggregates only fetch).
	 * @param refs Fetched referred persistent resource records by string
	 * representations of their corresponding references, or {@code null} if no
	 * referred resource records fetch was requested.
	 *
	 * @return New fetch result object.
	 */
	<C extends PersistentResourceFetchResult<R>> C newFetchResult(
			List<R> records, Map<String, Object> refs);

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

	/**
	 * Get handlers for all aggregate properties.
	 *
	 * @return Unmodifiable collection of property handlers.
	 */
	Collection<? extends AggregatePropertyHandler> getAggregateProperties();
}
