package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;

import org.bsworks.x2.resource.IdHandling;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.AccessRestriction;
import org.bsworks.x2.resource.annotations.IdProperty;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.util.StringUtils;


/**
 * Persistent resource record id property handler implementation.
 *
 * @author Lev Himmelfarb
 */
@AccessRestrictions({
	@AccessRestriction(ResourcePropertyAccess.UPDATE)
})
class IdPropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements IdPropertyHandler {

	/**
	 * Value handler.
	 */
	private final CanBeIdResourcePropertyValueHandler valueHandler;

	/**
	 * Id handling mode.
	 */
	private final IdHandling handling;


	/**
	 * Create new handler.
	 * 
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Resource property leaf value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class. May not be
	 * {@code null}, because an id can belong only to a persistent object.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null}.
	 */
	IdPropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd, final IdProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final CanBeIdResourcePropertyValueHandler leafValueHandler,
			final String ctxPersistentCollectionName,
			final String ctxPersistentFieldsPrefix) {
		super(containerClass, pd, valueHandler,
				new AccessChecker((IdPropertyHandlerImpl.class).getAnnotation(
						AccessRestrictions.class).value(),
						TargetType.PERSISTENT),
				new ResourcePropertyPersistenceImpl(
						ctxPersistentFieldsPrefix
							+ StringUtils.defaultIfEmpty(
									propAnno.persistentField(), pd.getName()),
						null, null, null, false),
				false, true);

		// container class must be persistent
		if (ctxPersistentCollectionName == null)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must belong to a persistent object.");

		// must be single-valued
		if (valueHandler.getCollectionDegree() > 0)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be a collection or a map.");

		// get specific properties
		this.valueHandler = leafValueHandler;
		this.handling = propAnno.handling();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public CanBeIdResourcePropertyValueHandler getValueHandler() {

		return this.valueHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public IdHandling getHandling() {

		return this.handling;
	}
}
