package org.bsworks.x2.services.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Application error context property.
 *
 * @author Lev Himmelfarb
 */
public class ApplicationErrorContextProperty
	implements Serializable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Property name.
	 */
	private final String name;

	/**
	 * Property values.
	 */
	private final List<String> values = new ArrayList<>();

	/**
	 * Read-only view of the property values.
	 */
	private final List<String> valuesRO =
		Collections.unmodifiableList(this.values);


	/**
	 * Create new property.
	 *
	 * @param name Property name.
	 */
	ApplicationErrorContextProperty(final String name) {

		this.name = name;
	}


	/**
	 * Add value.
	 *
	 * @param value The value.
	 */
	public void addValue(final String value) {

		this.values.add(value);
	}


	/**
	 * Get property name.
	 *
	 * @return Property name.
	 */
	public String getName() {

		return this.name;
	}

	/**
	 * Get property values.
	 *
	 * @return Unmodifiable list of property values.
	 */
	public List<String> getValues() {

		return this.valuesRO;
	}
}
