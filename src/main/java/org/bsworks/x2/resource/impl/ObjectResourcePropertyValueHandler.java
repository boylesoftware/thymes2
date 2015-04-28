package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bsworks.x2.Actor;
import org.bsworks.x2.resource.IdPropertyHandler;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.ResourcePropertyAccess;
import org.bsworks.x2.resource.ResourceReadSession;
import org.bsworks.x2.resource.ResourceReadSessionCache;
import org.bsworks.x2.resource.ResourceWriteSession;
import org.bsworks.x2.services.persistence.PersistentValueType;


/**
 * Object resource property value handler implementation.
 *
 * @author Lev Himmelfarb
 */
class ObjectResourcePropertyValueHandler
	extends SingleValueResourcePropertyValueHandler {

	/**
	 * Tells if handles a polymorphic object.
	 */
	private final boolean polymorphic;

	/**
	 * Object class properties. For polymorphic object, common superclass
	 * properties.
	 */
	private final ResourcePropertiesContainerImpl<?> objProps;

	/**
	 * Concrete value class properties by value type names, or {@code null} if
	 * not a polymorphic object.
	 */
	private final Map<String, ResourcePropertiesContainerImpl<?>> valueTypes;


	/**
	 * Create new handler for the specified object class.
	 *
	 * @param objProps The object class properties. For a polymorphic property, the
	 * common superclass properties.
	 * @param valueTypes For a polymorphic object, list of all possible concrete
	 * value classes with their properties. All classes in the map must be
	 * sub-classes of {@code objClass} (the constructor does not make a check).
	 * The keys are type names. For a non-polymorphic property must be
	 * {@code null} or empty.
	 */
	ObjectResourcePropertyValueHandler(
			final ResourcePropertiesContainerImpl<?> objProps,
			final Map<String, ResourcePropertiesContainerImpl<?>> valueTypes) {
		super(ResourcePropertyValueType.OBJECT);

		this.objProps = objProps;
		if ((valueTypes != null) && !valueTypes.isEmpty()) {
			this.polymorphic = true;
			this.valueTypes =
				Collections.unmodifiableMap(new HashMap<>(valueTypes));
		} else {
			this.polymorphic = false;
			this.valueTypes = null;
		}
	}


	/**
	 * Get contained resource properties.
	 *
	 * @return Resource properties container.
	 */
	ResourcePropertiesContainerImpl<?> getResourceProperties() {

		return this.objProps;
	}

	/**
	 * For a polymorphic object, get properties for the value types.
	 *
	 * @return Property containers for the value types, or {@code null} if not
	 * polymorphic.
	 */
	Collection<ResourcePropertiesContainerImpl<?>> getValueTypes() {

		if (!this.polymorphic)
			return null;

		return this.valueTypes.values();
	}


	/**
	 * Returns {@code false}.
	 */
	@Override
	public boolean hasStringRepresentation() {

		return false;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public String toString(final Object val) {

		throw new UnsupportedOperationException(
				"Object values cannot be represented as strings.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object valueOf(final String str) {

		throw new UnsupportedOperationException(
				"Object values cannot be represented as strings.");
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Class<?> getValueClass() {

		return this.objProps.getContainerClass();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public PersistentValueType getPersistentValueType() {

		return PersistentValueType.OBJECT;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void writeValue(final ResourcePropertyAccess access,
			final Object val, final ResourceWriteSession out)
		throws IOException {

		// start the object
		final ResourcePropertiesContainerImpl<?> objProps;
		if (this.polymorphic) {
			final TypePropertyHandlerImpl h = this.objProps.getTypeProperty();
			final String valueTypeName =
				h.getValueHandler().toString(h.getValue(val));
			objProps = this.valueTypes.get(valueTypeName);
			if (objProps == null)
				throw new IllegalArgumentException(
						"Invalid polymorphic property value type "
								+ valueTypeName + ".");
			out.startObject(val, h, valueTypeName);
		} else {
			objProps = this.objProps;
			out.startObject(val);
		}

		// get the consumer
		final Actor actor = out.getActor();

		// write object properties
		for (final AbstractResourcePropertyHandlerImpl propHandler :
				objProps.getProperties().values()) {

			// check access to the property
			if (!propHandler.isAllowed(access, actor))
				continue;

			// get property value
			final Object propVal = propHandler.getValue(val);

			// skip aggregate property if null
			if ((propVal == null)
					&& (propHandler
							instanceof DependentAggregatePropertyHandlerImpl))
				continue;

			// write the property
			out.addProperty(propHandler.getName());
			if (propVal == null)
				out.writeNullValue();
			else
				propHandler.getValueHandler().writeValue(access, propVal, out);
		}

		// end object
		out.endObject();
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Object readValue(final ResourcePropertyAccess access,
			final ResourceReadSession in)
		throws InvalidResourceDataException, IOException {

		// enter the object
		if (!in.enterObject())
			return null;

		// get object properties
		final ResourcePropertiesContainerImpl<?> objProps;
		if (this.polymorphic) {
			final String valueTypeName = in.readObjectType(
					this.objProps.getTypeProperty());
			objProps = this.valueTypes.get(valueTypeName);
			if (objProps == null)
				throw new InvalidResourceDataException(
						"Invalid polymorphic property type " + valueTypeName
						+ ".");
		} else {
			objProps = this.objProps;
		}

		// get object instance
		final Object obj;
		final ResourceReadSessionCache sessionCache = in.getSessionCache();
		try {

			// check if using session cache
			if (sessionCache != null) {

				// get the cache key
				final String objCacheKey;
				final IdPropertyHandler idPropHandler =
					objProps.getIdProperty();
				final Object id;
				if (idPropHandler != null) {
					final String idPropName = in.nextProperty();
					if (!idPropHandler.getName().equals(idPropName))
						throw new InvalidResourceDataException(
								"First property in the input for "
									+ objProps.getContainerClass().getName()
									+ " must be the record id.");
					id = idPropHandler.getValueHandler().readValue(access, in);
					objCacheKey = objProps.getCacheKeyPrefix() + id;
				} else { // no id property
					id = null;
					objCacheKey = objProps.getCacheKeyPrefix();
				}
				final String cacheKey = (objProps.getPropertyName() == null ?
						objCacheKey : sessionCache.getContext() + objCacheKey);

				// get the instance
				final Object cachedObj = sessionCache.get(cacheKey);
				if (cachedObj != null) {
					obj = cachedObj;
				} else {
					obj = objProps.getContainerClass().newInstance();
					if (idPropHandler != null)
						idPropHandler.setValue(obj, id);
					sessionCache.put(cacheKey, obj);
				}

				// enter instance context
				sessionCache.enterCacheContext(cacheKey);

			} else { // session cache is not used, create new instance
				obj = objProps.getContainerClass().newInstance();
			}

		} catch (final IllegalAccessException | InstantiationException e) {
			throw new RuntimeException("Error instantiating a resource.", e);
		}

		// get the provider
		final Actor actor = in.getActor();

		// read object properties
		final Map<String, AbstractResourcePropertyHandlerImpl> props =
			objProps.getProperties();
		for (String propName = in.nextProperty(); propName != null;
				propName = in.nextProperty()) {

			// get property handler and check access to the property
			final AbstractResourcePropertyHandlerImpl propHandler =
				props.get(propName);
			if ((propHandler == null)
					|| !propHandler.isAllowed(access, actor)) {
				in.swallowValue();
				continue;
			}

			// read the property value and set it in the object
			propHandler.setValue(obj,
					propHandler.getValueHandler().readValue(access, in));
		}

		// leave cache context
		if (sessionCache != null)
			sessionCache.leaveCacheContext();

		// return the object
		return obj;
	}
}
