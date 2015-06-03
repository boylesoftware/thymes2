package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.Set;

import org.bsworks.x2.InitializationException;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.annotations.DependentRefProperty;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;


/**
 * Dependent persistent resource reference property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class DependentRefPropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements DependentRefPropertyHandler {

	/**
	 * Container class.
	 */
	private final Class<?> containerClass;

	/**
	 * Target dependent resource class.
	 */
	private final Class<?> referredResourceClass;

	/**
	 * Reverse reference property name.
	 */
	private final String reverseRefPropertyName;

	/**
	 * Tells if optional.
	 */
	private final boolean optional;

	/**
	 * Persistence descriptor.
	 */
	private ResourcePropertyPersistenceImpl persistence;


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
		super(containerClass, pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						TargetType.DEP_REF),
				null, propAnno.updateIfNull(), propAnno.fetchedByDefault());

		// container class must be a persistent resource
		if (!prsrcClasses.contains(containerClass))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must belong to a persistent resource.");

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

		// get specific properties
		this.containerClass = containerClass;
		this.referredResourceClass = leafValueHandler.getRefTargetClass();
		this.reverseRefPropertyName = propAnno.reverseRefProperty();
		this.optional = propAnno.optional();
	}


	/**
	 * Complete handler initialization.
	 *
	 * @param persistentResources All persistent resource handlers by persistent
	 * resource names.
	 *
	 * @throws InitializationException If handler configuration is invalid.
	 */
	void completeInitialization(
			final Map<String, PersistentResourceHandlerImpl<?>>
			persistentResources)
		throws InitializationException {

		final PersistentResourceHandlerImpl<?> targetHandler =
			persistentResources.get(this.referredResourceClass.getSimpleName());

		final AbstractResourcePropertyHandlerImpl reverseRefHandler =
			targetHandler.getProperties().get(this.reverseRefPropertyName);
		if (reverseRefHandler == null)
			throw new InitializationException("Invalid reverse reference"
					+ " property name for property " + this.getName() + " of "
					+ this.containerClass.getName() + ".");

		final AbstractResourcePropertyValueHandlerImpl vh =
			reverseRefHandler.getValueHandler();
		if ((vh.getType() != ResourcePropertyValueType.REF)
				|| ((RefResourcePropertyValueHandler) vh).isWildcard()
				|| !((RefResourcePropertyValueHandler) vh)
						.getRefTargetClass().equals(this.containerClass))
			throw new InitializationException("Reverse reference property"
					+ " for property " + this.getName() + " of "
					+ this.containerClass.getName() + " has invalid type.");

		final ResourcePropertyPersistenceImpl reverseRefPD =
			reverseRefHandler.getPersistence();
		if (reverseRefPD == null)
			throw new InitializationException("Reverse reference property"
					+ " for property " + this.getName() + " of "
					+ this.containerClass.getName() + " must be persistent.");

		this.persistence = new ResourcePropertyPersistenceImpl(null,
				targetHandler.getPersistentCollectionName(),
				reverseRefPD.getFieldName(), null, this.optional);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourcePropertyPersistenceImpl getPersistence() {

		return this.persistence;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getReferredResourceClass() {

		return this.referredResourceClass;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getReverseRefPropertyName() {

		return this.reverseRefPropertyName;
	}
}
