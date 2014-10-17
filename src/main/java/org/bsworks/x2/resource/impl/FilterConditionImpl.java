package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.FilterCondition;
import org.bsworks.x2.resource.FilterConditionOperandType;
import org.bsworks.x2.resource.FilterConditionType;
import org.bsworks.x2.resource.InvalidResourceDataException;
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
	 * @param type Condition type.
	 * @param negated {@code true} if negated.
	 * @param prsrcHandler Root persistent resource handler.
	 * @param propPath Property path.
	 * @param operands Condition operands.
	 * @param prsrcClasses Set, to which to add any participating persistent
	 * resource classes.
	 */
	FilterConditionImpl(final FilterConditionType type, final boolean negated,
			final PersistentResourceHandlerImpl<?> prsrcHandler,
			final String propPath, final Object[] operands,
			final Set<Class<?>> prsrcClasses) {

		this.type = type;
		this.negated = negated;

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

		this.propChain = prsrcHandler.getPersistentPropertyChain(this.propPath);

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

		final AbstractResourcePropertyHandlerImpl propHandler =
			(AbstractResourcePropertyHandlerImpl) this.propChain.getLast();
		try {
			final ResourcePropertyValueHandler vh =
				propHandler.getValueHandler();
			final Collection<FilterConditionOperandImpl> operandsCol =
				new ArrayList<>(operands.length > 10 ? operands.length : 10);
			for (final Object op : operands) {
				if (op == null)
					throw new IllegalArgumentException(
							"Filter condition operands may not be null.");
				final Object opVal;
				if (op instanceof String)
					opVal = vh.valueOf((String) op);
				else
					opVal = op;
				operandsCol.add(new FilterConditionOperandImpl(opVal));
			}
			this.operands = Collections.unmodifiableCollection(operandsCol);
		} catch (final InvalidResourceDataException e) {
			throw new IllegalArgumentException("Invalid operand value.", e);
		}

		final boolean presenceCheck = ((this.type == FilterConditionType.EMPTY)
				|| (this.type == FilterConditionType.NOT_EMPTY));

		if (presenceCheck && !this.operands.isEmpty())
			throw new IllegalArgumentException("This type of filter"
					+ " condition does not use operands.");
		if (!presenceCheck && this.operands.isEmpty())
			throw new IllegalArgumentException("This type of filter"
					+ " condition requires at least one operand.");

		final ResourcePropertyValueType propTopValueType =
			propHandler.getValueHandler().getType();
		if ((this.propValueType == FilterConditionOperandType.KEY)
				&& (propTopValueType != ResourcePropertyValueType.MAP))
			throw new IllegalArgumentException("Property " + propPath
					+ " is not a map and does not have a key.");

		final ResourcePropertyValueType propLeafValueType =
				propHandler.getValueHandler().getLastInChain().getType();
		switch (propLeafValueType) {
		case OBJECT:
			if ((this.propValueType != FilterConditionOperandType.KEY)
					&& !presenceCheck)
				throw new IllegalArgumentException("This type of filter"
						+ " condition is not applicable to nested object"
						+ " property " + propPath + ".");
			break;
		case REF:
			if ((this.propValueType == FilterConditionOperandType.VALUE)
					&& !presenceCheck)
				throw new IllegalArgumentException("This type of filter"
						+ " condition is not applicable to reference property "
						+ propPath + ".");
			break;
		case STRING:
			break;
		default:
			if ((this.type == FilterConditionType.MATCH)
					|| (this.type == FilterConditionType.NOT_MATCH)
					|| (this.type == FilterConditionType.MATCH_CS)
					|| (this.type == FilterConditionType.NOT_MATCH_CS)
					|| (this.type == FilterConditionType.PREFIX)
					|| (this.type == FilterConditionType.NOT_PREFIX)
					|| (this.type == FilterConditionType.PREFIX_CS)
					|| (this.type == FilterConditionType.NOT_PREFIX_CS))
				throw new IllegalArgumentException("This type of filter"
						+ " condition is not applicable to non-string property "
						+ propPath + ".");
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
