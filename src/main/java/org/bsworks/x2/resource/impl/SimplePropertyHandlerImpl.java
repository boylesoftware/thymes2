package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;

import org.bsworks.x2.resource.SimplePropertyHandler;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;


/**
 * Simple data resource property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class SimplePropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements SimplePropertyHandler {

	/**
	 * Create new handler.
	 *
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class, or {@code null} if
	 * the container class is transient. Used to check that persistent property
	 * belongs to a persistent container.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null} if persistent property.
	 * Otherwise, ignored.
	 */
	SimplePropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd, final Property propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final String ctxPersistentCollectionName,
			final String ctxPersistentFieldsPrefix) {
		super(pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						(propAnno.persistence().persistent() ?
								TargetType.PERSISTENT : TargetType.TRANSIENT)),
				(!propAnno.persistence().persistent() ? null :
					new ResourcePropertyPersistenceImpl(propAnno, pd.getName(),
							ctxPersistentFieldsPrefix)),
				propAnno.updateIfNull());

		// cannot be persistent if dynamic
		final ResourcePropertyPersistenceImpl persistenceDesc =
			this.getPersistence();
		if ((valueHandler.getLastInChain()
				instanceof DynamicResourcePropertyValueHandler)
				&& (persistenceDesc != null))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be persistent because it is dynamic.");

		// check correctness of persistent property definition
		this.checkPersistentPropertyDef(containerClass, pd, valueHandler,
				ctxPersistentCollectionName, persistenceDesc);
	}
}
