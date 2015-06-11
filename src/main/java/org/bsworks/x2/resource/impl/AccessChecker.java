package org.bsworks.x2.resource.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.annotations.AccessRestriction;
import org.bsworks.x2.resource.annotations.Special;


/**
 * Used to check resource access.
 *
 * @author Lev Himmelfarb
 */
class AccessChecker {

	/**
	 * Access target type.
	 */
	enum TargetType {

		/**
		 * Something persistent.
		 */
		PERSISTENT,

		/**
		 * Something transient.
		 */
		TRANSIENT,

		/**
		 * Dependent resource reference.
		 */
		DEP_REF,

		/**
		 * Borrowed property (nested object owned by another persistent
		 * resource).
		 */
		BORROWED,

		/**
		 * Aggregate property.
		 */
		AGGREGATE
	}


	/**
	 * Special value for public access.
	 */
	private static final Set<String> ANY =
		Collections.unmodifiableSet(new HashSet<String>(0));

	/**
	 * Special value for no access.
	 */
	private static final Set<String> NONE =
		Collections.unmodifiableSet(new HashSet<String>(0));

	/**
	 * Special value for authenticated only access.
	 */
	private static final Set<String> AUTHED_ONLY =
		Collections.unmodifiableSet(new HashSet<String>(0));


	/**
	 * Roles allowed to see.
	 */
	private final Set<String> seeACL;

	/**
	 * Roles allowed to submit.
	 */
	private final Set<String> submitACL;

	/**
	 * Roles allowed to load.
	 */
	private final Set<String> loadACL;

	/**
	 * Roles allowed to persist.
	 */
	private final Set<String> persistACL;

	/**
	 * Roles allowed to update.
	 */
	private final Set<String> updateACL;

	/**
	 * Roles allowed to delete.
	 */
	private final Set<String> deleteACL;

	/**
	 * Checker target type.
	 */
	private final TargetType targetType;


	/**
	 * Create new checker.
	 *
	 * @param accessRestrictions Access restrictions.
	 * @param targetType Checker target type.
	 */
	AccessChecker(final AccessRestriction[] accessRestrictions,
			final TargetType targetType) {

		this.targetType = targetType;

		// process specified restrictions and create ACLs
		final Map<ResourcePropertyAccess, Set<String>> acls = new HashMap<>();
		for (final AccessRestriction r : accessRestrictions) {

			// test applicability of specified restrictions to the target
			final ResourcePropertyAccess access = r.value();
			final boolean allowedToSome = (r.allowTo().length > 0);
			if (allowedToSome) switch (targetType) {
			case TRANSIENT:
				switch (access) {
				case LOAD:
				case PERSIST:
				case UPDATE:
				case DELETE:
					throw new IllegalArgumentException("Access type "
							+ access + " requires a persistent property.");
				default:
				}
				break;
			case DEP_REF:
				if (access == ResourcePropertyAccess.PERSIST)
					throw new IllegalArgumentException("Dependent"
							+ " references cannot be persisted.");
				break;
			case BORROWED:
				if (access != ResourcePropertyAccess.SEE)
					throw new IllegalArgumentException("Access type "
							+ access + " is invalid for a property owned by"
							+ " another persistent resource.");
				break;
			case AGGREGATE:
				if (access != ResourcePropertyAccess.SEE)
					throw new IllegalArgumentException("Access type "
							+ access
							+ " is invalid for an aggregate property.");
				break;
			default: // PERSISTENT, all types of access are applicable
			}

			// test that access type is specified only once
			Set<String> roles = acls.get(access);
			if (roles != null)
				throw new IllegalArgumentException(
						"More than one roles list for " + access
						+ " access mode.");

			// get the roles list
			if ((r.allowTo().length == 1)
					&& r.allowTo()[0].equals(Special.AUTHED_ONLY)) {
				roles = AUTHED_ONLY;
			} else {
				roles = new HashSet<>();
				for (final String role : r.allowTo()) {
					if (role.equals(Special.AUTHED_ONLY))
						throw new IllegalArgumentException("Special role \""
								+ Special.AUTHED_ONLY + "\" must be the only"
								+ " element in the roles list.");
					roles.add(role);
				}
			}

			// save the roles for the access type
			acls.put(access, roles);
		}

		// create ACLs
		this.seeACL = getACL(acls, ResourcePropertyAccess.SEE);
		this.submitACL = getACL(acls, ResourcePropertyAccess.SUBMIT);
		this.loadACL = getACL(acls, ResourcePropertyAccess.LOAD);
		this.persistACL = getACL(acls, ResourcePropertyAccess.PERSIST);
		this.updateACL = getACL(acls, ResourcePropertyAccess.UPDATE);
		this.deleteACL = getACL(acls, ResourcePropertyAccess.DELETE);
	}

	/**
	 * Get ACL.
	 *
	 * @param acls ACLs.
	 * @param access Access mode.
	 *
	 * @return The ACL.
	 */
	private static Set<String> getACL(
			final Map<ResourcePropertyAccess, Set<String>> acls,
			final ResourcePropertyAccess access) {

		final Set<String> acl = acls.get(access);

		if (acl == null)
			return ANY;

		if (acl == AUTHED_ONLY)
			return AUTHED_ONLY;

		if (acl.isEmpty())
			return NONE;

		return Collections.unmodifiableSet(new HashSet<>(acl));
	}


	/**
	 * Tell if access is allowed.
	 *
	 * @param access Access type.
	 * @param actor The actor, or {@code null} for unauthenticated.
	 *
	 * @return {@code true} if allowed.
	 */
	boolean isAllowed(final ResourcePropertyAccess access, final Actor actor) {

		final Set<String> acl;
		switch (access) {
		case SEE:
			acl = this.seeACL;
			break;
		case SUBMIT:
			if ((this.targetType == TargetType.AGGREGATE)
				|| (this.targetType == TargetType.BORROWED))
				return false;
			acl = this.submitACL;
			break;
		case LOAD:
			acl = this.loadACL;
			break;
		case PERSIST:
			if (this.targetType != TargetType.PERSISTENT)
				return false;
			acl = this.persistACL;
			break;
		case UPDATE:
			if ((this.targetType == TargetType.TRANSIENT)
				|| (this.targetType == TargetType.AGGREGATE)
				|| (this.targetType == TargetType.BORROWED))
				return false;
			acl = this.updateACL;
			break;
		case DELETE:
			if ((this.targetType == TargetType.TRANSIENT)
				|| (this.targetType == TargetType.AGGREGATE)
				|| (this.targetType == TargetType.BORROWED))
				return false;
			acl = this.deleteACL;
			break;
		default: // never happens
			return false;
		}

		if (acl == ANY)
			return true;

		if (acl == NONE)
			return false;

		if (actor == null)
			return false;

		if (acl == AUTHED_ONLY)
			return true;

		return actor.hasAnyRole(acl);
	}
}
