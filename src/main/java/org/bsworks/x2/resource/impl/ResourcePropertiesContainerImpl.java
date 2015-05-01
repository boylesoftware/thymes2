package org.bsworks.x2.resource.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bsworks.x2.resource.AggregatePropertyHandler;
import org.bsworks.x2.resource.DependentRefPropertyHandler;
import org.bsworks.x2.resource.ObjectPropertyHandler;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.RefPropertyHandler;
import org.bsworks.x2.resource.ResourcePropertiesContainer;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.TypePropertyHandler;
import org.bsworks.x2.resource.annotations.AggregateProperty;
import org.bsworks.x2.resource.annotations.DependentRefProperty;
import org.bsworks.x2.resource.annotations.IdProperty;
import org.bsworks.x2.resource.annotations.MetaProperty;
import org.bsworks.x2.resource.annotations.Persistence;
import org.bsworks.x2.resource.annotations.Property;
import org.bsworks.x2.resource.annotations.TypeProperty;
import org.bsworks.x2.resource.annotations.ValueType;
import org.bsworks.x2.util.AnnotationUtils;
import org.bsworks.x2.util.StringUtils;


/**
 * Resource properties container implementation.
 *
 * @param <O> Type of the object that contains the properties.
 *
 * @author Lev Himmelfarb
 */
