package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.annotations.PersistentResource;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.util.StringUtils;


/**
 * Nested object resource property handler implementation.
 *
 * @author Lev Himmelfarb
 */
class ObjectPropertyHandlerImpl
	extends AbstractResourcePropertyHandlerImpl
	implements ObjectPropertyHandler {

	/**
	 * Owning persistent resource class, or {@code null}.
	 */
	private final Class<?> owningPersistentResourceClass;

	/**
	 * Tells if borrowed nested object property.
	 */
	private final boolean borrowed;

	/**
	 * Object properties container.
	 */
	private final ResourcePropertiesContainerImpl<?> propsContainer;

	/**
	 * Object properties.
	 */
	private final Map<String, ? extends ResourcePropertyHandler> properties;

	/**
	 * Simple properties.
	 */
	private final Collection<SimplePropertyHandlerImpl> simpleProperties;

	/**
	 * Nested object properties.
	 */
	private final Collection<? extends ObjectPropertyHandler> objectProperties;

	/**
	 * Reference properties.
	 */
	private final Collection<RefPropertyHandlerImpl> refProperties;

	/**
	 * Concrete type name, or {@code null} if not a concrete type handler.
	 */
	private final String concreteTypeName;


	/**
	 * Create new handler.
	 *
	 * @param prsrcClasses All persistent resource classes.
	 * @param prsrcClass Containing persistent resource class, or {@code null}
	 * if not part of a persistent resource.
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
	ObjectPropertyHandlerImpl(final Set<Class<?>> prsrcClasses,
			final Class<?> prsrcClass, final Class<?> containerClass,
			final PropertyDescriptor pd, final Property propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final ObjectResourcePropertyValueHandler leafValueHandler,
			final String ctxPersistentCollectionName,
			final String ctxPersistentFieldsPrefix) {
		this(prsrcClasses, prsrcClass, containerClass, pd, propAnno,
				valueHandler, leafValueHandler, ctxPersistentCollectionName,
				(!propAnno.persistence().persistent() ? null :
					new ResourcePropertyPersistenceImpl(propAnno, pd.getName(),
							ctxPersistentFieldsPrefix)),
				null);
	}

	/**
	 * Create new handler.
	 *
	 * @param prsrcClasses All persistent resource classes.
	 * @param prsrcClass Containing persistent resource class, or {@code null}
	 * if not part of a persistent resource.
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param valueHandler Resource property value handler.
	 * @param leafValueHandler Resource property leaf value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class, or {@code null} if
	 * the container class is transient. Used to check that persistent property
	 * belongs to a persistent container.
	 * @param persistenceDesc Property persistence descriptor, or {@code null}
	 * if transient.
	 * @param concreteTypeName Concrete type name, or {@code null} if not a
	 * concrete value of a polymorphic type.
	 */
	private ObjectPropertyHandlerImpl(final Set<Class<?>> prsrcClasses,
			final Class<?> prsrcClass, final Class<?> containerClass,
			final PropertyDescriptor pd, final Property propAnno,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final ObjectResourcePropertyValueHandler leafValueHandler,
			final String ctxPersistentCollectionName,
			final ResourcePropertyPersistenceImpl persistenceDesc,
			final String concreteTypeName) {
		super(containerClass, pd, valueHandler,
				new AccessChecker(propAnno.accessRestrictions(),
						((propAnno.ownedBy() != PersistentResource.class)
								&& (propAnno.ownedBy() != prsrcClass) ?
							TargetType.BORROWED :
						(persistenceDesc != null ?
							TargetType.PERSISTENT : TargetType.TRANSIENT))),
				persistenceDesc, propAnno.updateIfNull(),
				propAnno.fetchedByDefault());

		// determine and check owning persistent resource class
		this.owningPersistentResourceClass =
			(propAnno.ownedBy() != PersistentResource.class ?
					propAnno.ownedBy() : prsrcClass);
		if ((this.owningPersistentResourceClass != null)
				&& !prsrcClasses.contains(this.owningPersistentResourceClass))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " has owning persistent resource attribute that does not"
					+ " point at a persistent resource.");
		if ((this.owningPersistentResourceClass != null)
				&& (persistenceDesc == null))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " is owned by a  persistent resource and therefore must"
					+ " be persistent.");
		this.borrowed =
			((this.owningPersistentResourceClass != null)
					&& (this.owningPersistentResourceClass != prsrcClass));

		// check correctness of persistent property definition
		this.checkPersistentPropertyDef(containerClass, pd, valueHandler,
				ctxPersistentCollectionName, persistenceDesc);

		// get the properties
		this.propsContainer = leafValueHandler.getResourceProperties();

		// get polymorphic value type property is any
		final TypePropertyHandlerImpl typePropHandler =
			this.propsContainer.getTypeProperty();

		// create property handlers for polymorphic value types
		final Collection<ConcreteObjectPropertyHandler> valueTypeHandlers;
		final Collection<ResourcePropertiesContainerImpl<?>> valueTypeProps =
			leafValueHandler.getValueTypes();
		if (valueTypeProps != null) {

			// check that we have type property
			if (typePropHandler == null)
				throw new IllegalArgumentException("Class "
						+ this.propsContainer.getContainerClass().getName()
						+ " used in polymorphic property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " must have a type property.");

			// create handlers for concrete types
			valueTypeHandlers = new ArrayList<>(valueTypeProps.size());
			for (final ResourcePropertiesContainerImpl<?> vProps :
					valueTypeProps) {
				ObjectResourcePropertyValueHandler vValueHandler =
					new ObjectResourcePropertyValueHandler(vProps, null);
				final ResourcePropertyPersistenceImpl vPersistenceDesc;
				if (persistenceDesc != null) {
					final String valuesPersistentCollectionName =
						vProps.getPersistentCollectionName();
					final String vPersistentCollectionName;
					if (ctxPersistentCollectionName != null) {
						if (valuesPersistentCollectionName.equals(
								ctxPersistentCollectionName)
								|| valuesPersistentCollectionName.equals(
										persistenceDesc.getCollectionName()))
							vPersistentCollectionName = null;
						else
							vPersistentCollectionName =
								valuesPersistentCollectionName;
					} else { // should be null
						vPersistentCollectionName =
							valuesPersistentCollectionName;
					}
					if (vPersistentCollectionName == null) {
						if (typePropHandler.getPersistence() == null)
							throw new IllegalArgumentException("Type property "
									+ typePropHandler.getName() + " of "
									+ this.propsContainer.getContainerClass()
										.getName()
									+ " must be persistent.");
					}
					vPersistenceDesc = new ResourcePropertyPersistenceImpl(
							persistenceDesc.getFieldName(),
							vPersistentCollectionName,
							StringUtils.defaultIfEmpty(
									vProps.getParentIdPersistentFieldName(),
									persistenceDesc.getParentIdFieldName()),
							persistenceDesc.getKeyFieldName(),
							true);
				} else {
					vPersistenceDesc = null;
				}
				final String typeName = vProps.getConcreteTypeName();
				valueTypeHandlers.add(new ConcreteObjectPropertyHandler(
						new ObjectPropertyHandlerImpl(prsrcClasses, prsrcClass,
								containerClass, pd, propAnno, valueHandler,
								vValueHandler, ctxPersistentCollectionName,
								vPersistenceDesc, typeName),
						typePropHandler, typeName));
			}
		} else {
			valueTypeHandlers = null;
		}

		// save property collections depending on polymorphic or not
		if (valueTypeHandlers != null) {

			final Map<String, ResourcePropertyHandler> properties =
				new HashMap<>();
			final IdPropertyHandlerImpl idProp =
				this.propsContainer.getIdProperty();
			if (idProp != null)
				properties.put(idProp.getName(), idProp);
			final TypePropertyHandlerImpl typeProp =
				this.propsContainer.getTypeProperty();
			properties.put(typeProp.getName(), typeProp);
			for (final ConcreteObjectPropertyHandler vt : valueTypeHandlers)
				properties.put(vt.getName(), vt);
			this.properties = Collections.unmodifiableMap(properties);

			this.simpleProperties = Collections.emptyList();
			this.objectProperties = valueTypeHandlers;
			this.refProperties = Collections.emptyList();

		} else { // not polymorphic

			this.properties = this.propsContainer.getProperties();
			this.simpleProperties = this.propsContainer.getSimpleProperties();
			this.objectProperties = this.propsContainer.getObjectProperties();
			this.refProperties = this.propsContainer.getRefProperties();
		}

		// save concrete type name
		this.concreteTypeName = concreteTypeName;
	}


	/**
	 * For a concrete type handler, get the type name.
	 *
	 * @return The type name, or {@code null} if not a concrete type handler.
	 */
	String getConcreteTypeName() {

		return this.concreteTypeName;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getOwningPersistentResourceClass() {

		return this.owningPersistentResourceClass;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isBorrowed() {

		return this.borrowed;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getObjectClass() {

		return this.propsContainer.getContainerClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPersistentPropertyChain(
			final String propPath) {

		return this.propsContainer.getPersistentPropertyChain(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Map<String, ? extends ResourcePropertyHandler> getProperties() {

		return this.properties;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public IdPropertyHandlerImpl getIdProperty() {

		return this.propsContainer.getIdProperty();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public TypePropertyHandlerImpl getTypeProperty() {

		return this.propsContainer.getTypeProperty();
	}

	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean isConcreteType() {

		return false;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<SimplePropertyHandlerImpl> getSimpleProperties() {

		return this.simpleProperties;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<? extends ObjectPropertyHandler> getObjectProperties() {

		return this.objectProperties;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<RefPropertyHandlerImpl> getRefProperties() {

		return this.refProperties;
	}
}
