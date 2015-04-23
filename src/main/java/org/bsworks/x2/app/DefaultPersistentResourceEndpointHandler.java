package org.bsworks.x2.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeResult;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.RefsFetchResult;
import org.bsworks.x2.resource.RefsFetchSpec;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.services.persistence.LockType;
import org.bsworks.x2.services.persistence.PersistenceTransaction;
import org.bsworks.x2.services.persistence.PersistentResourceFetch;


/**
 * Default implementation of persistent resource endpoint handler.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public class DefaultPersistentResourceEndpointHandler<R>
	implements PersistentResourceEndpointHandler<R> {

	/**
	 * Handled persistent resource class handler.
	 */
	protected final PersistentResourceHandler<R> prsrcHandler;

	/**
	 * Persistent resource class.
	 */
	protected final Class<R> prsrcClass;

	/**
	 * Persistent resource record id property handler.
	 */
	protected final IdPropertyHandler idPropHandler;


	/**
	 * Create new handler.
	 *
	 * @param prsrcHandler Handled persistent resource class handler.
	 */
	public DefaultPersistentResourceEndpointHandler(
			final PersistentResourceHandler<R> prsrcHandler) {

		this.prsrcHandler = prsrcHandler;
		this.prsrcClass = this.prsrcHandler.getResourceClass();
		this.idPropHandler = this.prsrcHandler.getIdProperty();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentResourceHandler<R> getPersistentResourceHandler() {

		return this.prsrcHandler;
	}

	/**
	 * Default implementation returns instances of
	 * {@link DefaultGetPersistentResourceEndpointCallHandler},
	 * {@link DefaultPostPersistentResourceEndpointCallHandler},
	 * {@link DefaultPutPersistentResourceEndpointCallHandler} and
	 * {@link DefaultDeletePersistentResourceEndpointCallHandler} for the
	 * corresponding HTTP request methods.
	 */
	@Override
	public EndpointCallHandler<?> getCallHandler(
			final HttpMethod requestMethod) {

		switch (requestMethod) {
		case GET:
			return new DefaultGetPersistentResourceEndpointCallHandler<>(
					this);
		case POST:
			return new DefaultPostPersistentResourceEndpointCallHandler<>(
					this);
		case PUT:
			return new DefaultPutPersistentResourceEndpointCallHandler<>(
					this);
		case DELETE:
			return new DefaultDeletePersistentResourceEndpointCallHandler<>(
					this);
		default: // cannot happen
			throw new RuntimeException("Unknown HTTP request method.");
		}
	}

	/**
	 * Default implementation uses
	 * {@link PersistentResourceHandler#isAllowed(ResourcePropertyAccess, Actor)}
	 * method and has the following logic for each of the HTTP request methods:
	 *
	 * <table>
	 * <caption>Conditions by Methods</caption>
	 * <tr><th>Method<th>Condition to Allow
	 * <tr><td>GET<td>Actor has {@link ResourcePropertyAccess#SEE} and
	 * {@link ResourcePropertyAccess#LOAD} access to the resource.
	 * <tr><td>POST<td>Actor is not {@code null} and has
	 * {@link ResourcePropertyAccess#SUBMIT} and
	 * {@link ResourcePropertyAccess#PERSIST} access to the resource.
	 * <tr><td>PUT<td>Actor is not {@code null} and has
	 * {@link ResourcePropertyAccess#SUBMIT} and
	 * {@link ResourcePropertyAccess#UPDATE} access to the resource.
	 * <tr><td>DELETE<td>Actor is not {@code null} and has
	 * {@link ResourcePropertyAccess#DELETE} access to the resource.
	 * </table>
	 */
	@Override
	public boolean isAllowed(HttpMethod requestMethod, String requestURI,
			List<String> uriParams, Actor actor) {

		switch (requestMethod) {
		case GET:
			return (
				this.prsrcHandler.isAllowed(ResourcePropertyAccess.SEE, actor)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.LOAD,
						actor)
			);
		case POST:
			return (
				(actor != null)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.SUBMIT,
						actor)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.PERSIST,
						actor)
			);
		case PUT:
			return (
				(actor != null)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.SUBMIT,
						actor)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.UPDATE,
						actor)
			);
		case DELETE:
			return (
				(actor != null)
				&& this.prsrcHandler.isAllowed(ResourcePropertyAccess.DELETE,
						actor)
			);
		default: // cannot happen
			return false;
		}
	}

	/**
	 * Default implementation simply creates a {@link PersistentResourceFetch},
	 * configures it according to the specified parameters, executes it and
	 * returns the fetch result.
	 */
	@SuppressWarnings("unused")
	@Override
	public List<R> search(final EndpointCallContext ctx,
			final PropertiesFetchSpec<R> propsFetch, final FilterSpec<R> filter,
			final OrderSpec<R> order, final RangeSpec range,
			final RangeResult rangeResult, final RefsFetchSpec<R> refsFetch,
			final RefsFetchResult refsResult)
		throws EndpointCallErrorException {

		final PersistentResourceFetch<R> fetch =
			ctx.getPersistenceTransaction().createPersistentResourceFetch(
					this.prsrcClass);
		if (propsFetch != null)
			fetch.setPropertiesFetch(propsFetch);
		if (filter != null)
			fetch.setFilter(filter);
		if (order != null)
			fetch.setOrder(order);
		if (range != null)
			fetch.setRange(range, rangeResult);
		if (refsFetch != null)
			fetch.setRefsFetch(refsFetch, refsResult);

		return fetch.getResultList();
	}

	/**
	 * Default implementation creates a filter with a single condition for the
	 * persistent resource record id.
	 */
	@SuppressWarnings("unused")
	@Override
	public FilterSpec<R> getRecordFilter(final EndpointCallContext ctx,
			final Object recId)
		throws EndpointCallErrorException {

		return ctx.getFilterSpec(this.prsrcClass)
				.addCondition(this.idPropHandler.getName(),
						FilterConditionType.EQ, false, recId);
	}

	/**
	 * Default implementation simply creates a {@link PersistentResourceFetch},
	 * sets filter provided by the {@code recFilter} parameter, executes the
	 * fetch and returns the first result.
	 */
	@SuppressWarnings("unused")
	@Override
	public R get(final EndpointCallContext ctx, final Object recId,
			final FilterSpec<R> recFilter,
			final PropertiesFetchSpec<R> propsFetch)
		throws EndpointCallErrorException {

		final PersistentResourceFetch<R> fetch = ctx
				.getPersistenceTransaction()
				.createPersistentResourceFetch(this.prsrcClass)
				.setFilter(recFilter)
				.lockResult(LockType.SHARED);
		if (propsFetch != null)
			fetch.setPropertiesFetch(propsFetch);

		return fetch.getFirstResult();
	}

	/**
	 * Default implementation simply calls
	 * {@link PersistenceTransaction#persist(Class, Object)}.
	 */
	@SuppressWarnings("unused")
	@Override
	public void create(final EndpointCallContext ctx, final R recTmpl)
		throws EndpointCallErrorException {

		ctx.getPersistenceTransaction().persist(this.prsrcClass, recTmpl);
	}

	/**
	 * Default implementation simply calls
	 * {@link PersistenceTransaction#update(Class, Object, Object, Set)}.
	 */
	@SuppressWarnings("unused")
	@Override
	public Set<String> update(final EndpointCallContext ctx, final R rec,
			final R recTmpl)
		throws EndpointCallErrorException {

		final Set<String> updatedProps = new HashSet<>();
		ctx.getPersistenceTransaction().update(this.prsrcClass, rec, recTmpl,
				updatedProps);

		return updatedProps;
	}

	/**
	 * Default implementation uses
	 * {@link PersistenceTransaction#delete(Class, FilterSpec, Set)} with the
	 * filter provided by the {@code recFilter} parameter.
	 */
	@SuppressWarnings("unused")
	@Override
	public Set<Class<?>> delete(final EndpointCallContext ctx,
			final Object recId, final FilterSpec<R> recFilter)
		throws EndpointCallErrorException {

		final Set<Class<?>> affectedResources = new HashSet<>();
		ctx.getPersistenceTransaction().delete(this.prsrcClass, recFilter,
				affectedResources);

		return affectedResources;
	}
}
