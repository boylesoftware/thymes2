package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bsworks.x2.InitializationException;
import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertiesContainer;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.SimplePropertyHandler;
import org.bsworks.x2.resource.annotations.AggregateProperty;
import org.bsworks.x2.resource.annotations.AggregationFunction;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.util.StringUtils;


/**
 * Dependent persistent resource aggregate property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class AggregatePropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements AggregatePropertyHandler {

	/**
	 * Pattern for property names in the value expression.
	 */
	private static final Pattern PROPNAME_PATTERN =
		Pattern.compile("[a-z]\\w*");


	/**
	 * Container class.
	 */
	private final Class<?> containerClass;

	/**
	 * Aggregated collection property path.
	 */
	private final String aggregatedCollectionPropertyPath;

	/**
	 * Referred persistent resource classes.
	 */
	private final Set<Class<?>> usedPersistentResourceClasses =
		new HashSet<>();

	/**
	 * Read-only view of referred persistent resource classes.
	 */
	private final Set<Class<?>> usedPersistentResourceClassesRO =
		Collections.unmodifiableSet(this.usedPersistentResourceClasses);

	/**
	 * Longest intermediate reference path.
	 */
	private String lastIntermediateRefPath;

	/**
	 * Aggregation function.
	 */
	private final AggregationFunction function;

	/**
	 * Aggregated collection handler.
	 */
	private ResourcePropertiesContainer aggregatedCollectionHandler;

	/**
	 * Aggregation value expression.
	 */
	private final String aggregationValueExpression;

	/**
	 * Aggregation value property names.
	 */
	private final Set<String> usedValuePropertyNames = new HashSet<>();

	/**
	 * Read-only view of aggregation value property names.
	 */
	private final Set<String> usedValuePropertyNamesRO =
		Collections.unmodifiableSet(this.usedValuePropertyNames);


	/**
	 * Create new handler.
	 *
	 * @param containerClass Class that contains the property. Must be a
	 * persistent resource class.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Leaf resource property value handler.
	 */
	AggregatePropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd, final AggregateProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final SimpleResourcePropertyValueHandler leafValueHandler) {
		super(containerClass, pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						TargetType.AGGREGATE),
				null, false, false);

		// must be single-valued
		if (valueHandler.getCollectionDegree() > 0)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be a collection or a map.");

		// must not be primitive
		if (leafValueHandler.isPrimitive())
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be primitive to allow nulls.");

		// get specific properties
		this.containerClass = containerClass;
		this.aggregatedCollectionPropertyPath = propAnno.collection();
		this.function = propAnno.func();
		this.aggregationValueExpression = StringUtils.nullIfEmpty(
				propAnno.valueExpression());

		// make sure we have aggregation value expression if not count
		if ((this.aggregationValueExpression == null)
				&& (this.function != AggregationFunction.COUNT))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must have aggregation property specified.");
	}


	/**
	 * Complete handler initialization.
	 *
	 * @param prsrcHandler Handler of the persistent resource that contains the
	 * aggregate property.
	 * @param persistentResources All persistent resource handlers by persistent
	 * resource names.
	 *
	 * @throws InitializationException If handler configuration is invalid.
	 */
	void completeInitialization(
			final PersistentResourceHandlerImpl<?> prsrcHandler,
			final Map<String, PersistentResourceHandlerImpl<?>>
			persistentResources)
		throws InitializationException {

		// parse and validate aggregated collection property path
		final Deque<? extends ResourcePropertyHandler> colPropChain;
		try {
			colPropChain = prsrcHandler.getPersistentPropertyChain(
					this.aggregatedCollectionPropertyPath);
		} catch (final IllegalArgumentException e) {
			throw new InitializationException("Property " + this.getName()
					+ " of " + this.containerClass.getName()
					+ " has invalid aggregated collection property path.", e);
		}
		final StringBuilder curPropPath = new StringBuilder(64);
		for (final ResourcePropertyHandler ph : colPropChain) {
			if (curPropPath.length() > 0)
				curPropPath.append(".");
			curPropPath.append(ph.getName());
			if (ph instanceof RefPropertyHandler) {
				this.usedPersistentResourceClasses.add(
						((RefPropertyHandler) ph).getReferredResourceClass());
				this.lastIntermediateRefPath = curPropPath.toString();
			} else if (ph instanceof DependentRefPropertyHandler) {
				this.usedPersistentResourceClasses.add(
						((DependentRefPropertyHandler) ph)
							.getReferredResourceClass());
				this.lastIntermediateRefPath = curPropPath.toString();
			} else if (ph instanceof ObjectPropertyHandler) {
				if (((ObjectPropertyHandler) ph).isBorrowed())
					this.usedPersistentResourceClasses.add(
							((ObjectPropertyHandler) ph)
								.getOwningPersistentResourceClass());
			}
		}

		// get and analyze collection property
		final ResourcePropertyHandler colPropHandler = colPropChain.getLast();
		if (colPropHandler.isSingleValued())
			throw new InitializationException("Property " + this.getName()
					+ " of " + this.containerClass.getName()
					+ " has aggregated collection property path that points to"
					+ " a single-valued property.");
		if (colPropHandler instanceof ObjectPropertyHandler)
			this.aggregatedCollectionHandler =
				(ObjectPropertyHandler) colPropHandler;
		else if (colPropHandler instanceof RefPropertyHandler)
			this.aggregatedCollectionHandler = persistentResources.get(
					((RefPropertyHandler) colPropHandler)
						.getReferredResourceClass().getSimpleName());
		else if (colPropHandler instanceof DependentRefPropertyHandler)
			this.aggregatedCollectionHandler = persistentResources.get(
					((DependentRefPropertyHandler) colPropHandler)
						.getReferredResourceClass().getSimpleName());
		else
			throw new InitializationException("Property " + this.getName()
					+ " of " + this.containerClass.getName()
					+ " has aggregated collection property path that does not"
					+ " point to a nested object, a reference or a dependent"
					+ " resource reference.");
		if (this.aggregatedCollectionHandler == null)
			throw new InitializationException("Property " + this.getName()
					+ " of " + this.containerClass.getName()
					+ " has aggregated collection property path that points"
					+ " to a reference not associated with a concrete"
					+ " persistent resource.");

		// parse and validate the value expression
		if (this.aggregationValueExpression != null) {
			final Matcher m = PROPNAME_PATTERN.matcher(
					this.aggregationValueExpression);
			while (m.find()) {
				final String propName = m.group();
				if (this.usedValuePropertyNames.add(propName)) {
					final ResourcePropertyHandler ph =
						this.aggregatedCollectionHandler.getProperties().get(
								propName);
					if ((ph == null) || !ph.isSingleValued()
							|| (!(ph instanceof SimplePropertyHandler)
									&& !(ph instanceof IdPropertyHandler)
									&& !(ph instanceof RefPropertyHandler)))
						throw new InitializationException("Property "
								+ this.getName() + " of "
								+ this.containerClass.getName()
								+ " has aggregation value expression that"
								+ " contains references to non-existent,"
								+ " collection or not simple value"
								+ " properties.");
				}
			}
		} else {
			this.usedValuePropertyNames.add(
					this.aggregatedCollectionHandler.getIdProperty().getName());
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getAggregatedCollectionPropertyPath() {

		return this.aggregatedCollectionPropertyPath;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<Class<?>> getUsedPersistentResourceClasses() {

		return this.usedPersistentResourceClassesRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getLastIntermediateRefPath() {

		return this.lastIntermediateRefPath;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public AggregationFunction getFunction() {

		return this.function;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourcePropertiesContainer getAggregatedCollectionHandler() {

		return this.aggregatedCollectionHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getAggregationValueExpression() {

		return this.aggregationValueExpression;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<String> getUsedValuePropertyNames() {

		return this.usedValuePropertyNamesRO;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Matcher getValuePropertiesMatcher() {

		if (this.aggregationValueExpression == null)
			return null;

		return PROPNAME_PATTERN.matcher(this.aggregationValueExpression);
	}
}