class ResourcePropertiesContainerImpl<O>
	implements ResourcePropertiesContainer {

	/**
	 * Resource property annotations.
	 */
	private static final List<Class<? extends Annotation>> ANNOS;
	static {
		ANNOS = new ArrayList<>(6);
		ANNOS.add(IdProperty.class);
		ANNOS.add(MetaProperty.class);
		ANNOS.add(TypeProperty.class);
		ANNOS.add(Property.class);
		ANNOS.add(DependentRefProperty.class);
		ANNOS.add(AggregateProperty.class);
	}

	/**
	 * Property path pattern.
	 */
	private static final Pattern PROP_PATH_PATTERN =
		Pattern.compile("[a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*");


	/**
	 * Application resources manager.
	 */
	private final ResourcesImpl resources;

	/**
	 * Container class.
	 */
	private final Class<O> containerClass;

	/**
	 * Property name if container is a nested object property, or {@code null}
	 * if it is a resource.
	 */
	private final String propName;

	/**
	 * Prefix for the instance cache key.
	 */
	private final String cacheKeyPrefix;

	/**
	 * Concrete type name, or {@code null} if does not represent a concrete
	 * value of a polymorphic object.
	 */
	private final String concreteTypeName;

	/**
	 * Name of the persistent collection used to store persistent properties in
	 * this container, or {@code null} if not persistent.
	 */
	private final String persistentCollectionName;

	/**
	 * Name of the persistent field used to store the id of the record that is
	 * this container's parent, or {@code null} if not persistent, embedded or
	 * top-level persistent resource.
	 */
	private final String parentIdPersistentFieldName;

	/**
	 * Cache of property chains.
	 */
	private final ConcurrentMap<String, Deque<ResourcePropertyHandler>>
	propChains = new ConcurrentHashMap<>();

	/**
	 * All properties.
	 */
	private final Map<String, AbstractResourcePropertyHandlerImpl> props;

	/**
	 * Record id property, or {@code null} if none.
	 */
	private final IdPropertyHandlerImpl idProp;

	/**
	 * Polymorphic object type property, or {@code null} if none.
	 */
	private final TypePropertyHandlerImpl typeProp;

	/**
	 * Simple properties.
	 */
	private final Collection<SimplePropertyHandlerImpl> simpleProps;

	/**
	 * Object properties.
	 */
	private final Collection<ObjectPropertyHandlerImpl> objectProps;

	/**
	 * Reference properties.
	 */
	private final Collection<RefPropertyHandlerImpl> refProps;


	/**
	 * Create container handler for the specified container class.
	 *
	 * @param resources Reference to the resources manager.
	 * @param prsrcClasses All persistent resource classes.
	 * @param containerClass The container class.
	 * @param propName Property name if container is a nested object property,
	 * or {@code null} if it is a resource.
	 * @param concreteTypeName Concrete type name, or {@code null} if does not
	 * represent a concrete value of a polymorphic object.
	 * @param persistentCollectionName Name of the persistent collection used to
	 * store persistent properties in the container class, or {@code null} if
	 * not persistent.
	 * @param parentIdPersistentFieldName Name of the persistent field used to
	 * store the id of the record that is this container's parent, or
	 * {@code null} if not persistent, embedded or top-level persistent
	 * resource.
	 * @param persistentFieldsPrefix If persistent collection is specified,
	 * prefix for any persistent fields in the container class. Empty string for
	 * no prefix. Ignored if {@code persistentCollectionName} argument is
	 * {@code null}.
	 * @param parentPersistentFieldsPrefix Parent fields prefix used for the top
	 * polymorphic object.
	 *
	 * @throws IllegalArgumentException If something is wrong with the specified
	 * container class.
	 */
	ResourcePropertiesContainerImpl(final ResourcesImpl resources,
			final Set<Class<?>> prsrcClasses, final Class<O> containerClass,
			final String propName,
			final String concreteTypeName,
			final String persistentCollectionName,
			final String parentIdPersistentFieldName,
			final String persistentFieldsPrefix,
			final String parentPersistentFieldsPrefix) {

		// get the log
		final Log log = LogFactory.getLog(ResourcesImpl.class);
		final boolean debug = log.isDebugEnabled();

		// save the basics
		this.resources = resources;
		this.containerClass = containerClass;
		this.propName = propName;
		this.concreteTypeName = concreteTypeName;
		this.persistentCollectionName = persistentCollectionName;
		this.parentIdPersistentFieldName = parentIdPersistentFieldName;

		// introspect the container class
		if (debug)
			log.debug("introspecting " + this.containerClass.getName());
		final PropertyDescriptor[] pds;
		try {
			final BeanInfo bi =
				Introspector.getBeanInfo(this.containerClass, Object.class);
			pds = bi.getPropertyDescriptors();
		} catch (final IntrospectionException e) {
			throw new IllegalArgumentException("Class "
					+ this.containerClass.getName() + " is not introspectable.",
					e);
		}

		// create property collections
		final Map<String, AbstractResourcePropertyHandlerImpl> props =
			new HashMap<>(pds.length > 16 ? pds.length : 16);
		IdPropertyHandlerImpl idProp = null;
		TypePropertyHandlerImpl typeProp = null;
		final int listCap = (pds.length > 10 ? pds.length : 10);
		final ArrayList<SimplePropertyHandlerImpl> simpleProps =
			new ArrayList<>(listCap);
		final ArrayList<ObjectPropertyHandlerImpl> objectProps =
			new ArrayList<>(listCap);
		final ArrayList<RefPropertyHandlerImpl> refProps =
			new ArrayList<>(listCap);

		// create handlers for the object properties
		for (final PropertyDescriptor pd : pds) {

			// get property annotation
			Annotation propAnno = null;
			for (final Class<? extends Annotation> annoClass : ANNOS) {
				final Annotation a = AnnotationUtils.getPropertyAnnotation(
						annoClass, pd, this.containerClass);
				if (a == null)
					continue;
				if (propAnno != null)
					throw new IllegalArgumentException("Resource property "
							+ pd.getName() + " in class "
							+ this.containerClass.getName()
							+ " has conflicting annotations.");
				propAnno = a;
			}

			// check if annotated resource property
			if (propAnno == null)
				continue;
			if (debug)
				log.debug("    * property: " + pd.getName()
						+ " (" + pd.getPropertyType().getName() + ")");

			// get property value Java type
			final Method getter = pd.getReadMethod();
			final Type valueJavaType = (getter != null
					? getter.getGenericReturnType()
					: pd.getWriteMethod().getGenericParameterTypes()[0]);

			// create value handler
			final AbstractResourcePropertyValueHandlerImpl valueHandler =
				createValueHandler(resources, prsrcClasses, this.containerClass,
						pd, propAnno, 0, false, valueJavaType,
						this.persistentCollectionName);

			// create property handler
			final AbstractResourcePropertyHandlerImpl propHandler =
				ResourcePropertyHandlerFactory.createHandler(prsrcClasses,
						this.containerClass, pd, propAnno, valueHandler,
						this.persistentCollectionName, persistentFieldsPrefix,
						parentPersistentFieldsPrefix);

			// validate persistent map reference key
			if (propHandler.getPersistence() != null) {
				final CanBeMapKeyResourcePropertyValueHandler keyHandler =
					propHandler.getKeyValueHandler();
				if ((keyHandler instanceof RefResourcePropertyValueHandler)
						&& ((RefResourcePropertyValueHandler) keyHandler)
								.isWildcard())
					throw new IllegalArgumentException(
							"Persistent map property " + pd.getName() + " of "
							+ this.containerClass.getName()
							+ " cannot use a wildcard reference as a key.");
			}

			// place handler in the corresponding property collections
			props.put(propHandler.getName(), propHandler);
			if (propHandler instanceof IdPropertyHandlerImpl) {
				if (idProp != null)
					throw new IllegalArgumentException("Class "
							+ this.containerClass.getName()
							+ " defines more than one record id property.");
				idProp = (IdPropertyHandlerImpl) propHandler;
			} else if (propHandler instanceof TypePropertyHandlerImpl) {
				if (typeProp != null)
					throw new IllegalArgumentException("Class "
							+ this.containerClass.getName()
							+ " defines more than one polymorphic object type"
							+ " property.");
				typeProp = (TypePropertyHandlerImpl) propHandler;
			} else if (propHandler instanceof SimplePropertyHandlerImpl)
				simpleProps.add((SimplePropertyHandlerImpl) propHandler);
			else if (propHandler instanceof ObjectPropertyHandlerImpl)
				objectProps.add((ObjectPropertyHandlerImpl) propHandler);
			else if (propHandler instanceof RefPropertyHandlerImpl)
				refProps.add((RefPropertyHandlerImpl) propHandler);
		}

		// store property collections
		this.props = Collections.unmodifiableMap(new HashMap<>(props));
		this.idProp = idProp;
		this.typeProp = typeProp;
		simpleProps.trimToSize();
		this.simpleProps = Collections.unmodifiableList(simpleProps);
		objectProps.trimToSize();
		this.objectProps = Collections.unmodifiableList(objectProps);
		refProps.trimToSize();
		this.refProps = Collections.unmodifiableList(refProps);

		// determine cache key prefix
		this.cacheKeyPrefix =
			(this.propName != null ?
					this.propName : this.containerClass.getSimpleName())
			+ (this.idProp != null ? "#" : "");
	}

	/**
	 * Create resource property value handler.
	 *
	 * @param resources Reference to the resources manager.
	 * @param prsrcClasses All persistent resource classes.
	 * @param containerClass Class that contains the property.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param collectionLevel Level in the value type chain.
	 * @param map {@code true} if map property.
	 * @param valueJavaType Property value Java reflection type.
	 * @param ctxPersistentCollectionName Context persistent collection name, or
	 * {@code null} if none.
	 *
	 * @return Resource property value handler.
	 *
	 * @throws IllegalArgumentException If the type is invalid (that is
	 * unsupported).
	 */
	private static AbstractResourcePropertyValueHandlerImpl createValueHandler(
			final ResourcesImpl resources, final Set<Class<?>> prsrcClasses,
			final Class<?> containerClass, final PropertyDescriptor pd,
			final Annotation propAnno, final int collectionLevel,
			final boolean map, final Type valueJavaType,
			final String ctxPersistentCollectionName) {

		// check if dynamic value
		if ((valueJavaType instanceof WildcardType)
				|| valueJavaType.equals(Object.class))
			return new DynamicResourcePropertyValueHandler(resources);

		// get property value type category
		final ResourcePropertyValueType valueType = getValueType(valueJavaType);
		if (valueType == null)
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " uses unsupported type " + valueJavaType + ".");

		// analyze returned value type and recurse into multi-valued types
		AbstractResourcePropertyValueHandlerImpl elValueHandler;
		switch (valueType) {
		case STRING:
			return StringResourcePropertyValueHandler.INSTANCE;
		case BYTE:
			return (((Class<?>) valueJavaType).isPrimitive()
						? ByteResourcePropertyValueHandler.INSTANCE_PRIM
						: ByteResourcePropertyValueHandler.INSTANCE_REF);
		case SHORT:
			return (((Class<?>) valueJavaType).isPrimitive()
					? ShortResourcePropertyValueHandler.INSTANCE_PRIM
					: ShortResourcePropertyValueHandler.INSTANCE_REF);
		case INTEGER:
			return (((Class<?>) valueJavaType).isPrimitive()
					? IntegerResourcePropertyValueHandler.INSTANCE_PRIM
					: IntegerResourcePropertyValueHandler.INSTANCE_REF);
		case LONG:
			return (((Class<?>) valueJavaType).isPrimitive()
					? LongResourcePropertyValueHandler.INSTANCE_PRIM
					: LongResourcePropertyValueHandler.INSTANCE_REF);
		case BIG_DECIMAL:
			return BigDecimalResourcePropertyValueHandler.INSTANCE;
		case FLOAT:
			return (((Class<?>) valueJavaType).isPrimitive()
					? FloatResourcePropertyValueHandler.INSTANCE_PRIM
					: FloatResourcePropertyValueHandler.INSTANCE_REF);
		case DOUBLE:
			return (((Class<?>) valueJavaType).isPrimitive()
					? DoubleResourcePropertyValueHandler.INSTANCE_PRIM
					: DoubleResourcePropertyValueHandler.INSTANCE_REF);
		case BOOLEAN:
			return (((Class<?>) valueJavaType).isPrimitive()
					? BooleanResourcePropertyValueHandler.INSTANCE_PRIM
					: BooleanResourcePropertyValueHandler.INSTANCE_REF);
		case DATE:
			return DateResourcePropertyValueHandler.INSTANCE;
		case ENUM:
			return new EnumResourcePropertyValueHandler(
					(Class<?>) valueJavaType);
		case OBJECT:
			if (!(propAnno instanceof Property))
				throw new IllegalArgumentException("Nested object property "
						+ pd.getName() + " of " + containerClass.getName()
						+ " has invalid resource property annotation.");
			return createObjectValueHandler(resources, prsrcClasses,
					containerClass, (Class<?>) valueJavaType, pd,
					(Property) propAnno, collectionLevel, map,
					ctxPersistentCollectionName);
		case REF:
			final Type refTargetJavaType =
				((ParameterizedType) valueJavaType).getActualTypeArguments()[0];
			final boolean wildcardRef;
			final Class<?> refTargetClass;
			if (refTargetJavaType instanceof Class) {
				wildcardRef = false;
				refTargetClass = (Class<?>) refTargetJavaType;
				if (!prsrcClasses.contains(refTargetClass))
					throw new IllegalArgumentException("Target of reference"
							+ " property " + pd.getName() + " of "
							+ containerClass.getName()
							+ " is not a persistent resource.");
			} else if (refTargetJavaType instanceof WildcardType) {
				final Type[] bounds =
					((WildcardType) refTargetJavaType).getUpperBounds();
				if ((bounds.length != 1) || !(bounds[0] instanceof Class))
					throw new IllegalArgumentException("Target of reference"
							+ " property " + pd.getName() + " of "
							+ containerClass.getName()
							+ " has invalid type " + refTargetJavaType + ".");
				wildcardRef = true;
				final Class<?> boundClass = (Class<?>) bounds[0];
				refTargetClass = (boundClass.isAssignableFrom(Object.class) ?
						null : boundClass);
			} else // should not ever happen
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " uses unsupported type " + valueJavaType + ".");
			return new RefResourcePropertyValueHandler(resources,
					refTargetClass, wildcardRef);
		case LIST:
			elValueHandler = createValueHandler(resources, prsrcClasses,
					containerClass, pd, propAnno, collectionLevel + 1, false,
					((ParameterizedType) valueJavaType)
						.getActualTypeArguments()[0],
					ctxPersistentCollectionName);
			return new ListResourcePropertyValueHandler(elValueHandler,
					elValueHandler.getCollectionDegree() + 1);
		case SET:
			elValueHandler = createValueHandler(resources, prsrcClasses,
					containerClass, pd, propAnno, collectionLevel + 1, false,
					((ParameterizedType) valueJavaType)
						.getActualTypeArguments()[0],
					ctxPersistentCollectionName);
			return new SetResourcePropertyValueHandler(elValueHandler,
					elValueHandler.getCollectionDegree() + 1);
		case MAP:
			final Type keyJavaType =
				((ParameterizedType) valueJavaType).getActualTypeArguments()[0];
			final AbstractResourcePropertyValueHandlerImpl keyValueHandler =
					createValueHandler(resources, prsrcClasses,
							containerClass, pd, propAnno, collectionLevel + 1,
							false, keyJavaType, null);
			if (!(keyValueHandler instanceof
					CanBeMapKeyResourcePropertyValueHandler))
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " uses unsupported map key type " + keyJavaType
						+ ".");
			elValueHandler = createValueHandler(resources, prsrcClasses,
					containerClass, pd, propAnno, collectionLevel + 1, true,
					((ParameterizedType) valueJavaType)
						.getActualTypeArguments()[1],
					ctxPersistentCollectionName);
			return new MapResourcePropertyValueHandler(
					(CanBeMapKeyResourcePropertyValueHandler) keyValueHandler,
					elValueHandler,
					elValueHandler.getCollectionDegree() + 1);
		default: // cannot happen
			throw new IllegalArgumentException("Property " + pd.getName()
					+ " of " + containerClass.getName()
					+ " uses unsupported type " + valueJavaType + ".");
		}
	}

	/**
	 * Get property value type.
	 *
	 * @param valueJavaType Property value Java reflection type.
	 *
	 * @return Property value type, or {@code null} if the specified Java
	 * reflection type is invalid (that is unsupported).
	 */
	private static ResourcePropertyValueType getValueType(
			final Type valueJavaType) {

		if (valueJavaType instanceof Class) {
			final Class<?> valueClass = (Class<?>) valueJavaType;
			if ((String.class).equals(valueClass))
				return ResourcePropertyValueType.STRING;
			if ((Integer.class).equals(valueClass)
					|| (valueClass == Integer.TYPE))
				return ResourcePropertyValueType.INTEGER;
			if ((Short.class).equals(valueClass)
					|| (valueClass == Short.TYPE))
				return ResourcePropertyValueType.SHORT;
			if ((Byte.class).equals(valueClass)
					|| (valueClass == Byte.TYPE))
				return ResourcePropertyValueType.BYTE;
			if ((Long.class).equals(valueClass)
					|| (valueClass == Long.TYPE))
				return ResourcePropertyValueType.LONG;
			if ((Boolean.class).equals(valueClass)
					|| (valueClass == Boolean.TYPE))
				return ResourcePropertyValueType.BOOLEAN;
			if ((BigDecimal.class).equals(valueClass))
				return ResourcePropertyValueType.BIG_DECIMAL;
			if ((Double.class).equals(valueClass)
					|| (valueClass == Double.TYPE))
				return ResourcePropertyValueType.DOUBLE;
			if ((Float.class).equals(valueClass)
					|| (valueClass == Float.TYPE))
				return ResourcePropertyValueType.FLOAT;
			if ((Date.class).equals(valueClass))
				return ResourcePropertyValueType.DATE;
			if (valueClass.isEnum())
				return ResourcePropertyValueType.ENUM;
			if (!valueClass.isPrimitive()
					&& !valueClass.isArray()
					&& !valueClass.isAnnotation()
					&& !valueClass.equals(Object.class))
				return ResourcePropertyValueType.OBJECT;
		} else if (valueJavaType instanceof ParameterizedType) {
			final ParameterizedType valueJavaPType =
				(ParameterizedType) valueJavaType;
			final Type valueRawType = valueJavaPType.getRawType();
			if (valueRawType instanceof Class<?>) {
				final Class<?> valueClass = (Class<?>) valueRawType;
				if ((Ref.class).equals(valueClass))
					return ResourcePropertyValueType.REF;
				if ((List.class).equals(valueClass)
						|| (ArrayList.class).equals(valueClass))
					return ResourcePropertyValueType.LIST;
				if ((Set.class).equals(valueClass)
						|| (HashSet.class).equals(valueClass))
					return ResourcePropertyValueType.SET;
				if ((Map.class).equals(valueClass)
						|| (HashMap.class).equals(valueClass))
					return ResourcePropertyValueType.MAP;
			}
		}

		return null;
	}

	/**
	 * Create nested object value handler.
	 *
	 * @param resources Reference to the resources manager.
	 * @param prsrcClasses All persistent resource classes.
	 * @param containerClass Class that contains the nested object property.
	 * @param objClass The object class, for which to create a handler.
	 * @param pd Java bean property descriptor.
	 * @param propAnno Resource property annotation.
	 * @param collectionLevel Level in the value type chain.
	 * @param map {@code true} if map property.
	 * @param ctxPersistentCollectionName Context persistent collection name, or
	 * {@code null} if none.
	 *
	 * @return Value handler for the specified object class.
	 *
	 * @throws IllegalArgumentException If something is wrong with the specified
	 * object.
	 */
	private static ObjectResourcePropertyValueHandler createObjectValueHandler(
			final ResourcesImpl resources, final Set<Class<?>> prsrcClasses,
			final Class<?> containerClass, final Class<?> objClass,
			final PropertyDescriptor pd, final Property propAnno,
			final int collectionLevel, final boolean map,
			final String ctxPersistentCollectionName) {

		// check if existing resource
		final AbstractResourceHandlerImpl<?> existingRsrcHandler =
			resources.getRegisteredResourceHandler(objClass);
		if (existingRsrcHandler != null)
			return existingRsrcHandler.getResourceValueHandler();

		// get collection and fields prefix for the nested object properties
		final String persistentCollectionName;
		final String parentIdPersistentFieldName;
		final String persistentFieldsPrefix;
		final Persistence persistenceAnno = propAnno.persistence();
		if (persistenceAnno.persistent()) {
			if (ctxPersistentCollectionName == null)
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " cannot be persistent, because it is defined in a"
						+ " transient object.");
			persistentCollectionName = StringUtils.defaultIfEmpty(
					persistenceAnno.collection(), ctxPersistentCollectionName);
			parentIdPersistentFieldName =
				StringUtils.nullIfEmpty(persistenceAnno.parentIdField());
			persistentFieldsPrefix = persistenceAnno.field();
		} else { // object property is transient
			persistentCollectionName = null;
			parentIdPersistentFieldName = null;
			persistentFieldsPrefix = null;
		}

		// get object properties
		final ResourcePropertiesContainerImpl<?> props =
			new ResourcePropertiesContainerImpl<>(resources, prsrcClasses,
					objClass, pd.getName(), null, persistentCollectionName,
					parentIdPersistentFieldName, persistentFieldsPrefix,
					persistentFieldsPrefix);

		// check that we have an id if needed
		final boolean needsId = ((persistentCollectionName != null)
				&& !persistentCollectionName.equals(ctxPersistentCollectionName)
				&& (collectionLevel > 0) && !map);
		if (needsId && (props.getIdProperty() == null))
			throw new IllegalArgumentException("Nested object class "
					+ objClass.getName() + " needs a record id property.");

		// polymorphic property value types
		final Map<String, ResourcePropertiesContainerImpl<?>> valueTypes =
			new HashMap<>();
		for (final ValueType typeAnno : propAnno.valueTypes()) {
			final Class<?> vObjClass = typeAnno.concreteClass();
			if (!objClass.isAssignableFrom(vObjClass))
				throw new IllegalArgumentException("Property " + pd.getName()
						+ " of " + containerClass.getName()
						+ " specifies value class " + vObjClass.getName()
						+ " that is not a subclass of the property type.");
			final String vPersistentCollectionName =
				StringUtils.defaultIfEmpty(typeAnno.persistentCollection(),
						persistentCollectionName);
			final String vParentIdPersistentFieldName;
			final String vPersistentFieldsPrefix;
			if (persistenceAnno.persistent()) {
				vParentIdPersistentFieldName = StringUtils.defaultIfEmpty(
						typeAnno.parentIdPersistentField(),
						persistenceAnno.parentIdField());
				vPersistentFieldsPrefix = persistentFieldsPrefix
						+ typeAnno.persistentFieldsPrefix();
			} else {
				if (vPersistentCollectionName != null)
					throw new IllegalArgumentException("Value type "
							+ vObjClass.getName() + " of property "
							+ pd.getName() + " of " + containerClass.getName()
							+ " cannot be persistent, because the property is"
							+ " transient.");
				vParentIdPersistentFieldName = null;
				vPersistentFieldsPrefix = null;
			}
			final String typeName = StringUtils.defaultIfEmpty(
					typeAnno.name(), vObjClass.getSimpleName());
			final ResourcePropertiesContainerImpl<?> vProps =
				new ResourcePropertiesContainerImpl<>(resources, prsrcClasses,
						vObjClass, pd.getName(), typeName,
						vPersistentCollectionName, vParentIdPersistentFieldName,
						vPersistentFieldsPrefix, persistentFieldsPrefix);
			valueTypes.put(typeName, vProps);
		}

		// create and return value handler
		return new ObjectResourcePropertyValueHandler(props, valueTypes);
	}


	/**
	 * Get container class.
	 *
	 * @return The container class.
	 */
	Class<O> getContainerClass() {

		return this.containerClass;
	}

	/**
	 * Get property name if container is a nested object property.
	 *
	 * @return Property name, or {@code null} if it is a resource.
	 */
	String getPropertyName() {

		return this.propName;
	}

	/**
	 * Get prefix for the resource read session cache key.
	 *
	 * @return Cache key prefix.
	 */
	String getCacheKeyPrefix() {

		return this.cacheKeyPrefix;
	}

	/**
	 * Get concrete type name.
	 *
	 * @return Concrete type name, or {@code null} if the container does not
	 * represent a concrete value of a polymorphic object.
	 */
	String getConcreteTypeName() {

		return this.concreteTypeName;
	}

	/**
	 * Get name of the persistent collection used to store persistent properties
	 * in this container.
	 *
	 * @return Persistent collection name, or {@code null} if not persistent.
	 */
	String getPersistentCollectionName() {

		return this.persistentCollectionName;
	}

	/**
	 * Get name of the persistent field used to store the id of the record that
	 * is this container's parent.
	 *
	 * @return Persistent field name, or {@code null} if not persistent,
	 * embedded or top-level persistent resource.
	 */
	String getParentIdPersistentFieldName() {

		return this.parentIdPersistentFieldName;
	}

	/**
	 * Get polymorphic object value type property, if any.
	 *
	 * @return Type property handler, or {@code null} if none in the container.
	 */
	TypePropertyHandlerImpl getTypeProperty() {

		return this.typeProp;
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Deque<ResourcePropertyHandler> getPersistentPropertyChain(
			final String propPath) {

		// check if we have cached chain for this path
		final Deque<ResourcePropertyHandler> cachedRes =
			this.propChains.get(propPath);
		if (cachedRes != null)
			return cachedRes;

		// split path to parts
		if (!PROP_PATH_PATTERN.matcher(propPath).matches())
			throw new IllegalArgumentException("Invalid resource property path "
					+ propPath + ".");
		final String[] propPathParts = propPath.split("\\.");

		// process the path and build the chain
		final Deque<ResourcePropertyHandler> res = new LinkedList<>();
		ResourcePropertyHandler lastPropHandler = null;
		ResourcePropertiesContainer curContainer = this;
		for (int i = 0; i < propPathParts.length; i++) {

			// was last property a container?
			if (curContainer == null)
				throw new IllegalArgumentException(
						"Invalid resource property path " + propPath + ": "
							+ ResourcesImpl.chainToPath(res, null)
							+ " does not contain properties.");

			// get the property handler and validate it
			lastPropHandler =
				curContainer.getProperties().get(propPathParts[i]);
			final boolean typeProp =
				(lastPropHandler instanceof TypePropertyHandler);
			if ((lastPropHandler == null)
					|| (typeProp
							&& (curContainer instanceof ObjectPropertyHandler)
							&& ((ObjectPropertyHandler) curContainer)
								.isConcreteType())
					|| (!typeProp
							&& (lastPropHandler.getPersistence() == null)
							&& !(lastPropHandler
									instanceof AggregatePropertyHandler)))
				throw new IllegalArgumentException(
						"Invalid resource property path " + propPath + ": "
							+ ResourcesImpl.chainToPath(res, null)
							+ " does not contain persistent property "
							+ propPathParts[i] + ".");

			// add handler to the chain
			res.add(lastPropHandler);

			// get next container
			curContainer = null;
			if (lastPropHandler instanceof ObjectPropertyHandler) {

				curContainer = (ObjectPropertyHandler) lastPropHandler;

			} else if (lastPropHandler instanceof RefPropertyHandler) {

				final Class<?> refTargetClass =
					((RefPropertyHandler) lastPropHandler)
						.getReferredResourceClass();
				if ((refTargetClass != null)
						&& this.resources.isPersistentResource(
								refTargetClass))
					curContainer = this.resources.getPersistentResourceHandler(
							refTargetClass);

			} else if (lastPropHandler instanceof DependentRefPropertyHandler) {

				final Class<?> refTargetClass =
					((DependentRefPropertyHandler) lastPropHandler)
						.getReferredResourceClass();
				if ((refTargetClass != null)
						&& this.resources.isPersistentResource(
								refTargetClass))
					curContainer = this.resources.getPersistentResourceHandler(
							refTargetClass);
			}
		}

		// save the chain in the cache
		this.propChains.putIfAbsent(propPath, res);

		// return the chain
		// PROBLEM: there is no unmodifiable deque
		return res;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Map<String, AbstractResourcePropertyHandlerImpl> getProperties() {

		return this.props;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public IdPropertyHandlerImpl getIdProperty() {

		return this.idProp;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<SimplePropertyHandlerImpl> getSimpleProperties() {

		return this.simpleProps;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<ObjectPropertyHandlerImpl> getObjectProperties() {

		return this.objectProps;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public Collection<RefPropertyHandlerImpl> getRefProperties() {

		return this.refProps;
	}
}
