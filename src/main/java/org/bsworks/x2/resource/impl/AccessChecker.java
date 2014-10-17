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
 * User to check resource access.
 *
 * @author Lev Himmelfarb
 */
class AccessChecker {

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
	 * Tells if the checker is for something persistent.
	 */
	private final boolean persistent;

	/**
	 * Tells if the checked is for a dependent reference property.
	 */
	private final boolean depRef;


	/**
	 * Create new checker.
	 *
	 * @param accessRestrictions Access restrictions.
	 * @param persistent {@code true} if checker is for a persistent something.
	 * @param depRef {@code true} if for a dependent reference property.
	 */
	AccessChecker(final AccessRestriction[] accessRestrictions,
			final boolean persistent, final boolean depRef) {

		this.persistent = persistent;
		this.depRef = depRef;

		final Map<ResourcePropertyAccess, Set<String>> acls = new HashMap<>();
		for (final AccessRestriction r : accessRestrictions) {
			final boolean allowedToSome = (r.allowTo().length > 0);
			if (!persistent) {
				switch (r.value()) {
				case LOAD:
				case PERSIST:
				case UPDATE:
				case DELETE:
					if (allowedToSome)
						throw new IllegalArgumentException("Access type "
								+ r.value()
								+ " requires a persistent property.");
				default:
				}
			} else if (depRef) {
				switch (r.value()) {
				case PERSIST:
					if (allowedToSome)
						throw new IllegalArgumentException("Dependent"
								+ " references cannot be persisted.");
				default:
				}
			}
			Set<String> roles = acls.get(r.value());
			if (roles != null)
				throw new IllegalArgumentException(
						"More than one roles list for " + r.value()
						+ " access mode.");
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
			acls.put(r.value(), roles);
		}

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
			acl = this.submitACL;
			break;
		case LOAD:
			acl = this.loadACL;
			break;
		case PERSIST:
			if (!this.persistent || this.depRef)
				return false;
			acl = this.persistACL;
			break;
		case UPDATE:
			if (!this.persistent)
				return false;
			acl = this.updateACL;
			break;
		case DELETE:
			if (!this.persistent)
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
