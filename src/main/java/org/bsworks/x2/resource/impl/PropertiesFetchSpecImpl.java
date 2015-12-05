package org.bsworks.x2.resource.impl;

import java.util.Collections;
import java.util.Deque;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PropertiesFetchSpecBuilder;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Properties fetch specification object implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class PropertiesFetchSpecImpl<R>
	implements PropertiesFetchSpecBuilder<R> {

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Tells if in "include by default" mode.
	 */
	private boolean includeByDefault = false;

	/**
	 * Included property paths.
	 */
	private final SortedSet<String> includePaths = new TreeSet<>();

	/**
	 * Excluded property paths.
	 */
	private final SortedSet<String> excludePaths = new TreeSet<>();

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
	PropertiesFetchSpecImpl(final ResourcesImpl resources,
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
	public PropertiesFetchSpecBuilder<R> include(final String propPath) {

		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		if (propChain.getLast() instanceof ObjectPropertyHandler)
			throw new InvalidSpecificationException("Included property cannot"
					+ " be a nested object itself.");

		this.includePaths.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> includeByDefault() {

		if (this.includeByDefault)
			throw new IllegalStateException("In include by default mode.");

		if (!this.includePaths.isEmpty() || !this.excludePaths.isEmpty())
			throw new IllegalStateException(
					"Must be called before adding properties.");

		this.includeByDefault = true;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> exclude(final String propPath) {

		if (!this.includeByDefault)
			throw new IllegalStateException("In exclude by default mode.");

		this.prsrcHandler.getPersistentPropertyChain(propPath);

		this.excludePaths.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isIncluded(final String propPath) {

		// check if explicitly included
		final boolean included = (this.includePaths.contains(propPath)
				|| !this.includePaths.subSet(propPath + ".", propPath + "/")
				.isEmpty());

		// check for exclusion
		if (this.includeByDefault) {

			// check that it is not explicitly excluded
			final StringBuilder propPathBuf = new StringBuilder(propPath);
			int dotInd = propPathBuf.length();
			do {
				propPathBuf.setLength(dotInd);
				if (this.excludePaths.contains(propPathBuf.toString()))
					return false;
			} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);

			// check if explicitly included
			if (included)
				return true;

			// check if all properties in the chain are fetched
			propPathBuf.setLength(0);
			for (final ResourcePropertyHandler ph :
					this.prsrcHandler.getPersistentPropertyChain(propPath)) {
				if (propPathBuf.length() > 0)
					propPathBuf.append('.');
				propPathBuf.append(ph.getName());
				if (!ph.isFetchedByDefault()
						&& !this.includePaths.contains(propPathBuf.toString()))
					return false;
			}

			// OK, it's included
			return true;
		}

		// default mode, tell if explicitly included
		return included;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecImpl<R> fetch(final String propPath) {

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
			throw new InvalidSpecificationException("The property " + propPath
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
