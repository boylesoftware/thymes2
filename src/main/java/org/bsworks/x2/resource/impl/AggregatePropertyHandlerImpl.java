package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	 * Pattern for property references in the value expression.
	 */
	private static final Pattern PROPREF_PATTERN =
		Pattern.compile("[a-z]\\w*(?:\\.[a-z]\\w*)*");


	/**
	 * Container class.
	 */
	private final Class<?> containerClass;

	/**
	 * Aggregated collection property path.
	 */
	private final String aggregatedCollectionPropertyPath;

	/**
	 * Deep aggregated resource property path.
	 */
	private String deepAggregatedResourcePropertyPath;

	/**
	 * Aggregation depth.
	 */
	private int aggregationDepth;

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
	 * Aggregated property paths.
	 */
	private final Set<String> aggregatedPropertyPaths = new HashSet<>();

	/**
	 * Read-only view of aggregated property paths.
	 */
	private final Set<String> aggregatedPropertyPathsRO =
		Collections.unmodifiableSet(this.aggregatedPropertyPaths);

	/**
	 * Map key property name, or {@code null} if not a map.
	 */
	private final String keyPropertyName;


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

		// get key property
		this.keyPropertyName = StringUtils.nullIfEmpty(propAnno.key());

		// map or single-valued?
		if (valueHandler.getCollectionDegree() > 0) {

			// make sure it's a map
			if (valueHandler.getType() != ResourcePropertyValueType.MAP)
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " cannot be a collection.");


			// make sure we have a key
			if (this.keyPropertyName == null)
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " is a map and must have a key attribute.");

		} else { // single-valued

			// make sure we don't have a key
			if (this.keyPropertyName != null)
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " is not a map and cannot have a key.");
		}
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
		final StringBuilder deepAggregatedResourcePropertyPath =
			new StringBuilder(128);
		deepAggregatedResourcePropertyPath.append(
				this.aggregatedCollectionPropertyPath);
		this.aggregationDepth = 0;
		if (this.aggregationValueExpression != null) {
			final Matcher m = PROPREF_PATTERN.matcher(
					this.aggregationValueExpression);
			final List<String> deepPath = new ArrayList<>();
			while (m.find()) {
				final String propPath = m.group();
				if (this.aggregatedPropertyPaths.add(propPath)) {
					final Deque<? extends ResourcePropertyHandler> propChain;
					try {
						propChain = this.aggregatedCollectionHandler
								.getPersistentPropertyChain(propPath);
					} catch (final IllegalArgumentException e) {
						throw new InitializationException("Property "
								+ this.getName() + " of "
								+ this.containerClass.getName()
								+ " has aggregation value expression that"
								+ " contains references to non-existent"
								+ " or not persistent properties.", e);
					}
					int depth = 0;
					for (final Iterator<? extends ResourcePropertyHandler> i =
							propChain.iterator(); true; depth++) {
						final ResourcePropertyHandler ph = i.next();
						if (!i.hasNext())
							break;
						if (depth < deepPath.size()) {
							if (!ph.getName().equals(deepPath.get(depth)))
								throw new InitializationException("Property "
										+ this.getName() + " of "
										+ this.containerClass.getName()
										+ " has aggregation value expression"
										+ " that contains references to nested"
										+ " properties that are not in the same"
										+ " chain of nested objects.");
						} else {
							deepPath.add(ph.getName());
						}
						deepAggregatedResourcePropertyPath.append(".").append(
								ph.getName());
						if (ph instanceof RefPropertyHandler) {
							this.usedPersistentResourceClasses.add(
									((RefPropertyHandler) ph)
										.getReferredResourceClass());
							this.lastIntermediateRefPath =
								deepAggregatedResourcePropertyPath.toString();
						} else if (ph instanceof DependentRefPropertyHandler) {
							this.usedPersistentResourceClasses.add(
									((DependentRefPropertyHandler) ph)
										.getReferredResourceClass());
							this.lastIntermediateRefPath =
								deepAggregatedResourcePropertyPath.toString();
						} else if (ph instanceof ObjectPropertyHandler) {
							if (((ObjectPropertyHandler) ph).isBorrowed())
								this.usedPersistentResourceClasses.add(
									((ObjectPropertyHandler) ph)
										.getOwningPersistentResourceClass());
						}
					}
					final ResourcePropertyHandler leafPH = propChain.getLast();
					if (!leafPH.isSingleValued()
							|| (!(leafPH instanceof SimplePropertyHandler)
									&& !(leafPH instanceof IdPropertyHandler)
									&& !(leafPH instanceof RefPropertyHandler)))
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
			this.aggregatedPropertyPaths.add(
					this.aggregatedCollectionHandler.getIdProperty().getName());
		}
		this.deepAggregatedResourcePropertyPath =
			deepAggregatedResourcePropertyPath.toString();

		// validate key property
		if (this.keyPropertyName != null) {

			this.aggregatedPropertyPaths.add(this.keyPropertyName);

			final ResourcePropertyHandler ph =
				this.aggregatedCollectionHandler.getProperties().get(
						this.keyPropertyName);
			if ((ph == null) || !ph.isSingleValued()
					|| !(ph.getValueHandler()
							instanceof CanBeMapKeyResourcePropertyValueHandler))
				throw new InitializationException("Property "
						+ this.getName() + " of "
						+ this.containerClass.getName()
						+ " refers to key property that does not exist or may"
						+ " not be used as a map key.");
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
	public String getDeepAggregatedResourcePropertyPath() {

		return this.deepAggregatedResourcePropertyPath;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public int getAggregationDepth() {

		return this.aggregationDepth;
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
	public Matcher getValuePropertiesMatcher() {

		if (this.aggregationValueExpression == null)
			return null;

		return PROPREF_PATTERN.matcher(this.aggregationValueExpression);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getKeyPropertyName() {

		return this.keyPropertyName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Set<String> getAggregatedPropertyPaths() {

		return this.aggregatedPropertyPathsRO;
	}
}
