package org.bsworks.x2.resource.impl;

import java.util.Collections;
import java.util.Deque;
import java.util.SortedMap;
import java.util.TreeMap;

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
	private final SortedMap<String, Class<?>> fetchedRefProps = new TreeMap<>();

	/**
	 * Read-only view of fetched reference properties.
	 */
	private final SortedMap<String, Class<?>> fetchedRefPropsRO =
		Collections.unmodifiableSortedMap(this.fetchedRefProps);


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

		if (propPath.endsWith(".*"))
			return !this.fetchedRefProps
					.subMap(propPath,
							propPath.substring(0, propPath.length() - 2) + "/")
					.isEmpty();

		return this.fetchedRefProps.containsKey(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SortedMap<String, Class<?>> getFetchedRefProperties() {

		return this.fetchedRefPropsRO;
	}
}
