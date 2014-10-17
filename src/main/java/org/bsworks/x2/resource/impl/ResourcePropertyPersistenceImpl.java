package org.bsworks.x2.resource.impl;

import org.bsworks.x2.resource.ResourcePropertyPersistence;
import org.bsworks.x2.resource.annotations.Persistence;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.util.StringUtils;


/**
 * Resource property persistence descriptor implementation.
 *
 * @author Lev Himmelfarb
 */
class ResourcePropertyPersistenceImpl
	implements ResourcePropertyPersistence {

	/**
	 * Field name.
	 */
	private final String fieldName;

	/**
	 * Collection name.
	 */
	private final String collectionName;

	/**
	 * Parent id field name.
	 */
	private final String parentIdFieldName;

	/**
	 * Key field name.
	 */
	private final String keyFieldName;

	/**
	 * Tells if optional.
	 */
	private final boolean optional;


	/**
	 * Create new descriptor for a property.
	 *
	 * @param propAnno Property annotation.
	 * @param propName Property name.
	 * @param ctxPersistentFieldsPrefix Context persistent field names prefix,
	 * may be empty string but not {@code null}.
	 */
	ResourcePropertyPersistenceImpl(final Property propAnno,
			final String propName, final String ctxPersistentFieldsPrefix) {

		final Persistence persistenceAnno = propAnno.persistence();

		this.fieldName = ctxPersistentFieldsPrefix
				+ StringUtils.defaultIfEmpty(persistenceAnno.field(), propName);
		this.collectionName =
			StringUtils.nullIfEmpty(persistenceAnno.collection());
		if (this.collectionName != null) {
			this.parentIdFieldName =
				StringUtils.nullIfEmpty(persistenceAnno.parentIdField());
			this.keyFieldName =
				StringUtils.nullIfEmpty(persistenceAnno.keyField());
		} else {
			this.parentIdFieldName = null;
			this.keyFieldName = null;
		}
		this.optional = persistenceAnno.optional();
	}

	/**
	 * Create new descriptor.
	 *
	 * @param fieldName Field name.
	 * @param collectionName Collection name.
	 * @param parentIdFieldName Parent id field name.
	 * @param keyFieldName Key field name.
	 * @param optional {@code true} if optional.
	 */
	ResourcePropertyPersistenceImpl(final String fieldName,
			final String collectionName, final String parentIdFieldName,
			final String keyFieldName, final boolean optional) {

		this.fieldName = fieldName;
		this.collectionName = collectionName;
		this.parentIdFieldName = parentIdFieldName;
		this.keyFieldName = keyFieldName;
		this.optional = optional;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getFieldName() {

		return this.fieldName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getCollectionName() {

		return this.collectionName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getParentIdFieldName() {

		return this.parentIdFieldName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String getKeyFieldName() {

		return this.keyFieldName;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isOptional() {

		return this.optional;
	}
}
