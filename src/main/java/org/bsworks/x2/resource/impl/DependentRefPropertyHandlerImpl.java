package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Set;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.annotations.DependentRefProperty;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;


/**
 * Dependent persistent resource reference property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class DependentRefPropertyHandlerImpl
	extends AbstractDependentResourcePropertyHandlerImpl
	implements DependentRefPropertyHandler {

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
	DependentRefPropertyHandlerImpl(final Set<Class<?>> prsrcClasses,
			final Class<?> containerClass, final PropertyDescriptor pd,
			final DependentRefProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final RefResourcePropertyValueHandler leafValueHandler) {
		super(prsrcClasses, containerClass, pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						TargetType.DEP_REF),
				propAnno.updateIfNull(), leafValueHandler.getRefTargetClass(),
				propAnno.reverseRefProperty(), propAnno.optional());

		// check maximum collection degree for persistent property
		final int collectionDegree = valueHandler.getCollectionDegree();
		if (collectionDegree > 1)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " has more than one collection degree.");

		// check it's not a map
		if (valueHandler.getType() == ResourcePropertyValueType.MAP)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be a map.");

		// check target is not a wildcard
		if (leafValueHandler.isWildcard())
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot have a wildcard target.");
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isFetchedByDefault() {

		return true;
	}
}
