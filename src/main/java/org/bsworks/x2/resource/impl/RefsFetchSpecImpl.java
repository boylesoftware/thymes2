package org.bsworks.x2.resource.impl;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.RefsFetchSpec;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Referred resources fetch specification implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class RefsFetchSpecImpl<R>
	implements RefsFetchSpec<R> {

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Fetched referred persistent resource classes by reference property paths.
	 */
	private final Map<String, Class<?>> fetchedRefProps = new HashMap<>();

	/**
	 * Read-only view of fetched reference properties.
	 */
	private final Map<String, Class<?>> fetchedRefPropsRO =
		Collections.unmodifiableMap(this.fetchedRefProps);


	/**
	 * Create new specification object.
	 *
	 * @param resources Application resources manager.
	 * @param prsrcClass Persistent resource class.
	 */
	RefsFetchSpecImpl(final ResourcesImpl resources,
			final Class<R> prsrcClass) {

		this.prsrcHandler = resources.getPersistentResourceHandler(prsrcClass);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<R> getPersistentResourceClass() {

		return this.prsrcHandler.getResourceClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public RefsFetchSpecImpl<R> add(final String propPath) {

		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		boolean lastWasRef = false;
		for (final ResourcePropertyHandler prop : propChain) {
			if (prop instanceof RefPropertyHandler) {
				lastWasRef = true;
				this.fetchedRefProps.put(
						ResourcesImpl.chainToPath(propChain, prop),
						((RefPropertyHandler) prop).getReferredResourceClass());
			} else if (prop instanceof DependentRefPropertyHandler) {
				lastWasRef = true;
				this.fetchedRefProps.put(
						ResourcesImpl.chainToPath(propChain, prop),
						((DependentRefPropertyHandler) prop)
							.getReferredResourceClass());
			} else {
				lastWasRef = false;
			}
		}
		if (!lastWasRef)
			throw new IllegalArgumentException("The property " + propPath
					+ " is not a reference.");

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isFetchRequested(final String propPath) {

		return this.fetchedRefProps.containsKey(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Map<String, Class<?>> getFetchedRefProperties() {

		return this.fetchedRefPropsRO;
	}
}
