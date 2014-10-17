package org.bsworks.x2.services.persistence.impl.jdbc;

import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Descriptor of a collection property available within a query.
 *
 * @author Lev Himmelfarb
 */
class CollectionQueryProperty {

	/**
	 * Collection property handler.
	 */
	private final ResourcePropertyHandler propHandler;

	/**
	 * Name of the table that represents the collection.
	 */
	private final String collectionTableName;

	/**
	 * Collection table alias.
	 */
	private final String collectionTableAlias;

	/**
	 * Condition expression used to join the collection table to its parent.
	 */
	private final String collectionTableJoinCondition;

	/**
	 * Expression for the key value, or {@code null} if not available.
	 */
	private final String keyExpr;

	/**
	 * Expression for the collection element value, which is record id if
	 * the collection elements are nested objects or references.
	 */
	private final String valueExpr;


	/**
	 * Create new collection property stump.
	 *
	 * @param propHandler Collection property handler.
	 * @param collectionTableName Name of the table that represents the
	 * collection.
	 * @param collectionTableAlias Collection table alias.
	 * @param collectionTableJoinCondition Condition expression used to join
	 * the collection table to its parent.
	 * @param keyExpr Expression for the key value, or {@code null} if not
	 * available.
	 * @param valueExpr Expression for the collection element value, which
	 * is record id if the collection elements are nested objects or
	 * references.
	 */
	CollectionQueryProperty(final ResourcePropertyHandler propHandler,
			final String collectionTableName,
			final String collectionTableAlias,
			final String collectionTableJoinCondition,
			final String keyExpr, final String valueExpr) {

		this.propHandler = propHandler;
		this.collectionTableName = collectionTableName;
		this.collectionTableAlias = collectionTableAlias;
		this.collectionTableJoinCondition = collectionTableJoinCondition;
		this.keyExpr = keyExpr;
		this.valueExpr = valueExpr;
	}


	/**
	 * Get collection property handler.
	 *
	 * @return The property handler.
	 */
	ResourcePropertyHandler getPropHandler() {

		return this.propHandler;
	}

	/**
	 * Get name of the table that represents the collection.
	 *
	 * @return The collection table name.
	 */
	String getCollectionTableName() {

		return this.collectionTableName;
	}

	/**
	 * Get collection table alias within the query.
	 *
	 * @return The collection table alias.
	 */
	String getCollectionTableAlias() {

		return this.collectionTableAlias;
	}

	/**
	 * Get condition expression used to join the collection table to the parent
	 * record table.
	 *
	 * @return Collection table join condition expression.
	 */
	String getCollectionTableJoinCondition() {

		return this.collectionTableJoinCondition;
	}

	/**
	 * Get expression for the key value for a map collection.
	 *
	 * @return Key value expression, or {@code null} if not available.
	 */
	String getKeyExpression() {

		return this.keyExpr;
	}

	/**
	 * Get expression for the collection element value, which is the record id
	 * if the collection elements are nested objects or references.
	 *
	 * @return Element value expression.
	 */
	String getValueExpression() {

		return this.valueExpr;
	}
}
