package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Set;

import org.bsworks.x2.resource.DependentAggregatePropertyHandler;
import org.bsworks.x2.resource.annotations.AggregationFunction;
import org.bsworks.x2.resource.annotations.DependentAggregateProperty;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.services.persistence.PersistentValueType;
import org.bsworks.x2.util.StringUtils;


/**
 * Dependent persistent resource aggregate property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class DependentAggregatePropertyHandlerImpl
	extends AbstractDependentResourcePropertyHandlerImpl
	implements DependentAggregatePropertyHandler {

	/**
	 * Aggregation function.
	 */
	private final AggregationFunction function;

	/**
	 * Aggregation property name.
	 */
	private final String aggregationPropertyName;


	/**
	 * Create new handler.
	 *
	 * @param prsrcClasses All persistent resource classes. Used to check the
	 * reference target.
	 * @param containerClass Class that contains the property. Must be a
	 * persistent resource class.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Leaf resource property value handler.
	 */
	DependentAggregatePropertyHandlerImpl(final Set<Class<?>> prsrcClasses,
			final Class<?> containerClass, final PropertyDescriptor pd,
			final DependentAggregateProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final SimpleResourcePropertyValueHandler leafValueHandler) {
		super(prsrcClasses, containerClass, pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						TargetType.DEP_AGGREGATE),
				false, propAnno.resourceClass(), propAnno.reverseRefProperty(),
				propAnno.optional());

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
		this.function = propAnno.func();
		this.aggregationPropertyName = StringUtils.nullIfEmpty(
				propAnno.aggregationProperty());

		// make sure we have aggregation property if not count
		if ((this.aggregationPropertyName == null)
				&& (this.function != AggregationFunction.COUNT))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must have aggregation property specified.");
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	void setPersistence(final PersistentResourceHandlerImpl<?> targetHandler) {

		super.setPersistence(targetHandler);

		if (this.aggregationPropertyName != null) {

			final AbstractResourcePropertyHandlerImpl aggPropHandler =
				targetHandler.getProperties().get(this.aggregationPropertyName);
			if (aggPropHandler == null)
				throw new IllegalArgumentException("Invalid aggregation"
						+ " property name for property " + this.getName()
						+ " of " + this.containerClass.getName() + ".");
			if (!aggPropHandler.isSingleValued())
				throw new IllegalArgumentException("Property "
						+ aggPropHandler.getName()
						+ " of " + targetHandler.getResourceClass().getName()
						+ " must be single-valued to be used in aggregates.");
			if (aggPropHandler.getPersistence() == null)
				throw new IllegalArgumentException("Property "
						+ aggPropHandler.getName()
						+ " of " + targetHandler.getResourceClass().getName()
						+ " must be persistent to be used in aggregates.");

			switch (this.function) {
			case COUNT:
			case COUNT_DISTINCT:
				break;
			case MIN:
			case MAX:
				if (this.getValueHandler().getType()
						!= aggPropHandler.getValueHandler().getType())
					throw new IllegalArgumentException("Property "
							+ this.getName() + " of "
							+ this.containerClass.getName()
							+ " must have same type as property "
							+ aggPropHandler.getName() + " of "
							+ targetHandler.getResourceClass().getName() + ".");
			default:
			}
		}

		switch (this.function) {
		case COUNT:
		case COUNT_DISTINCT:
			if (this.getValueHandler().getPersistentValueType()
					!= PersistentValueType.NUMERIC)
				throw new IllegalArgumentException("Property "
					+ this.getName() + " of "
					+ this.containerClass.getName() + " must be numeric.");
			break;
		default:
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isFetchedByDefault() {

		return false;
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
	public String getAggregationPropertyName() {

		return this.aggregationPropertyName;
	}
}
