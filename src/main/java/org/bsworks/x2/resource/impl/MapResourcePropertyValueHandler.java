package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * {@link Map} resource property value handler.
 *
 * @author Lev Himmelfarb
 */
class MapResourcePropertyValueHandler
	extends MultiValueResourcePropertyValueHandler {

	/**
	 * Map entry key value handler.
	 */
	private final CanBeMapKeyResourcePropertyValueHandler entryKeyHandler;

	/**
	 * Map entry value handler.
	 */
	private final AbstractResourcePropertyValueHandlerImpl entryValueHandler;


	/**
	 * Create new handler.
	 *
	 * @param entryKeyHandler Map entry key value handler.
	 * @param entryValueHandler Map entry value handler.
	 * @param collectionDegree Collection degree with 1 meaning the last map.
	 */
	MapResourcePropertyValueHandler(
			final CanBeMapKeyResourcePropertyValueHandler entryKeyHandler,
			final AbstractResourcePropertyValueHandlerImpl entryValueHandler,
			final int collectionDegree) {
		super(ResourcePropertyValueType.MAP, collectionDegree,
				entryValueHandler);

		this.entryKeyHandler = entryKeyHandler;
		this.entryValueHandler = entryValueHandler;
	}


	/**
	 * Get key value handler.
	 *
	 * @return Key value handler.
	 */
	CanBeMapKeyResourcePropertyValueHandler getKeyValueHandler() {

		return this.entryKeyHandler;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean hasStringRepresentation() {

		return this.entryValueHandler.hasStringRepresentation();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString(final Object val) {

		return this.entryValueHandler.toString(val);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str)
		throws InvalidResourceDataException {

		return this.entryValueHandler.valueOf(str);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		return this.entryValueHandler.getValueClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return this.entryValueHandler.getPersistentValueType();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		out.startMap();

		for (final Map.Entry<?, ?> entry : ((Map<?, ?>) val).entrySet()) {
			final Object key = entry.getKey();
			if (key == null)
				throw new IllegalArgumentException("Map key may not be null.");
			out.writeKey(this.entryKeyHandler.toString(key));
			final Object elementValue = entry.getValue();
			if (elementValue == null)
				out.writeNullValue();
			else
				this.entryValueHandler.writeValue(access, elementValue, out);
		}

		out.endMap();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Map<?, ?> readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		final Map<Object, Object> res = new HashMap<>();

		if (!in.enterMap())
			return null;

		for (String keyStr = in.readKey(); keyStr != null;
				keyStr = in.readKey()) {
			final Object key = this.entryKeyHandler.valueOf(keyStr);
			if (res.put(key, this.entryValueHandler.readValue(access, in))
					!= null)
				throw new InvalidResourceDataException(
						"Multiple entries for key \"" + keyStr + "\".");
		}

		return res;
	}
}
