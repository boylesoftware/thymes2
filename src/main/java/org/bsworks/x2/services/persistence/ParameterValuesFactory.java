package org.bsworks.x2.services.persistence;


/**
 * Query parameter value handlers factory.
 *
 * @param <V> Produced parameter value handlers type.
 *
 * @author Lev Himmelfarb
 */
public interface ParameterValuesFactory<V extends ParameterValue> {

	/**
	 * Get parameter value handler.
	 *
	 * @param type Value type.
	 * @param value The value.
	 *
	 * @return The value handler.
	 */
	V getParameterValue(PersistentValueType type, Object value);
}
