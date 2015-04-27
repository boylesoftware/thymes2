package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;

import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;


/**
 * Persistent resource reference property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class RefPropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements RefPropertyHandler {

	/**
	 * Referred resource class. For a transient reference, may be a superclass
	 * or {@code null} for a wildcard target.
	 */
	private final Class<?> referredResourceClass;


	/**
	 * Create new handler.
	 *
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Resource property leaf value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class, or {@code null} if
	 * the container class is transient. Used to check that persistent property
	 * belongs to a persistent container.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null} if persistent property.
	 * Otherwise, ignored.
	 */
	RefPropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd, final Property propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final RefResourcePropertyValueHandler leafValueHandler,
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

		// check correctness of persistent property definition
		final ResourcePropertyPersistenceImpl persistenceDesc =
			this.getPersistence();
		this.checkPersistentPropertyDef(containerClass, pd, valueHandler,
				ctxPersistentCollectionName, persistenceDesc);
		if (persistenceDesc != null) {

			// check target is not a wildcard
			if (leafValueHandler.isWildcard())
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " cannot have a wildcard target.");
		}

		// get specific properties
		this.referredResourceClass = leafValueHandler.getRefTargetClass();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getReferredResourceClass() {

		return this.referredResourceClass;
	}
}
