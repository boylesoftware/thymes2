package org.bsworks.x2.resource;


/**
 * Property value transformation function.
 *
 * @author Lev Himmelfarb
 */
public enum PropertyValueFunction {

	/**
	 * Plain property value (no transformation function).
	 */
	PLAIN(new Class<?>[] {}),

	/**
	 * Value string representation length.
	 */
	LENGTH(new Class<?>[] {}),

	/**
	 * Value string representation's substring.
	 */
	SUBSTRING(new Class<?>[] { Integer.class, Integer.class }),

	/**
	 * Value string representation padded on the left side to the specified
	 * total length using the specified padding character.
	 */
	LPAD(new Class<?>[] { Integer.class, Character.class });


	/**
	 * Function parameter types.
	 */
	private final Class<?>[] _paramTypes;


	/**
	 * Create new value.
	 *
	 * @param paramTypes Function parameter types.
	 */
	private PropertyValueFunction(final Class<?>[] paramTypes) {

		this._paramTypes = paramTypes;
	}


	/**
	 * Get function parameter types.
	 *
	 * @return Array of classes that define the number and types of the function
	 * parameters. Can be empty, but never {@code null}.
	 */
	public Class<?>[] paramTypes() {

		return this._paramTypes;
	}
}
