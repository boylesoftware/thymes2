package org.bsworks.x2.services.persistence.impl.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bsworks.x2.resource.ResourcePropertyHandler;


/**
 * Query branch.
 *
 * @author Lev Himmelfarb
 */
class QueryBranch {

	/**
	 * The query builder.
	 */
	private final QueryBuilder qb;

	/**
	 * Anchor column expression.
	 */
	private final String anchorColExpr;

	/**
	 * Join expression.
	 */
	private final String joinExpr;

	/**
	 * Join attachment expression.
	 */
	private final String joinAttachmentExpr;

	/**
	 * Merged branches join attachment expressions.
	 */
	private final String mergedJoinAttachmentExprs;

	/**
	 * Node property path.
	 */
	private final String propPath;

	/**
	 * Node property handler.
	 */
	private final ResourcePropertyHandler nodeProp;

	/**
	 * Attachment chain.
	 */
	private final List<ResourcePropertyHandler> attachmentChain;


	/**
	 * Create new branch object.
	 *
	 * @param qb Query builder.
	 * @param anchorColExpr Anchor column expression, or {@code null} for the
	 * root.
	 * @param joinExpr Join expression, empty for embedded property branch, or
	 * {@code null} for the root.
	 * @param joinAttachmentExpr Attachment expression in the join expression,
	 * which is the value expression for the column in the joined table that is
	 * used to attach it to the parent table.
	 * @param mergedJoinAttachmentExprs Comma-separated list of attachment
	 * expressions of all branches merged into this branch.
	 * @param propPath Branch property path, or empty string if root branch.
	 * @param attachmentChain Attachment chain with the last element being the
	 * handler of the property that is the branch root, or empty list if the
	 * branch of the top-level resource's branch.
	 */
	QueryBranch(final QueryBuilder qb, final String anchorColExpr,
			final String joinExpr, final String joinAttachmentExpr,
			final String mergedJoinAttachmentExprs, final String propPath,
			final List<ResourcePropertyHandler> attachmentChain) {

		this.qb = qb;
		this.anchorColExpr = anchorColExpr;
		this.joinExpr = joinExpr;
		this.joinAttachmentExpr = joinAttachmentExpr;
		this.mergedJoinAttachmentExprs = mergedJoinAttachmentExprs;
		this.propPath = propPath;
		final int chainLen = attachmentChain.size();
		this.nodeProp = (chainLen == 0 ? null :
			attachmentChain.get(chainLen - 1));
		this.attachmentChain =
			Collections.unmodifiableList(new ArrayList<>(attachmentChain));
	}


	/**
	 * Get branch's query builder.
	 *
	 * @return The query builder.
	 */
	QueryBuilder getQueryBuilder() {

		return this.qb;
	}

	/**
	 * Get "SELECT" clause column expression for the branch's anchor.
	 *
	 * @return Column expression, or {@code null} for the root.
	 */
	String getAnchorColumnExpression() {

		return this.anchorColExpr;
	}

	/**
	 * Get "FROM" clause join expression used to attach the branch to the stem.
	 *
	 * @return Join expression, empty string for embedded property branch, or
	 * {@code null} for the root.
	 */
	String getJoinExpression() {

		return this.joinExpr;
	}

	/**
	 * Get attachment expression in the join expression. The attachment
	 * expression is the column expression used in the join expression for
	 * attaching the joined table to the parent table.
	 *
	 * @return Attachment expression, or empty string or {@code null} similarly
	 * to what's returned by {@link #getJoinExpression()}.
	 */
	String getJoinAttachmentExpression() {

		return this.joinAttachmentExpr;
	}

	/**
	 * Get comma-separated list of join expressions of all branches merged into
	 * this branch.
	 *
	 * @return Comma-separated list of attachment expressions, or empty string
	 * or {@code null} if none.
	 */
	String getMergedJoinAttachmentExpressions() {

		return this.mergedJoinAttachmentExprs;
	}

	/**
	 * Get branch property path.
	 *
	 * @return Property path, or empty string if root branch.
	 */
	String getNodePropertyPath() {

		return this.propPath;
	}

	/**
	 * Tell if the branch root property (the last property in the branch
	 * attachment chain) is a collection.
	 *
	 * @return {@code true} if collection branch.
	 */
	boolean isCollection() {

		if (this.nodeProp == null)
			return false;

		return !this.nodeProp.isSingleValued();
	}

	/**
	 * Get branch attachment chain.
	 *
	 * @return The nested properties chain. The first element is a property of
	 * the top-level persistent resource. The last element is a collection
	 * property, which needs to be set with the result of this branch query.
	 * Empty list for the root.
	 */
	List<ResourcePropertyHandler> getAttachmentChain() {

		return this.attachmentChain;
	}
}
