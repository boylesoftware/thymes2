package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.FilterSpecBuilder;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.PropertyValueFunction;


/**
 * Filter specification implementation.
 *
 * @param <R> Fetch persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class FilterSpecImpl<R>
	implements FilterSpecBuilder<R> {

	/**
	 * Application resources manager.
	 */
	private final ResourcesImpl resources;

	/**
	 * Persistent resource handler.
	 */
	private final PersistentResourceHandlerImpl<R> prsrcHandler;

	/**
	 * Tells if disjunction.
	 */
	private boolean disjunction;

	/**
	 * Parent, or {@code null}.
	 */
	private final FilterSpecImpl<R> parent;

	/**
	 * Base path.
	 */
	private String basePath;

	/**
	 * Nested junctions.
	 */
	private final Collection<FilterSpecImpl<R>> junctions = new ArrayList<>();

	/**
	 * Read-only view of the nested junctions.
	 */
	private final Collection<FilterSpecImpl<R>> junctionsRO =
		Collections.unmodifiableCollection(this.junctions);

	/**
	 * Filter conditions.
	 */
	private final Collection<FilterConditionImpl> conditions =
		new ArrayList<>();

	/**
	 * Read-only view of the filter conditions.
	 */
	private final Collection<FilterConditionImpl> conditionsRO =
		Collections.unmodifiableCollection(this.conditions);

	/**
	 * Used property paths.
	 */
	private final SortedSet<String> usedProps = new TreeSet<>();

	/**
	 * Read-only view of the used property paths.
	 */
	private final SortedSet<String> usedPropsRO =
		Collections.unmodifiableSortedSet(this.usedProps);

	/**
	 * Participating persistent resource classes.
	 */
	private final Set<Class<?>> prsrcClasses;

	/**
	 * Read-only view of participating persistent resource classes.
	 */
	private final Set<Class<?>> prsrcClassesRO;


	/**
	 * Create new specification object.
	 *
	 * @param resources Application resources manager.
	 * @param prsrcHandler Persistent resource handler.
	 * @param disjunction {@code true} if disjunction.
	 * @param parent Parent filter or {@code null} for the top.
	 */
	FilterSpecImpl(final ResourcesImpl resources,
			final PersistentResourceHandlerImpl<R> prsrcHandler,
			final boolean disjunction, final FilterSpecImpl<R> parent) {

		this.resources = resources;
		this.prsrcHandler = prsrcHandler;
		this.disjunction = disjunction;
		this.parent = parent;

		if (parent == null) {
			this.basePath = "";
			this.prsrcClasses = new HashSet<>();
			this.prsrcClassesRO =
				Collections.unmodifiableSet(this.prsrcClasses);
		} else {
			this.basePath = parent.basePath;
			this.prsrcClasses = parent.prsrcClasses;
			this.prsrcClassesRO = parent.prsrcClassesRO;
		}
	}


	/**
	 * Add used property path.
	 *
	 * @param propPath Property path.
	 */
	void addUsedProperty(final String propPath) {

		this.usedProps.add(propPath);

		if (this.parent != null)
			this.parent.addUsedProperty(propPath);
	}

	/**
	 * Update base path of this builder and recursively all of its nested con-
	 * and disjunctions.
	 *
	 * @param basePath New base path.
	 */
	void updateBasePath(final String basePath) {

		this.basePath = basePath;

		for (final FilterSpecImpl<R> junc : this.junctions)
			junc.updateBasePath(basePath);
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
	public boolean isDisjunction() {

		return this.disjunction;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.resource.FilterSpec#makeDisjunction()
	 */
	@Override
	public FilterSpecBuilder<R> makeDisjunction() {

		this.disjunction = true;

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecBuilder<R> setBasePath(final String propPath) {

		// validate the path
		this.prsrcHandler.getPersistentPropertyChain(propPath);

		// make sure used properties are children of the new base path
		final String basePath = propPath + ".";
		for (final String usedPropPath : this.usedProps) {
			if (!usedPropPath.startsWith(basePath))
				throw new InvalidSpecificationException("Property "
						+ usedPropPath + " used by the filter belongs to a"
						+ " different base path.");
		}

		// update base path
		this.updateBasePath(basePath);

		// done
		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getBasePath() {

		return (this.basePath.isEmpty() ? "" :
			this.basePath.substring(0, this.basePath.length() - 1));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecImpl<R> getParent() {

		return this.parent;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecImpl<R> addConjunction() {

		final FilterSpecImpl<R> j =
			new FilterSpecImpl<>(this.resources, this.prsrcHandler, false,
					this);

		this.junctions.add(j);

		return j;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecImpl<R> addDisjunction() {

		final FilterSpecImpl<R> j =
			new FilterSpecImpl<>(this.resources, this.prsrcHandler, true, this);

		this.junctions.add(j);

		return j;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecBuilder<R> addCondition(final String propPath,
			final PropertyValueFunction func, final Object[] funcParams,
			final FilterConditionType type, final boolean negate,
			final Object... operands) {

		final FilterConditionImpl c = new FilterConditionImpl(this.resources,
				type, func, funcParams, negate, this.prsrcHandler,
				this.basePath + propPath, operands, this.prsrcClasses);

		this.conditions.add(c);

		this.addUsedProperty(c.getPropertyPath());

		return this;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterSpecBuilder<R> addTrueCondition(final String propPath,
			final FilterConditionType type, final Object... operands) {

		return this.addCondition(propPath, PropertyValueFunction.PLAIN, null,
				type, false, operands);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<FilterConditionImpl> getConditions() {

		return this.conditionsRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<? extends FilterSpec<R>> getJunctions() {

		return this.junctionsRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SortedSet<String> getUsedProperties() {

		return this.usedPropsRO;
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

		if (propPath.endsWith(".*"))
			return !this.usedProps.subSet(
						propPath,
						propPath.substring(0, propPath.length() - 2) + "/")
					.isEmpty();

		return (this.usedProps.contains(propPath)
				|| !this.usedProps.subSet(propPath + ".", propPath + "/")
					.isEmpty());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isByIdOnly() {

		return ((this.usedProps.size() == 1) && this.usedProps.contains(
				this.prsrcHandler.getIdProperty().getName()));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<Class<?>> getParticipatingPersistentResources() {

		return this.prsrcClassesRO;
	}
}
