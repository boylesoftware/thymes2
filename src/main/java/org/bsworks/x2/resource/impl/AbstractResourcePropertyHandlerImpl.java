package org.bsworks.x2.resource.impl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Common parent for various resource property handler implementations.
 *
 * @author Lev Himmelfarb
 */
abstract class AbstractResourcePropertyHandlerImpl
	implements ResourcePropertyHandler {

	/**
	 * Property name.
	 */
	private final String name;

	/**
	 * Property access checker.
	 */
	private final AccessChecker accessChecker;

	/**
	 * Property getter.
	 */
	private final Method getter;

	/**
	 * Property setter.
	 */
	private final Method setter;

	/**
	 * Property value handler.
	 */
	private final AbstractResourcePropertyValueHandlerImpl valueHandler;

	/**
	 * Map key value handler, if applicable.
	 */
	private final CanBeMapKeyResourcePropertyValueHandler keyValueHandler;

	/**
	 * Property persistence descriptor, or {@code null} if not persistent.
	 */
	private final ResourcePropertyPersistenceImpl persistence;

	/**
	 * Tells if the property needs to be set to {@code null} if it is
	 * {@code null} in the incoming data.
	 */
	private final boolean updateIfNull;

	/**
	 * Tells if fetched by default.
	 */
	private final boolean fetchedByDefault;


	/**
	 * Create new handler.
	 *
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param valueHandler Property value handler.
	 * @param accessChecker Property access checker.
	 * @param persistence Property persistence descriptor, or {@code null} if
	 * not persistent.
	 * @param updateIfNull {@code true} if the property needs to be set to
	 * {@code null} if it is {@code null} in the incoming data.
	 * @param fetchedByDefault {@code true} if the property needs to be fetched
	 * from persistent storage by default, {@code false} if needs to be fetched
	 * only if explicitly requested by the properties fetch specification.
	 */
	protected AbstractResourcePropertyHandlerImpl(final Class<?> containerClass,
			final PropertyDescriptor pd,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final AccessChecker accessChecker,
			final ResourcePropertyPersistenceImpl persistence,
			final boolean updateIfNull, final boolean fetchedByDefault) {

		this.name = pd.getName();
		this.accessChecker = accessChecker;
		this.getter = pd.getReadMethod();
		this.setter = pd.getWriteMethod();
		this.valueHandler = valueHandler;
		this.persistence = persistence;
		this.updateIfNull = updateIfNull;
		this.fetchedByDefault = fetchedByDefault;

		if (valueHandler instanceof MapResourcePropertyValueHandler)
			this.keyValueHandler =
				((MapResourcePropertyValueHandler) valueHandler)
					.getKeyValueHandler();
		else
			this.keyValueHandler = null;

		if (!this.fetchedByDefault
			&& (valueHandler instanceof SimpleResourcePropertyValueHandler)
			&& ((SimpleResourcePropertyValueHandler) valueHandler)
				.isPrimitive())
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " is not fetched by default and therefore may not be"
					+ " primitive.");
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final String getName() {

		return this.name;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final boolean isAllowed(final ResourcePropertyAccess access,
			final Actor actor) {

		return this.accessChecker.isAllowed(access, actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public AbstractResourcePropertyValueHandlerImpl getValueHandler() {

		return this.valueHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public CanBeMapKeyResourcePropertyValueHandler getKeyValueHandler() {

		return this.keyValueHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final boolean isGettable() {

		return (this.getter != null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Object getValue(final Object obj) {

		if (this.getter == null)
			throw new UnsupportedOperationException(
					"The property " + this.name + " is not readable.");

		try {
			return this.getter.invoke(obj);
		} catch (final IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Error getting property value.", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final boolean isSettable() {

		return (this.setter != null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final void setValue(final Object obj, final Object val) {

		if (this.setter == null)
			throw new UnsupportedOperationException(
					"The property " + this.name + " is not writable.");

		try {
			this.setter.invoke(obj, val);
		} catch (final IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Error setting property value.", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final boolean isSingleValued() {

		return (this.valueHandler.getCollectionDegree() == 0);
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
	public boolean updateIfNull() {

		return this.updateIfNull;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isFetchedByDefault() {

		return this.fetchedByDefault;
	}


	/**
	 * Check persistent property definition correctness.
	 *
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param valueHandler Resource property value handler.
	 * @param ctxPersistentCollectionName Name of the persistent collection used
	 * to store persistent properties in the container class, or {@code null} if
	 * the container class is transient. Used to check that persistent property
	 * belongs to a persistent container.
	 * @param persistenceDesc Property persistence descriptor, or {@code null}
	 * if transient.
	 *
	 * @throws IllegalArgumentException If incorrect definition.
	 */
	protected final void checkPersistentPropertyDef(
			final Class<?> containerClass,
			final PropertyDescriptor pd,
			final AbstractResourcePropertyValueHandlerImpl valueHandler,
			final String ctxPersistentCollectionName,
			final ResourcePropertyPersistenceImpl persistenceDesc) {

		// no check if transient
		if (persistenceDesc == null)
			return;

		// container class must be persistent
		if (ctxPersistentCollectionName == null)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must belong to a persistent object.");

		// check the collection degree
		final int collectionDegree = valueHandler.getCollectionDegree();
		if (collectionDegree > 1)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " has more than one collection degree.");

		// if not embedded, must have parent id field
		final boolean embedded =
			(persistenceDesc.getCollectionName() == null);
		if (!embedded && (persistenceDesc.getParentIdFieldName() == null))
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " must specify parent id persistent field.");

		// a not embedded map must have a key
		if ((valueHandler.getType() == ResourcePropertyValueType.MAP)
				&& !embedded) {
			if (persistenceDesc.getKeyFieldName() == null)
				throw new IllegalArgumentException("Property "
						+ pd.getName() + " of " + containerClass.getName()
						+ " must specify map key persistent field.");
		} else { // not a map or embedded, no need for a key
			if (persistenceDesc.getKeyFieldName() != null)
				throw new IllegalArgumentException("Property "
						+ pd.getName() + " of " + containerClass.getName()
						+ " is not a map or is embedded, so it cannot"
						+ " specify map key persistent field.");
		}
	}
}
