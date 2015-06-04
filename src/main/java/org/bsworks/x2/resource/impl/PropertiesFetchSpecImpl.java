package org.bsworks.x2.resource.impl;

import java.util.Deque;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Properties fetch specification object implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class PropertiesFetchSpecImpl<R>
	implements PropertiesFetchSpec<R> {

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
	public PropertiesFetchSpec<R> include(final String propPath) {

		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		if (propChain.getLast() instanceof ObjectPropertyHandler)
			throw new IllegalArgumentException("Included property cannot be a"
					+ " nested object itself.");

		this.includePaths.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpec<R> includeByDefault() {

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
	public PropertiesFetchSpec<R> exclude(final String propPath) {

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

			// check if all properties in the chain are fetched by default
			for (final ResourcePropertyHandler ph :
					this.prsrcHandler.getPersistentPropertyChain(propPath)) {
				if (!ph.isFetchedByDefault())
					return false;
			}

			// OK, it's included
			return true;
		}

		// default mode, tell if explicitly included
		return included;
	}
}
