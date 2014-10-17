package org.bsworks.x2.resource;


/**
 * Resource property persistence descriptor.
 *
 * @author Lev Himmelfarb
 */
public interface ResourcePropertyPersistence {

	/**
	 * Get name of the persistent field used to store the property value.
	 *
	 * <p>For a nested object property that is stored in its own collection
	 * ({@link #getCollectionName()} does not return {@code null}), the name
	 * returned by this method is not used.
	 *
	 * @return The field name.
	 */
	String getFieldName();

	/**
	 * Get name of the persistent collection used to store property values.
	 *
	 * @return The collection name, or {@code null} if the property values are
	 * stored in the same collection as the object that contains the property.
	 */
	String getCollectionName();

	/**
	 * Name of the persistent field in the collection returned by
	 * {@link #getCollectionName()} that stores the id of the containing record.
	 *
	 * @return The field name, or {@code null} if {@link #getCollectionName()}
	 * returns {@code null}.
	 */
	String getParentIdFieldName();

	/**
	 * Name of the persistent field in the collection returned by
	 * {@link #getCollectionName()} that stores the map key value.
	 *
	 * @return The field name, or {@code null} if {@link #getCollectionName()}
	 * returns {@code null} or the property is not a map.
	 */
	String getKeyFieldName();

	/**
	 * Tells if existence of the property value or values is optional.
	 *
	 * @return {@code true} if optional.
	 */
	boolean isOptional();
}
