package org.bsworks.x2.resource.impl;

import java.io.IOException;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Ref} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class RefResourcePropertyValueHandler
	extends CanBeMapKeyResourcePropertyValueHandler {

	/**
	 * Resource manager.
	 */
	private final ResourcesImpl resources;

	/**
	 * Reference target class, or {@code null} for any reference.
	 */
	private final Class<?> refTargetClass;

	/**
	 * Tells if the reference target is a wildcard.
	 */
	private final boolean wildcard;


	/**
	 * Create new handler.
	 *
	 * @param resources Resources manager.
	 * @param refTargetClass Reference target class, or {@code null} for any
	 * reference. If {@code null}, {@code wildcard} argument must be
	 * {@code true}.
	 * @param wildcard {@code true} if wildcard reference.
	 */
	RefResourcePropertyValueHandler(final ResourcesImpl resources,
			final Class<?> refTargetClass, final boolean wildcard) {
		super(ResourcePropertyValueType.REF);

		this.resources = resources;
		this.refTargetClass = refTargetClass;
		this.wildcard = wildcard;

		if (!this.wildcard && (this.refTargetClass == null))
			throw new IllegalArgumentException("Need target persistent resource"
					+ " class for a non-wildcard reference.");
	}


	/**
	 * Tell if wildcard reference.
	 *
	 * @return {@code true} if wildcard reference.
	 */
	boolean isWildcard() {

		return this.wildcard;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getRefTargetClass() {

		return this.refTargetClass;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public RefImpl<?> valueOf(final String str)
		throws InvalidResourceDataException {

		if (str == null)
			return null;

		final RefImpl<?> ref;
		try {
			ref = this.resources.parseRef(str);
		} catch (final IllegalArgumentException e) {
			throw new InvalidResourceDataException(
					"Invalid persistent resource reference value " + str
					+ ".", e);
		}

		if ((this.refTargetClass != null)
				&& !this.refTargetClass.isAssignableFrom(
						ref.getResourceClass()))
			throw new InvalidResourceDataException(
					"Invalid persistent resource reference target class "
							+ ref.getResourceClass().getName() + ".");

		return ref;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		return Ref.class;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		if (this.wildcard
				|| !this.resources.isPersistentResource(this.refTargetClass))
			throw new UnsupportedOperationException(
					"Not a persistent property");

		return this.resources.getPersistentResourceHandler(this.refTargetClass)
				.getIdProperty().getValueHandler().getPersistentValueType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.writeValue((Ref<?>) val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Ref<?> readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		final Ref<?> ref = in.readRefValue();
		if (ref == null)
			return null;

		if ((this.refTargetClass != null)
				&& !this.refTargetClass.isAssignableFrom(
						ref.getResourceClass()))
			throw new InvalidResourceDataException(
					"Invalid persistent resource reference target class "
							+ ref.getResourceClass().getName() + ".");

		return ref;
	}
}
