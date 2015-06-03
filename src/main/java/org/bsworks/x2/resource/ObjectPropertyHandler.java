package org.bsworks.x2.resource;


/**
 * Nested object resource property handler.
 *
 * @author Lev Himmelfarb
 */
public interface ObjectPropertyHandler
	extends ResourcePropertyHandler, ResourcePropertiesContainer {

	/**
	 * Get class of the persistent resource that owns the nested object records,
	 * if property is part of a persistent resource.
	 *
	 * @return Owning persistent resource class, or {@code null} if not part of
	 * a persistent resource.
	 */
	Class<?> getOwningPersistentResourceClass();

	/**
	 * Tells if nested object property is owned by another persistent resource.
	 *
	 * @return {@code true} if borrowed.
	 */
	boolean isBorrowed();

	/**
	 * Get object value class.
	 *
	 * <p>For a polymorphic object property, get the common superclass. For a
	 * concrete value handler, get the concrete class.
	 *
	 * @return The object value class.
	 */
	Class<?> getObjectClass();

	/**
	 * Get handler of the polymorphic object type property if this is a handler
	 * of a polymorphic nested object.
	 *
	 * <p>Polymorphic object property handler is special. As a container, it
	 * provides the record id property (if any) via {@link #getIdProperty()},
	 * the concrete value type property via {@link #getTypeProperty()}, and it
	 * lists concrete value type handlers as nested object properties via
	 * {@link #getObjectProperties()}. All other container property access
	 * methods return nothing. The handlers returned by
	 * {@link #getObjectProperties()} are all single-valued and the property
	 * names are concrete value type names. They cannot be used to set property
	 * values, but their get property value methods returns the object itself if
	 * its concrete type is that of the handler's. Otherwise, {@code null} is
	 * returned.
	 *
	 * @return Polymorphic object concrete value type property handler, or
	 * {@code null} if this is not a polymorphic object property handler.
	 */
	TypePropertyHandler getTypeProperty();

	/**
	 * Tell if the handler is for a concrete value type of a polymorphic object
	 * property.
	 *
	 * @return {@code true} if concrete value type handler.
	 */
	boolean isConcreteType();
}
