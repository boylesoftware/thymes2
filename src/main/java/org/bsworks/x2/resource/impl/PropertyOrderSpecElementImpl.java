package org.bsworks.x2.resource.impl;

import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.PropertyOrderSpecElement;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.SortDirection;


/**
 * Property order specification element implementation.
 *
 * @author Lev Himmelfarb
 */
class PropertyOrderSpecElementImpl
	implements PropertyOrderSpecElement {

	/**
	 * Empty value transformation function parameters.
	 */
	private static final Object[] NO_PARAMS = new Object[0];


	/**
	 * Sort direction.
	 */
	private final SortDirection dir;

	/**
	 * Property path.
	 */
	private final String propPath;

	/**
	 * Property value transformation function.
	 */
	private final PropertyValueFunction func;

	/**
	 * Transformation function parameters.
	 */
	private final Object[] funcParams;

	/**
	 * Property chain.
	 */
	private final Deque<? extends ResourcePropertyHandler> propChain;


	/**
	 * Create new element.
	 *
	 * @param dir Sort direction.
	 * @param prsrcHandler Top persistent resource handler.
	 * @param propPath Property path.
	 * @param prsrcClasses Set, to which to add any participating persistent
	 * resource classes.
	 * @param func Value transformation function.
	 * @param funcParams Value transformation function parameters. May be
	 * {@code null} for no parameters.
	 *
	 * @throws InvalidSpecificationException If the specification is invalid.
	 */
	PropertyOrderSpecElementImpl(final SortDirection dir,
			final PersistentResourceHandlerImpl<?> prsrcHandler,
			final String propPath, final PropertyValueFunction func,
			final Object[] funcParams, final Set<Class<?>> prsrcClasses) {

		this.dir = dir;

		final boolean refId = propPath.endsWith("/id");
		if (refId)
			this.propPath =
				propPath.substring(0, propPath.length() - "/id".length());
		else
			this.propPath = propPath;

		this.func = func;
		this.funcParams = (funcParams != null ? funcParams : NO_PARAMS);
		final Class<?>[] funcParamTypes = this.func.paramTypes();
		if (funcParamTypes.length != this.funcParams.length)
			throw new InvalidSpecificationException("Invalid \"" + this.func
					+ "\" value transformation function parameters number.");
		for (int i = 0; i < this.funcParams.length; i++)
			if ((this.funcParams[i] == null)
					|| !this.funcParams[i].getClass().equals(funcParamTypes[i]))
				throw new InvalidSpecificationException("Invalid \"" + this.func
						+ "\" value transformation function parameter " + i
						+ " type.");

		this.propChain = prsrcHandler.getPersistentPropertyChain(this.propPath);

		final ResourcePropertyHandler lastProp = this.propChain.getLast();

		if (lastProp instanceof AggregatePropertyHandler)
			throw new InvalidSpecificationException(
					"Cannot order by aggregate property.");

		for (final Iterator<? extends ResourcePropertyHandler> i =
				this.propChain.iterator(); i.hasNext();) {
			final ResourcePropertyHandler prop = i.next();

			if (!prop.isSingleValued())
				throw new InvalidSpecificationException(
						"Cannot order by collection property.");

			if ((prop instanceof RefPropertyHandler) && i.hasNext())
				prsrcClasses.add(((RefPropertyHandler) prop)
						.getReferredResourceClass());
			else if (prop instanceof DependentRefPropertyHandler)
				prsrcClasses.add(((DependentRefPropertyHandler) prop)
						.getReferredResourceClass());
		}

		if (lastProp instanceof ObjectPropertyHandler)
			throw new InvalidSpecificationException(
					"Cannot order by nested object property.");

		if ((lastProp instanceof RefPropertyHandler)
				|| (lastProp instanceof DependentRefPropertyHandler)) {
			if (!refId)
				throw new InvalidSpecificationException(
						"Cannot order by reference property.");
		} else {
			if (refId)
				throw new InvalidSpecificationException("Cannot use /id"
						+ " qualifier unless the property is a reference.");
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public SortDirection getSortDirection() {

		return this.dir;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getPropertyPath() {

		return this.propPath;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertyValueFunction getValueFunction() {

		return this.func;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object[] getValueFunctionParams() {

		return this.funcParams;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPropertyChain() {

		return this.propChain;
	}
}
