package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterCondition;
import org.bsworks.x2.resource.FilterConditionOperandType;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.InvalidSpecificationException;
import org.bsworks.x2.resource.PropertyValueFunction;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.ResourcePropertyValueHandler;


/**
 * Filter condition implementation.
 *
 * @author Lev Himmelfarb
 */
class FilterConditionImpl
	implements FilterCondition {

	/**
	 * Condition type.
	 */
	private final FilterConditionType type;

	/**
	 * Tells if negated.
	 */
	private final boolean negated;

	/**
	 * Property path.
	 */
	private final String propPath;

	/**
	 * Property value type.
	 */
	private final FilterConditionOperandType propValueType;

	/**
	 * Property value transformation function.
	 */
	private final PropertyValueFunction valueFunc;

	/**
	 * Property value transformation function parameters.
	 */
	private final Object[] valueFuncParams;

	/**
	 * Property chain.
	 */
	private final Deque<? extends ResourcePropertyHandler> propChain;

	/**
	 * Condition operands.
	 */
	private final Collection<FilterConditionOperandImpl> operands;


	/**
	 * Create new condition.
	 *
	 * @param resources Application resources manager.
	 * @param type Condition type.
	 * @param valueFunc Property value transformation function.
	 * @param valueFuncParams Property value transformation function parameters.
	 * May be {@code null} if the function takes no parameters.
	 * @param negated {@code true} if negated.
	 * @param prsrcHandler Root persistent resource handler.
	 * @param propPath Property path.
	 * @param operands Condition operands. Cannot be {@code null}, but can be
	 * empty.
	 * @param prsrcClasses Set, to which to add any participating persistent
	 * resource classes.
	 *
	 * @throws InvalidSpecificationException If condition specification is
	 * invalid.
	 */
	FilterConditionImpl(final ResourcesImpl resources,
			final FilterConditionType type,
			final PropertyValueFunction valueFunc,
			final Object[] valueFuncParams, final boolean negated,
			final PersistentResourceHandlerImpl<?> prsrcHandler,
			final String propPath, final Object[] operands,
			final Set<Class<?>> prsrcClasses) {

		this.type = type;
		this.valueFunc = valueFunc;
		this.valueFuncParams = (valueFuncParams != null ?
				valueFuncParams : new Object[0]);
		this.negated = negated;

		// get property path and tested value operand type
		if (propPath.endsWith("/id")) {
			this.propPath = propPath.substring(0,
					propPath.length() - "/id".length());
			this.propValueType = FilterConditionOperandType.ID;
		} else if (propPath.endsWith("/key")) {
			this.propPath = propPath.substring(0,
					propPath.length() - "/key".length());
			this.propValueType = FilterConditionOperandType.KEY;
		} else {
			this.propPath = propPath;
			this.propValueType = FilterConditionOperandType.VALUE;
		}

		// get property path chain
		this.propChain = prsrcHandler.getPersistentPropertyChain(this.propPath);

		// get handler of the property at the end of the chain
		final AbstractResourcePropertyHandlerImpl propHandler =
			(AbstractResourcePropertyHandlerImpl) this.propChain.getLast();

		// cannot use aggregates in filters
		if (propHandler instanceof AggregatePropertyHandler)
			throw new InvalidSpecificationException(
					"Cannot use aggregate properties in filters.");

		// get property value handlers (top and leaf)
		final AbstractResourcePropertyValueHandlerImpl propTopValueHandler =
			propHandler.getValueHandler();
		final AbstractResourcePropertyValueHandlerImpl propLeafValueHandler =
			propHandler.getValueHandler().getLastInChain();

		// determine if the test is a presence check, validate operands number
		final boolean presenceCheck = ((this.type == FilterConditionType.EMPTY)
				|| (this.type == FilterConditionType.NOT_EMPTY));
		if (presenceCheck && (operands.length > 0))
			throw new InvalidSpecificationException("This type of filter"
					+ " condition does not use operands.");
		if (!presenceCheck && (operands.length == 0))
			throw new InvalidSpecificationException("This type of filter"
					+ " condition requires at least one operand.");

		// get value handler for the operand
		final ResourcePropertyValueHandler opValueHandler;
		switch (this.propValueType) {

		case ID:

			// make sure the property is a reference
			if (!propLeafValueHandler.isRef())
				throw new InvalidSpecificationException("Property " + propPath
						+ " is not a reference and its id cannot be tested.");

			// use target resource id property value handler for operand values
			opValueHandler = resources.getPersistentResourceHandler(
					propHandler.getValueHandler().getLastInChain()
						.getRefTargetClass())
				.getIdProperty().getValueHandler();

			break;

		case KEY:

			// make sure the property is a map
			if (propTopValueHandler.getType() != ResourcePropertyValueType.MAP)
				throw new InvalidSpecificationException("Property " + propPath
						+ " is not a map and does not have a key.");

			// use key value handler for operand values
			opValueHandler = propHandler.getKeyValueHandler();

			break;

		default: // VALUE

			// make sure the property has simple value to test
			if (!(propLeafValueHandler
					instanceof SimpleResourcePropertyValueHandler))
				throw new InvalidSpecificationException("Property " + propPath
						+ " does not have simple value.");

			// use property value handler for operand values
			opValueHandler = propHandler.getValueHandler();
		}

		// gather all operands using appropriate value handler
		try {
			final Collection<FilterConditionOperandImpl> operandsCol =
				new ArrayList<>(operands.length > 10 ? operands.length : 10);
			for (final Object op : operands) {
				if (op == null)
					throw new InvalidSpecificationException(
							"Filter condition operands may not be null.");
				operandsCol.add(new FilterConditionOperandImpl(
						op instanceof String ?
								opValueHandler.valueOf((String) op) : op));
			}
			this.operands = Collections.unmodifiableCollection(operandsCol);
		} catch (final InvalidResourceDataException e) {
			throw new InvalidSpecificationException("Invalid operand value.",
					e);
		}

		// save participating persistent resource classes from the chain
		for (final Iterator<? extends ResourcePropertyHandler> i =
				this.propChain.iterator(); i.hasNext();) {
			final ResourcePropertyHandler prop = i.next();
			if ((prop instanceof RefPropertyHandler) && i.hasNext())
				prsrcClasses.add(((RefPropertyHandler) prop)
						.getReferredResourceClass());
			else if (prop instanceof DependentRefPropertyHandler)
				prsrcClasses.add(((DependentRefPropertyHandler) prop)
						.getReferredResourceClass());
		}
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public FilterConditionType getType() {

		return this.type;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isNegated() {

		return this.negated;
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
	public FilterConditionOperandType getPropertyValueType() {

		return this.propValueType;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PropertyValueFunction getValueFunction() {

		return this.valueFunc;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object[] getValueFunctionParams() {

		return this.valueFuncParams;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<? extends ResourcePropertyHandler> getPropertyChain() {

		return this.propChain;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<FilterConditionOperandImpl> getOperands() {

		return this.operands;
	}
}
