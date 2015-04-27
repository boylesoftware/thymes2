package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Set;

import org.bsworks.x2.resource.DependentResourcePropertyHandler;


/**
 * Abstract parent for dependent persistent resource property handler
 * implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class AbstractDependentResourcePropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements DependentResourcePropertyHandler {

	/**
	 * Container class.
	 */
	protected final Class<?> containerClass;

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
	 * @param valueHandler Resource property value handler.
	 * @param accessChecker Access checker.
	 * @param updateIfNull Update if null flag.
	 * @param referredResourceClass Target dependent resource class. Must be a
	 * persistent resource class.
	 * @param reverseRefPropertyName Reverse reference property name.
	 * @param optional Optional flag.
	 */
	protected AbstractDependentResourcePropertyHandlerImpl(
			final Set<Class<?>> prsrcClasses, final Class<?> containerClass,
			final PropertyDescriptor pd,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final AccessChecker accessChecker, final boolean updateIfNull,
			final Class<?> referredResourceClass,
			final String reverseRefPropertyName, final boolean optional) {
		super(pd, valueHandler, accessChecker, null, updateIfNull);

		// container class must be a persistent resource
		if (!prsrcClasses.contains(containerClass))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must belong to a persistent resource.");

		// save specific properties
		this.containerClass = containerClass;
		this.referredResourceClass = referredResourceClass;
		this.reverseRefPropertyName = reverseRefPropertyName;
		this.optional = optional;
	}


	/**
	 * Create and set property persistence descriptor.
	 *
	 * @param targetHandler Reference target persistent resource handler.
	 */
	void setPersistence(final PersistentResourceHandlerImpl<?> targetHandler) {

		final AbstractResourcePropertyHandlerImpl reverseRefHandler =
			targetHandler.getProperties().get(this.reverseRefPropertyName);
		if (reverseRefHandler == null)
			throw new IllegalArgumentException("Invalid reverse reference"
					+ " property name for property " + this.getName() + " of "
					+ this.containerClass.getName() + ".");

		final AbstractResourcePropertyValueHandlerImpl vh =
			reverseRefHandler.getValueHandler();
		if ((vh.getType() != ResourcePropertyValueType.REF)
				|| ((RefResourcePropertyValueHandler) vh).isWildcard()
				|| !((RefResourcePropertyValueHandler) vh)
						.getRefTargetClass().equals(this.containerClass))
			throw new IllegalArgumentException("Reverse reference property"
					+ " for property " + this.getName() + " of "
					+ this.containerClass.getName() + " has invalid type.");

		final ResourcePropertyPersistenceImpl reverseRefPD =
			reverseRefHandler.getPersistence();
		if (reverseRefPD == null)
			throw new IllegalArgumentException("Reverse reference property"
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
