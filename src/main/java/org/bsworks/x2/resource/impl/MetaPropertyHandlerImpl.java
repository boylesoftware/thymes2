package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.Set;

import org.bsworks.x2.resource.MetaPropertyHandler;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.AccessRestriction;
import org.bsworks.x2.resource.annotations.MetaProperty;
import org.bsworks.x2.util.StringUtils;


/**
 * Persistent resource meta-property handler implementation.
 *
 * @author Lev Himmelfarb
 */
@AccessRestrictions({
	@AccessRestriction(ResourcePropertyAccess.UPDATE)
})
class MetaPropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements MetaPropertyHandler {

	/**
	 * Meta-property type.
	 */
	private final MetaPropertyType type;


	/**
	 * Create new handler.
	 *
	 * @param prsrcClasses All persistent resource classes. Used to verify that
	 * the container class is a persistent resource.
	 * @param containerClass Class that contains the property. Must be a
	 * persistent resource class.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Resource property leaf value handler.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null}.
	 */
	MetaPropertyHandlerImpl(final Set<Class<?>> prsrcClasses,
			final Class<?> containerClass, final PropertyDescriptor pd,
			final MetaProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final SimpleResourcePropertyValueHandler leafValueHandler,
			final String ctxPersistentFieldsPrefix) {
		super(pd, valueHandler,
				new AccessChecker((MetaPropertyHandlerImpl.class).getAnnotation(
						AccessRestrictions.class).value(), true, false),
				new ResourcePropertyPersistenceImpl(
						ctxPersistentFieldsPrefix
							+ StringUtils.defaultIfEmpty(
									propAnno.persistentField(), pd.getName()),
						null, null, null, false),
				false);

		// container class must be a persistent resource
		if (!prsrcClasses.contains(containerClass))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must belong to a persistent resource.");

		// must be single-valued
		if (valueHandler.getCollectionDegree() > 0)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be a collection or a map.");

		// check the value type
		final ResourcePropertyValueType valueType = leafValueHandler.getType();
		boolean invalid = false;
		switch (propAnno.type()) {
		case VERSION:
			invalid = ((valueType != ResourcePropertyValueType.INTEGER)
					&& (valueType != ResourcePropertyValueType.LONG));
			break;
		case CREATION_ACTOR:
		case MODIFICATION_ACTOR:
			invalid = (valueType != ResourcePropertyValueType.STRING);
			break;
		case CREATION_TIMESTAMP:
		case MODIFICATION_TIMESTAMP:
			invalid = (valueType != ResourcePropertyValueType.DATE);
			break;
		default:
		}
		if (invalid)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " has invalid type.");

		// get specific properties
		this.type = propAnno.type();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public MetaPropertyType getType() {

		return this.type;
	}
}
