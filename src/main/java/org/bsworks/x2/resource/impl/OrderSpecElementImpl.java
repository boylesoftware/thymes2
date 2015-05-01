package org.bsworks.x2.resource.impl;

import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.OrderSpecElement;
import org.bsworks.x2.resource.OrderType;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Order specification element implementation.
 *
 * @author Lev Himmelfarb
 */
class OrderSpecElementImpl
	implements OrderSpecElement {

	/**
	 * Order type.
	 */
	private final OrderType type;

	/**
	 * Property path.
	 */
	private final String propPath;

	/**
	 * Property chain.
	 */
	private final Deque<? extends ResourcePropertyHandler> propChain;


	/**
	 * Create new element.
	 *
	 * @param type Order type.
	 * @param prsrcHandler Top persistent resource handler.
	 * @param propPath Property path.
	 * @param prsrcClasses Set, to which to add any participating persistent
	 * resource classes.
	 */
	OrderSpecElementImpl(final OrderType type,
			final PersistentResourceHandlerImpl<?> prsrcHandler,
			final String propPath, final Set<Class<?>> prsrcClasses) {

		this.type = type;

		final boolean refId = propPath.endsWith("/id");
		if (refId)
			this.propPath =
				propPath.substring(0, propPath.length() - "/id".length());
		else
			this.propPath = propPath;

		this.propChain = prsrcHandler.getPersistentPropertyChain(this.propPath);

		final ResourcePropertyHandler lastProp = this.propChain.getLast();

		if (lastProp instanceof AggregatePropertyHandler)
			throw new IllegalArgumentException(
					"Cannot order by aggregate property.");

		for (final Iterator<? extends ResourcePropertyHandler> i =
				this.propChain.iterator(); i.hasNext();) {
			final ResourcePropertyHandler prop = i.next();

			if (!prop.isSingleValued())
				throw new IllegalArgumentException(
						"Cannot order by collection property.");

			if ((prop instanceof RefPropertyHandler) && i.hasNext())
				prsrcClasses.add(((RefPropertyHandler) prop)
						.getReferredResourceClass());
			else if (prop instanceof DependentRefPropertyHandler)
				prsrcClasses.add(((DependentRefPropertyHandler) prop)
						.getReferredResourceClass());
		}

		if (lastProp instanceof ObjectPropertyHandler)
			throw new IllegalArgumentException(
					"Cannot order by nested object property.");

		if ((lastProp instanceof RefPropertyHandler)
				|| (lastProp instanceof DependentRefPropertyHandler)) {
			if (!refId)
				throw new IllegalArgumentException(
						"Cannot order by reference property.");
		} else {
			if (refId)
				throw new IllegalArgumentException("Cannot use /id qualifier"
						+ " unless the property is a reference.");
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public OrderType getType() {

		return this.type;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getPropertyPath() {

		return this.propPath;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPropertyChain() {

		return this.propChain;
	}
}
