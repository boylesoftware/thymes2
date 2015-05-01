package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Set;

import org.bsworks.x2.resource.annotations.AggregateProperty;
import org.bsworks.x2.resource.annotations.DependentRefProperty;
import org.bsworks.x2.resource.annotations.IdProperty;
import org.bsworks.x2.resource.annotations.MetaProperty;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.resource.annotations.TypeProperty;


/**
 * Resource property handler factory.
 *
 * @author Lev Himmelfarb
 */
final class ResourcePropertyHandlerFactory {

	/**
	 * The factory method is static.
	 */
	private ResourcePropertyHandlerFactory() {}


	/**
	 * Create resource property handler.
	 *
	 * @param prsrcClasses All persistent resource classes.
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Property value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class, or {@code null} if
	 * no persistent collection in the context.
	 * @param ctxPersistentFieldsPrefix If persistent collection is specified,
	 * prefix for any persistent fields in the container class. Empty string for
	 * no prefix. Ignored if {@code persistentCollectionName} argument is
	 * {@code null}.
	 * @param parentPersistentFieldsPrefix Prefix to use if the property is a
	 * polymorphic object type property. This is relevant for embedded
	 * polymorphic objects so that the objects themselves can have different
	 * prefixes, but the type persistent field is shared, because it belongs to
	 * the parent.
	 *
	 * @return Resource property handler.
	 *
	 * @throws IllegalArgumentException If something is wrong with the resource
	 * property definition.
	 */
	static AbstractResourcePropertyHandlerImpl createHandler(
			final Set<Class<?>> prsrcClasses, final Class<?> containerClass,
			final PropertyDescriptor pd, final Annotation propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final String ctxPersistentCollectionName,
			final String ctxPersistentFieldsPrefix,
			final String parentPersistentFieldsPrefix) {

		final AbstractResourcePropertyValueHandlerImpl leafValueHandler =
			valueHandler.getLastInChain();
		if (propAnno instanceof IdProperty) {
			if (leafValueHandler instanceof CanBeIdResourcePropertyValueHandler)
				return new IdPropertyHandlerImpl(containerClass, pd,
						(IdProperty) propAnno,
						valueHandler,
						(CanBeIdResourcePropertyValueHandler) leafValueHandler,
						ctxPersistentCollectionName, ctxPersistentFieldsPrefix);
		} else if (propAnno instanceof MetaProperty) {
			if (leafValueHandler instanceof SimpleResourcePropertyValueHandler)
				return new MetaPropertyHandlerImpl(prsrcClasses,
						containerClass, pd, (MetaProperty) propAnno,
						valueHandler,
						(SimpleResourcePropertyValueHandler) leafValueHandler,
						ctxPersistentFieldsPrefix);
		} else if (propAnno instanceof TypeProperty) {
			if (leafValueHandler instanceof CanBeIdResourcePropertyValueHandler)
				return new TypePropertyHandlerImpl(containerClass, pd,
						(TypeProperty) propAnno, valueHandler,
						(CanBeIdResourcePropertyValueHandler) leafValueHandler,
						parentPersistentFieldsPrefix);
		} else if (propAnno instanceof DependentRefProperty) {
			if (leafValueHandler instanceof RefResourcePropertyValueHandler)
				return new DependentRefPropertyHandlerImpl(prsrcClasses,
						containerClass, pd, (DependentRefProperty) propAnno,
						valueHandler,
						(RefResourcePropertyValueHandler) leafValueHandler);
		} else if (propAnno instanceof AggregateProperty) {
			if (leafValueHandler instanceof SimpleResourcePropertyValueHandler)
				return new AggregatePropertyHandlerImpl(containerClass, pd,
						(AggregateProperty) propAnno, valueHandler,
						(SimpleResourcePropertyValueHandler) leafValueHandler);
		} else if (propAnno instanceof Property) {
			if (leafValueHandler instanceof ObjectResourcePropertyValueHandler)
				return new ObjectPropertyHandlerImpl(containerClass, pd,
						(Property) propAnno, valueHandler,
						(ObjectResourcePropertyValueHandler) leafValueHandler,
						ctxPersistentCollectionName, ctxPersistentFieldsPrefix);
			else if (leafValueHandler
					instanceof RefResourcePropertyValueHandler)
				return new RefPropertyHandlerImpl(containerClass, pd,
						(Property) propAnno, valueHandler,
						(RefResourcePropertyValueHandler) leafValueHandler,
						ctxPersistentCollectionName, ctxPersistentFieldsPrefix);
			else if ((leafValueHandler
						instanceof SimpleResourcePropertyValueHandler)
						|| (leafValueHandler
								instanceof DynamicResourcePropertyValueHandler))
				return new SimplePropertyHandlerImpl(containerClass, pd,
						(Property) propAnno, valueHandler,
						ctxPersistentCollectionName, ctxPersistentFieldsPrefix);
		}

		throw new IllegalArgumentException("Property " + pd.getName()
				+ " of " + containerClass.getName()
				+ " has annotation that does not match its type.");
	}
}
