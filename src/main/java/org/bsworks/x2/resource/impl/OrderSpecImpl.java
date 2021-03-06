package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpecBuilder;
import org.bsworks.x2.resource.OrderSpecElement;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.SortDirection;


/**
 * Order specification implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class OrderSpecImpl<R>
	implements OrderSpecBuilder<R> {

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Specification elements.
	 */
	private final List<OrderSpecElement> elements = new ArrayList<>();

	/**
	 * Read-only view of the specification elements.
	 */
	private final List<OrderSpecElement> elementsRO =
		Collections.unmodifiableList(this.elements);

	/**
	 * Segments.
	 */
	private final List<FilterSpec<R>> segments = new ArrayList<>();

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
	public List<OrderSpecElement> getElements() {

		return this.elementsRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isEmpty() {

		return (this.elements.isEmpty() && this.segments.isEmpty());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isUsed(final String propPath) {

		if (propPath.endsWith(".*")) {
			if (!this.usedProps.subSet(
					propPath,
					propPath.substring(0, propPath.length() - 2) + "/")
				.isEmpty())
			return true;
		} else if (this.usedProps.contains(propPath)
				|| !this.usedProps.subSet(propPath + ".", propPath + "/")
					.isEmpty())
			return true;

		for (final FilterSpec<R> segment : this.segments)
			if (segment.isUsed(propPath))
				return true;

		return false;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<Class<?>> getParticipatingPersistentResources() {

		return this.prsrcClassesRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public OrderSpecBuilder<R> add(final SortDirection dir,
			final String propPath) {

		return this.add(dir, propPath, PropertyValueFunction.PLAIN);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public OrderSpecBuilder<R> add(final SortDirection dir,
			final String propPath, final PropertyValueFunction func,
			final Object... funcParams) {

		final PropertyOrderSpecElementImpl el =
			new PropertyOrderSpecElementImpl(dir, this.prsrcHandler, propPath,
					func, funcParams, this.prsrcClasses);
		this.elements.add(el);

		this.usedProps.add(el.getPropertyPath());

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public OrderSpecBuilder<R> addSegment(final SortDirection dir,
			final FilterSpec<R> split) {

		this.elements.add(new SegmentOrderSpecElementImpl(dir, split));

		this.segments.add(split);

		this.prsrcClasses.addAll(split.getParticipatingPersistentResources());

		return this;
	}
}
