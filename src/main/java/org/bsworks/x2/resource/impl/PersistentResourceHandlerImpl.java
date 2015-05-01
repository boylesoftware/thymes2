package org.bsworks.x2.resource.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.MetaPropertyType;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.PersistentResource;
import org.bsworks.x2.resource.impl.AccessChecker.TargetType;
import org.bsworks.x2.util.StringUtils;


/**
 * Persistent resource handler implementation.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
class PersistentResourceHandlerImpl<R>
	extends AbstractResourceHandlerImpl<R>
	implements PersistentResourceHandler<R> {

	/**
	 * Persistent collection name.
	 */
	private final String persistentCollectionName;

	/**
	 * Resource access checker.
	 */
	private final AccessChecker accessChecker;

	/**
	 * Record meta-property handlers.
	 */
	private final Map<MetaPropertyType, MetaPropertyHandlerImpl>
	metaPropertyHandlers;

	/**
	 * Dependent resource reference property handlers.
	 */
	private final Collection<DependentRefPropertyHandlerImpl>
	dependentRefPropertyHandlers;

	/**
	 * Aggregate property handlers.
	 */
	private final Collection<AggregatePropertyHandlerImpl>
	aggregatePropertyHandlers;


	/**
	 * Create persistent application resource handler.
	 *
	 * @param resources Reference to the resources manager.
	 * @param prsrcClasses All persistent resource classes.
	 * @param prsrcClass Persistent resource class.
	 * @param prsrcAnno The {@link PersistentResource} annotation.
	 *
	 * @throws IllegalArgumentException If something is wrong with the specified
	 * persistent resource class.
	 */
	PersistentResourceHandlerImpl(final ResourcesImpl resources,
			final Set<Class<?>> prsrcClasses, final Class<R> prsrcClass,
			final PersistentResource prsrcAnno) {
		super(new ResourcePropertiesContainerImpl<>(resources, prsrcClasses,
						prsrcClass, null, null,
						StringUtils.defaultIfEmpty(
								prsrcAnno.persistentCollection(),
								prsrcClass.getSimpleName()), null,
						"", ""));

		// process annotation
		this.persistentCollectionName =
			this.props.getPersistentCollectionName();
		this.accessChecker =
			new AccessChecker(prsrcAnno.accessRestrictions(),
					TargetType.PERSISTENT);

		// check that we have record id property
		if (this.getIdProperty() == null)
			throw new IllegalArgumentException("Persistent resource "
					+ prsrcClass.getName()
					+ " does not contain record id property.");

		// get meta-properties, aggregate and dependent resource properties
		this.metaPropertyHandlers =
			new HashMap<>(MetaPropertyType.values().length);
		final ArrayList<DependentRefPropertyHandlerImpl>
		dependentRefPropertyHandlers = new ArrayList<>();
		final ArrayList<AggregatePropertyHandlerImpl>
		aggregatePropertyHandlers = new ArrayList<>();
		for (final AbstractResourcePropertyHandlerImpl propHandler :
				this.getProperties().values()) {
			if (propHandler instanceof MetaPropertyHandlerImpl) {
				final MetaPropertyHandlerImpl metaPropHandler =
					(MetaPropertyHandlerImpl) propHandler;
				if (this.metaPropertyHandlers.put(metaPropHandler.getType(),
						metaPropHandler) != null)
					throw new IllegalArgumentException("Persistent resource "
							+ prsrcClass.getName()
							+ " contains more than one "
							+ metaPropHandler.getType() + " meta-property.");
			} else if (propHandler instanceof
							DependentRefPropertyHandlerImpl) {
				final DependentRefPropertyHandlerImpl ph =
					(DependentRefPropertyHandlerImpl) propHandler;
				dependentRefPropertyHandlers.add(ph);
			} else if (propHandler instanceof AggregatePropertyHandlerImpl) {
				final AggregatePropertyHandlerImpl ph =
					(AggregatePropertyHandlerImpl) propHandler;
				aggregatePropertyHandlers.add(ph);
			}
		}
		dependentRefPropertyHandlers.trimToSize();
		this.dependentRefPropertyHandlers =
			Collections.unmodifiableCollection(dependentRefPropertyHandlers);
		aggregatePropertyHandlers.trimToSize();
		this.aggregatePropertyHandlers =
			Collections.unmodifiableCollection(aggregatePropertyHandlers);
	}


	/* (non-Javadoc)
	 * @see org.bsworks.x2.resource.PersistentResourceHandler#getPersistentCollectionName()
	 */
	@Override
	public String getPersistentCollectionName() {

		return this.persistentCollectionName;
	}

	/* (non-Javadoc)
	 * @see org.bsworks.x2.resource.PersistentResourceHandler#isAllowed(org.bsworks.x2.resource.ResourcePropertyAccess, org.bsworks.x2.Actor)
	 */
	@Override
	public boolean isAllowed(final ResourcePropertyAccess access,
			final Actor actor) {

		return this.accessChecker.isAllowed(access, actor);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public IdPropertyHandlerImpl getIdProperty() {

		return this.props.getIdProperty();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public MetaPropertyHandlerImpl getMetaProperty(
			final MetaPropertyType type) {

		return this.metaPropertyHandlers.get(type);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<DependentRefPropertyHandlerImpl>
	getDependentRefProperties() {

		return this.dependentRefPropertyHandlers;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<AggregatePropertyHandlerImpl> getAggregateProperties() {

		return this.aggregatePropertyHandlers;
	}
}
