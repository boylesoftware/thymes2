package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;

import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.TypePropertyHandler;
import org.bsworks.x2.resource.annotations.AccessRestriction;
import org.bsworks.x2.resource.annotations.TypeProperty;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.util.StringUtils;


/**
 * Polymorphic object type property handler implementation.
 *
 * @author Lev Himmelfarb
 */
@AccessRestrictions({
	@AccessRestriction(ResourcePropertyAccess.SEE),
	@AccessRestriction(ResourcePropertyAccess.SUBMIT),
	@AccessRestriction(ResourcePropertyAccess.UPDATE)
})
class TypePropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements TypePropertyHandler {

	/**
	 * Value handler.
	 */
	private final CanBeIdResourcePropertyValueHandler valueHandler;


	/**
	 * Create new handler.
	 * 
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Resource property leaf value handler.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null}.
	 */
	TypePropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd, final TypeProperty propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final CanBeIdResourcePropertyValueHandler leafValueHandler,
			final String ctxPersistentFieldsPrefix) {
		super(containerClass, pd, valueHandler,
				new AccessChecker(
						(TypePropertyHandlerImpl.class).getAnnotation(
								AccessRestrictions.class).value(),
						(propAnno.persistent() ?
								TargetType.PERSISTENT : TargetType.TRANSIENT)),
				(!propAnno.persistent() ? null :
					new ResourcePropertyPersistenceImpl(
							ctxPersistentFieldsPrefix
								+ StringUtils.defaultIfEmpty(
										propAnno.persistentField(),
										pd.getName()),
							null, null, null, false)),
				false, true);

		// must be single-valued
		if (valueHandler.getCollectionDegree() > 0)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " cannot be a collection or a map.");

		// get specific properties
		this.valueHandler = leafValueHandler;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public CanBeIdResourcePropertyValueHandler getValueHandler() {

		return this.valueHandler;
	}
}
