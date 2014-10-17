package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.OrderType;


/**
 * Order specification implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class OrderSpecImpl<R>
	implements OrderSpec<R> {

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Specification elements.
	 */
	private final List<OrderSpecElementImpl> elements = new ArrayList<>();

	/**
	 * Read-only view of the specification elements.
	 */
	private final List<OrderSpecElementImpl> elementsRO =
		Collections.unmodifiableList(this.elements);

	/**
	 * Used property paths.
	 */
	private final SortedSet<String> usedProps = new TreeSet<>();

	/**
	 * Participating persistent resource classes.
	 */
	private final Set<Class<?>> prsrcClasses = new HashSet<>();

	/**
	 * Read-only view of participating persistent resource classes.
	 */
	private final Set<Class<?>> prsrcClassesRO =
		Collections.unmodifiableSet(this.prsrcClasses);


	/**
	 * Create new specification object.
	 *
	 * @param prsrcHandler Persistent resource handler.
	 */
	OrderSpecImpl(final PersistentResourceHandlerImpl<R> prsrcHandler) {

		this.prsrcHandler = prsrcHandler;
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
	public OrderSpecImpl<R> add(final OrderType orderType,
			final String propPath) {

		this.elements.add(new OrderSpecElementImpl(orderType, this.prsrcHandler,
				propPath, this.prsrcClasses));

		this.usedProps.add(propPath);

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public List<OrderSpecElementImpl> getElements() {

		return this.elementsRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isEmpty() {

		return this.usedProps.isEmpty();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isUsed(final String propPath) {

		return (this.usedProps.contains(propPath)
				|| !this.usedProps.subSet(propPath + ".", propPath + "/")
					.isEmpty());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<Class<?>> getParticipatingPersistentResources() {

		return this.prsrcClassesRO;
	}
}
