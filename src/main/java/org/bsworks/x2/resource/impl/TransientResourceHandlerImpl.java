package org.bsworks.x2.resource.impl;

import java.util.Set;

import org.bsworks.x2.resource.IdPropertyHandler;


/**
 * Transient resource handler implementation.
 *
 * @param <R> Handled resource type.
 *
 * @author Lev Himmelfarb
 */
class TransientResourceHandlerImpl<R>
	extends AbstractResourceHandlerImpl<R> {

	/**
	 * Create transient application resource handler.
	 *
	 * @param resources Reference to the resources manager.
	 * @param prsrcClasses All persistent resource classes.
	 * @param rsrcClass Resource class.
	 *
	 * @throws IllegalArgumentException If something is wrong with the specified
	 * resource class.
	 */
	TransientResourceHandlerImpl(final ResourcesImpl resources,
			final Set<Class<?>> prsrcClasses, final Class<R> rsrcClass) {
		super(new ResourcePropertiesContainerImpl<>(resources, prsrcClasses,
						rsrcClass, null, null, null, null, null, null));
	}


	/**
	 * Returns {@code null} as transient resources do not have ids.
	 */
	@Override
	public IdPropertyHandler getIdProperty() {

		return null;
	}
}
