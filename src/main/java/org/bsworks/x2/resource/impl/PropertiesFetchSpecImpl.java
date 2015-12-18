package org.bsworks.x2.resource.impl;

import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterSpec;
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
	 * An inclusion/exclusion rule.
	 */
	private static final class Rule {

		/**
		 * Handler of the property, with which the rule is associated/
		 */
		final ResourcePropertyHandler propHandler;

		/**
		 * Tell if the matching property is included, excluded, or unspecified
		 * (if {@code null}).
		 */
		Boolean includeSelf;

		/**
		 * Tell if the children of matching property are included, excluded, or
		 * unspecified (if {@code null}).
		 */
		Boolean includeChildren;


		/**
		 * Create new rule.
		 *
		 * @param propHandler Property handler.
		 * @param includeSelf Include self flag, or {@code null}.
		 * @param includeChildren Include children flag, or {@code null}.
		 */
		Rule(final ResourcePropertyHandler propHandler,
				final Boolean includeSelf, final Boolean includeChildren) {

			this.propHandler = propHandler;
			this.includeSelf = includeSelf;
			this.includeChildren = includeChildren;
		}
	}


	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Tells if in "include by default" mode.
	 */
	private boolean includeByDefault = false;

	/**
	 * The rules by property paths.
	 */
	private final Map<String, Rule> rules = new HashMap<>();

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
	 * Aggregate property filters by property paths.
	 */
	private final Map<String, FilterSpec<R>> aggergateFilters = new HashMap<>();


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
	public boolean isIncluded(final String propPath) {

		// get the property chain and validate the property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// find the longest matching rule
		final StringBuilder curPropPath = new StringBuilder(propPath);
		Rule rule = null;
		final int propPathLen = propPath.length();
		int dotInd = propPathLen;
		do {
			curPropPath.setLength(dotInd);
			rule = this.rules.get(curPropPath.toString());
		} while ((rule == null)
				&& ((dotInd = curPropPath.lastIndexOf(".")) > 0));

		// get the rule value
		final Boolean include;
		if (rule == null)
			include = null;
		else if (curPropPath.length() == propPathLen)
			include = rule.includeSelf;
		else
			include = rule.includeChildren;

		// use the rule if has rule
		if (include != null)
			return include.booleanValue();

		// no rule, default behavior:

		// check if all excluded by default
		if (!this.includeByDefault)
			return false;

		// check that the chain up to the rule is fetched by default
		for (final Iterator<? extends ResourcePropertyHandler> phIterator =
				propChain.descendingIterator(); phIterator.hasNext();) {
			final ResourcePropertyHandler ph = phIterator.next();
			if ((rule != null) && (rule.propHandler == ph))
				break;
			if (!ph.isFetchedByDefault())
				return false;
		}

		// included by default
		return true;
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

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpec<R> getAggregateFilter(final String propPath) {

		return this.aggergateFilters.get(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> includeByDefault() {

		if (this.includeByDefault)
			throw new IllegalStateException(
					"Already in include by default mode.");

		this.includeByDefault = true;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> include(final String propPath) {

		// get property chain and validate property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// last property may not be an object
		if (propChain.getLast() instanceof ObjectPropertyHandler)
			throw new InvalidSpecificationException("Property " + propPath
					+ " is a nested object and cannot be explicitely"
					+ " included.");

		// create rules for each property in the path
		final StringBuilder curPropPathBuf =
			new StringBuilder(propPath.length());
		for (final Iterator<? extends ResourcePropertyHandler> phIterator =
				propChain.iterator(); phIterator.hasNext();) {
			final ResourcePropertyHandler ph = phIterator.next();

			// create current property path
			if (curPropPathBuf.length() > 0)
				curPropPathBuf.append('.');
			curPropPathBuf.append(ph.getName());
			final String curPropPath = curPropPathBuf.toString();

			// create/update inclusion rule
			Rule rule = this.rules.get(curPropPath);
			if (rule == null) {
				rule = new Rule(ph, Boolean.TRUE, null);
				this.rules.put(curPropPath, rule);
			} else if (rule.includeSelf == null) {
				rule.includeSelf = Boolean.TRUE;
			}

			// add intermediate reference to fetch
			if (phIterator.hasNext()) {
				if (ph instanceof RefPropertyHandler)
					this.fetchedRefProps.put(curPropPath,
							((RefPropertyHandler) ph)
								.getReferredResourceClass());
				else if (ph instanceof DependentRefPropertyHandler)
					this.fetchedRefProps.put(curPropPath,
							((DependentRefPropertyHandler) ph)
								.getReferredResourceClass());
			}
		}

		// done
		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecImpl<R> fetch(final String propPath) {

		// get property chain and validate property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// last property must be a reference
		final ResourcePropertyHandler lastProp = propChain.getLast();
		if (!(lastProp instanceof RefPropertyHandler)
				&& !(lastProp instanceof DependentRefPropertyHandler))
			throw new InvalidSpecificationException("Property " + propPath
					+ " is not a reference.");

		// create rules for each property in the path
		final StringBuilder curPropPathBuf =
			new StringBuilder(propPath.length());
		for (final Iterator<? extends ResourcePropertyHandler> phIterator =
				propChain.iterator(); phIterator.hasNext();) {
			final ResourcePropertyHandler ph = phIterator.next();

			// create current property path
			if (curPropPathBuf.length() > 0)
				curPropPathBuf.append('.');
			curPropPathBuf.append(ph.getName());
			final String curPropPath = curPropPathBuf.toString();

			// create/update inclusion rule
			Rule rule = this.rules.get(curPropPath);
			if (rule == null) {
				rule = new Rule(ph, Boolean.TRUE, null);
				this.rules.put(curPropPath, rule);
			} else if (rule.includeSelf == null) {
				rule.includeSelf = Boolean.TRUE;
			}

			// add reference to fetch
			if (ph instanceof RefPropertyHandler)
				this.fetchedRefProps.put(curPropPath,
						((RefPropertyHandler) ph)
							.getReferredResourceClass());
			else if (ph instanceof DependentRefPropertyHandler)
				this.fetchedRefProps.put(curPropPath,
						((DependentRefPropertyHandler) ph)
							.getReferredResourceClass());
		}

		// done
		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> exclude(final String propPath) {

		// get property chain and validate property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// create/update exclusion rule
		Rule rule = this.rules.get(propPath);
		if (rule == null) {
			rule = new Rule(propChain.getLast(), Boolean.FALSE, Boolean.FALSE);
			this.rules.put(propPath, rule);
		} else {
			rule.includeSelf = Boolean.FALSE;
			rule.includeChildren = Boolean.FALSE;
		}

		// done
		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> includeFilteredAggregate(
			final String propPath, final FilterSpec<R> filter) {

		// get property chain and validate property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// last property must be an aggregate
		if (!(propChain.getLast() instanceof AggregatePropertyHandler))
			throw new InvalidSpecificationException("Property " + propPath
					+ " is not an aggregate.");

		// validate the filter conditions
		for (final String filterPropPath : filter.getUsedProperties()) {
			if (!filterPropPath.startsWith(propPath)
					|| (filterPropPath.length() == propPath.length()))
				throw new InvalidSpecificationException("Filter associated with"
						+ " aggregate property " + propPath
						+ " uses properties that do not belong to the"
						+ " aggregated collection.");
		}

		// include the property
		this.include(propPath);

		// save the filter
		this.aggergateFilters.put(propPath, filter);

		// done
		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertiesFetchSpecBuilder<R> excludeProperties(
			final String propPath) {

		// get property chain and validate property path
		final Deque<? extends ResourcePropertyHandler> propChain =
			this.prsrcHandler.getPersistentPropertyChain(propPath);

		// last property must be a reference
		final ResourcePropertyHandler lastProp = propChain.getLast();
		if (!(lastProp instanceof RefPropertyHandler)
				&& !(lastProp instanceof DependentRefPropertyHandler))
			throw new InvalidSpecificationException("Property " + propPath
					+ " is not a reference.");

		// create/update exclusion rule
		Rule rule = this.rules.get(propPath);
		if (rule == null) {
			rule = new Rule(lastProp, null, Boolean.FALSE);
			this.rules.put(propPath, rule);
		} else {
			rule.includeChildren = Boolean.FALSE;
		}

		// done
		return this;
	}
}
