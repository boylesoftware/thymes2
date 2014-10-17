package org.bsworks.x2.resource.impl;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;

import org.bsworks.x2.resource.ResourceHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Common parent for transient and persistent resource handler implementations.
 *
 * @param <R> Handled resource type.
 *
 * @author Lev Himmelfarb
 */
abstract class AbstractResourceHandlerImpl<R>
	implements ResourceHandler<R> {

	/**
	 * Handled resource class.
	 */
	private final Class<R> rsrcClass;

	/**
	 * Resource properties.
	 */
	protected final ResourcePropertiesContainerImpl<R> props;

	/**
	 * Whole resource value handler.
	 */
	private final ObjectResourcePropertyValueHandler valueHandler;


	/**
	 * Create resource handler.
	 *
	 * @param rsrcProps Handled resource class properties.
	 */
	protected AbstractResourceHandlerImpl(
			final ResourcePropertiesContainerImpl<R> rsrcProps) {

		this.rsrcClass = rsrcProps.getContainerClass();
		this.props = rsrcProps;
		this.valueHandler =
			new ObjectResourcePropertyValueHandler(this.props,
					null /* never polymorphic */);
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Class<R> getResourceClass() {

		return this.rsrcClass;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final ObjectResourcePropertyValueHandler getResourceValueHandler() {

		return this.valueHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPersistentPropertyChain(
			final String propPath) {

		return this.props.getPersistentPropertyChain(propPath);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Map<String, AbstractResourcePropertyHandlerImpl>
	getProperties() {

		return this.props.getProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Collection<SimplePropertyHandlerImpl> getSimpleProperties() {

		return this.props.getSimpleProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Collection<ObjectPropertyHandlerImpl> getObjectProperties() {

		return this.props.getObjectProperties();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public final Collection<RefPropertyHandlerImpl> getRefProperties() {

		return this.props.getRefProperties();
	}
}
