package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link List} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class ListResourcePropertyValueHandler
	extends MultiValueResourcePropertyValueHandler {

	/**
	 * List element value handler.
	 */
	private final AbstractResourcePropertyValueHandlerImpl elementValueHandler;


	/**
	 * Create new handler.
	 *
	 * @param elementValueHandler List element value handler.
	 * @param collectionDegree Collection degree with 1 meaning the last
	 * collection.
	 */
	ListResourcePropertyValueHandler(
			final AbstractResourcePropertyValueHandlerImpl elementValueHandler,
			final int collectionDegree) {
		super(ResourcePropertyValueType.LIST, collectionDegree,
				elementValueHandler);

		this.elementValueHandler = elementValueHandler;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean hasStringRepresentation() {

		return this.elementValueHandler.hasStringRepresentation();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString(final Object val) {

		return this.elementValueHandler.toString(val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str)
		throws InvalidResourceDataException {

		return this.elementValueHandler.valueOf(str);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		return this.elementValueHandler.getValueClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return this.elementValueHandler.getPersistentValueType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.startCollection();

		for (final Object elementVal : (List<?>) val) {
			if (elementVal == null)
				out.writeNullValue();
			else
				this.elementValueHandler.writeValue(access, elementVal, out);
		}

		out.endCollection();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public List<?> readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		final List<Object> res = new ArrayList<>();

		if (!in.enterCollection())
			return null;

		boolean end = false;
		do {
			final Object elementVal =
				this.elementValueHandler.readValue(access, in);
			if ((elementVal == null) && in.wasCollectionEnd())
				end = true;
			else
				res.add(elementVal);
		} while (!end);

		return res;
	}
}
