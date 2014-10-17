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
	 * Property paths.
	 */
	private final SortedSet<String> paths = new TreeSet<>();


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

		if (this.includeByDefault)
			throw new IllegalStateException("In include by default mode.");

		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		if (propChain.getLast() instanceof ObjectPropertyHandler)
			throw new IllegalArgumentException("Included property cannot be a"
					+ " nested object itself.");

		this.paths.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpec<R> includeByDefault() {

		if (this.includeByDefault)
			throw new IllegalStateException("In include by default mode.");

		if (!this.paths.isEmpty())
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

		this.paths.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isIncluded(final String propPath) {

		boolean included;
		if (this.includeByDefault) { // check for exclusion
			included = true;
			final StringBuilder propPathBuf = new StringBuilder(propPath);
			int dotInd = propPathBuf.length();
			do {
				propPathBuf.setLength(dotInd);
				if (this.paths.contains(propPathBuf.toString())) {
					included = false;
					break;
				}
			} while ((dotInd = propPathBuf.lastIndexOf(".")) > 0);
		} else { // check for inclusion
			included = (this.paths.contains(propPath)
					|| !this.paths.subSet(propPath + ".", propPath + "/")
						.isEmpty());
		}

		return included;
	}
}
