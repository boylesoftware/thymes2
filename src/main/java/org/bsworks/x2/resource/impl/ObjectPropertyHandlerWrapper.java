package org.bsworks.x2.resource.impl;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyPersistence;
import org.bsworks.x2.resource.ResourcePropertyValueHandler;
import org.bsworks.x2.resource.SimplePropertyHandler;
import org.bsworks.x2.resource.TypePropertyHandler;


/**
 * Delegating object property handler wrapper.
 *
 * @author Lev Himmelfarb
 */
abstract class ObjectPropertyHandlerWrapper
	implements ObjectPropertyHandler {

	/**
	 * Wrapped property handler.
	 */
	protected final ObjectPropertyHandler propHandler;


	/**
	 * Create new wrapper.
	 *
	 * @param propHandler Property handler to wrap.
	 */
	protected ObjectPropertyHandlerWrapper(
			final ObjectPropertyHandler propHandler) {

		this.propHandler = propHandler;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getName() {

		return this.propHandler.getName();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isAllowed(final ResourcePropertyAccess access,
			final Actor actor) {

		return this.propHandler.isAllowed(access, actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourcePropertyValueHandler getValueHandler() {

		return this.propHandler.getValueHandler();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourcePropertyValueHandler getKeyValueHandler() {

		return this.propHandler.getKeyValueHandler();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isGettable() {

		return this.propHandler.isGettable();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object getValue(final Object obj) {

		return this.propHandler.getValue(obj);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isSettable() {

		return this.propHandler.isSettable();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void setValue(final Object obj, final Object val) {

		this.propHandler.setValue(obj, val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isSingleValued() {

		return this.propHandler.isSingleValued();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public ResourcePropertyPersistence getPersistence() {

		return this.propHandler.getPersistence();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean updateIfNull() {

		return this.propHandler.updateIfNull();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isFetchedByDefault() {

		return this.propHandler.isFetchedByDefault();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPersistentPropertyChain(
			final String propPath) {

		return this.propHandler.getPersistentPropertyChain(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Map<String, ? extends ResourcePropertyHandler> getProperties() {

		return this.propHandler.getProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public IdPropertyHandler getIdProperty() {

		return this.propHandler.getIdProperty();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<? extends SimplePropertyHandler> getSimpleProperties() {

		return this.propHandler.getSimpleProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<? extends ObjectPropertyHandler> getObjectProperties() {

		return this.propHandler.getObjectProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<? extends RefPropertyHandler> getRefProperties() {

		return this.propHandler.getRefProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getOwningPersistentResourceClass() {

		return this.propHandler.getOwningPersistentResourceClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isBorrowed() {

		return this.propHandler.isBorrowed();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getObjectClass() {

		return this.propHandler.getObjectClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public TypePropertyHandler getTypeProperty() {

		return this.propHandler.getTypeProperty();
	}
}
