package org.bsworks.x2.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.RefsFetchSpec;
import org.bsworks.x2.services.persistence.PersistenceTransaction;
import org.bsworks.x2.services.persistence.PersistenceTransactionHandler;


/**
 * Endpoint call context implementation.
 *
 * @author Lev Himmelfarb
 */
class EndpointCallContextImpl
	implements EndpointCallContext {

	/**
	 * Instance runtime context.
	 */
	private final RuntimeContextImpl runtimeCtx;

	/**
	 * Call HTTP method.
	 */
	private final HttpMethod requestMethod;

	/**
	 * Context-relative request URI.
	 */
	private final String requestURI;

	/**
	 * Positional URI parameters.
	 */
	private final List<String> uriParams;

	/**
	 * The HTTP request.
	 */
	private final HttpServletRequest httpRequest;

	/**
	 * The actor making the request, or {@code null}.
	 */
	private Actor actor;

	/**
	 * Tell if the actor was set in the context using the
	 * {@link #assumeActor(Actor)} method.
	 */
	private boolean assumedActor;

	/**
	 * Context attributes.
	 */
	private final Map<String, Object> attributes = new HashMap<>();

	/**
	 * Tells if the call does not intend to modify any persistent data.
	 */
	private boolean readOnly;

	/**
	 * Handler of the persistence transaction, associated with the context.
	 */
	private PersistenceTransactionHandler tx;

	/**
	 * Side-tasks to submit for execution on successful transaction commit.
	 */
	private final Collection<Runnable> tasksOnCommit = new ArrayList<>();


	/**
	 * Create new context.
	 *
	 * @param runtimeCtx Instance runtime context.
	 * @param requestMethod Call HTTP method.
	 * @param requestURI Context-relative request URI.
	 * @param uriParams Unmodifiable list of positional URI parameters.
	 * @param httpRequest The HTTP request.
	 * @param actor The actor making the request, or {@code null}.
	 * @param readOnly {@code true} if the call does not intend to modify any
	 * persistent data.
	 */
	EndpointCallContextImpl(final RuntimeContextImpl runtimeCtx,
			final HttpMethod requestMethod, final String requestURI,
			final List<String> uriParams, final HttpServletRequest httpRequest,
			final Actor actor, final boolean readOnly) {

		this.runtimeCtx = runtimeCtx;
		this.requestMethod = requestMethod;
		this.requestURI = requestURI;
		this.uriParams = uriParams;
		this.httpRequest = httpRequest;
		this.actor = actor;
		this.assumedActor = false;
		this.readOnly = readOnly;
	}


	/**
	 * Close the context.
	 *
	 * @param commit {@code true} to commit the persistence transaction if one
	 * is associated with the context.
	 */
	void close(final boolean commit) {

		this.endPersistenceTransaction(commit);
	}

	/**
	 * End persistence transaction associated with the context, if any.
	 *
	 * @param commit {@code true} to commit the persistence transaction.
	 */
	private void endPersistenceTransaction(final boolean commit) {

		final PersistenceTransactionHandler tx = this.tx;
		if (tx != null) {
			this.tx = null;
			try {
				if (commit) {
					tx.commitTransaction();
					for (final Runnable task : this.tasksOnCommit)
						this.runtimeCtx.submitSideTask(task);
				}
			} finally {
				this.tasksOnCommit.clear();
				tx.close();
			}
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public RuntimeContextImpl getRuntimeContext() {

		return this.runtimeCtx;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public HttpMethod getRequestMethod() {

		return this.requestMethod;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getRequestContextPath() {

		return this.httpRequest.getContextPath();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getRequestURI() {

		return this.requestURI;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getRequestURIParam(final int pos) {

		if (pos < 0)
			return this.uriParams.get(this.uriParams.size() + pos);

		return this.uriParams.get(pos);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getRequestParam(final String name) {

		return this.httpRequest.getParameter(name);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String[] getRequestParamValues(final String name) {

		return this.httpRequest.getParameterValues(name);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getRequestHeader(final String name) {

		return this.httpRequest.getHeader(name);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Date getRequestDateHeader(final String name) {

		final long val = this.httpRequest.getDateHeader(name);

		return (val < 0 ? null : new Date(val));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void setAttribute(final String name, final Object value) {

		this.attributes.put(name, value);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <Y> Y getAttribute(final String name, final Class<Y> valueClass) {

		final Object value = this.attributes.get(name);
		if (value == null)
			return null;

		return valueClass.cast(value);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Actor getActor() {

		return this.actor;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void assumeActor(final Actor actor) {

		this.endPersistenceTransaction(true);

		this.actor = actor;
		this.assumedActor = true;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isAssumedActor() {

		return this.assumedActor;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistenceTransaction getPersistenceTransaction() {

		if (this.tx == null)
			this.tx = this.runtimeCtx.getPersistenceService()
				.createPersistenceTransaction(this.actor, this.readOnly);

		return this.tx.getTransaction();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void submitSideTask(final Runnable task,
			final boolean delayUntilCommit) {

		if (delayUntilCommit) {
			if (this.tx == null)
				throw new IllegalStateException("There is no persistence"
						+ " transaction associated with the context.");
			this.tasksOnCommit.add(task);
		} else {
			this.runtimeCtx.submitSideTask(task);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> Ref<R> createRef(final Class<R> prsrcClass, final Object rec) {

		return this.runtimeCtx.getResources().createRef(prsrcClass, rec);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void registerPersistentResourceModification(
			final Class<?>... prsrcClasses) {

		final Set<Class<?>> prsrcClassesSet = new HashSet<>(
				prsrcClasses.length > 16 ? prsrcClasses.length : 16);
		for (final Class<?> prsrcClass : prsrcClasses)
			prsrcClassesSet.add(prsrcClass);

		this.runtimeCtx.getPersistentResourceVersioningService()
			.registerCollectionsModification(this.getPersistenceTransaction(),
					prsrcClassesSet);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void registerPersistentResourceModification(
			final Set<Class<?>> prsrcClasses) {

		this.runtimeCtx.getPersistentResourceVersioningService()
			.registerCollectionsModification(this.getPersistenceTransaction(),
					prsrcClasses);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void lockPersistentResourceCollections(
			final Set<Class<?>> prsrcClasses) {

		this.runtimeCtx.getPersistentResourceVersioningService()
			.lockCollections(this.getPersistenceTransaction(), prsrcClasses);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> RefsFetchSpec<R> getRefsFetchSpec(final Class<R> prsrcClass) {

		return this.runtimeCtx.getResources().getRefsFetchSpec(prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> PropertiesFetchSpec<R> getPropertiesFetchSpec(
			final Class<R> prsrcClass) {

		return this.runtimeCtx.getResources().getPropertiesFetchSpec(
				prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> FilterSpec<R> getFilterSpec(final Class<R> prsrcClass) {

		return this.runtimeCtx.getResources().getFilterSpec(prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> OrderSpec<R> getOrderSpec(final Class<R> prsrcClass) {

		return this.runtimeCtx.getResources().getOrderSpec(prsrcClass);
	}
}
