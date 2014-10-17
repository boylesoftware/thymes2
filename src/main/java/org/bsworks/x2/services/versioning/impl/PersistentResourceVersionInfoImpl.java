package org.bsworks.x2.services.versioning.impl;

import java.util.Date;

import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.services.versioning.PersistentResourceVersionInfo;


/**
 * Simple persistent resource version descriptor implementation.
 *
 * @author Lev Himmelfarb
 */
public class PersistentResourceVersionInfoImpl
	implements PersistentResourceVersionInfo {

	/**
	 * Version number.
	 */
	private long version;

	/**
	 * Last modification timestamp.
	 */
	private Date lastModificationTimestamp;


	/**
	 * Create new descriptor.
	 *
	 * @param version Version number.
	 * @param lastModificationTimestamp Last modification timestamp.
	 */
	public PersistentResourceVersionInfoImpl(final long version,
			final Date lastModificationTimestamp) {

		this.version = version;
		this.lastModificationTimestamp = lastModificationTimestamp;
	}

	/**
	 * Create new uninitialized descriptor. Use setters to set the properties.
	 */
	public PersistentResourceVersionInfoImpl() {}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Property
	@Override
	public long getVersion() {

		return this.version;
	}

	/**
	 * Set version number.
	 *
	 * @param version Version number.
	 */
	public void setVersion(final long version) {

		this.version = version;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Property
	@Override
	public Date getLastModificationTimestamp() {

		return this.lastModificationTimestamp;
	}

	/**
	 * Set last modification timestamp.
	 *
	 * @param lastModificationTimestamp Last modification timestamp.
	 */
	public void setLastModificationTimestamp(
			final Date lastModificationTimestamp) {

		this.lastModificationTimestamp = lastModificationTimestamp;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public int hashCode() {

		return Long.valueOf(this.version
					+ (this.lastModificationTimestamp != null ?
							this.lastModificationTimestamp.getTime() : 0))
				.hashCode();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean equals(final Object obj) {

		if (obj == this)
			return true;

		if ((obj == null)
				|| !(obj instanceof PersistentResourceVersionInfoImpl))
			return false;

		final PersistentResourceVersionInfoImpl other =
			(PersistentResourceVersionInfoImpl) obj;

		final long thisTS =
			(this.lastModificationTimestamp != null ?
					this.lastModificationTimestamp.getTime() : 0);
		final long otherTS =
			(other.lastModificationTimestamp != null ?
					other.lastModificationTimestamp.getTime() : 0);

		return ((other.version == this.version) && (otherTS == thisTS));
	}
}
