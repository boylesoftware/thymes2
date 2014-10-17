package org.bsworks.x2.services.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Section in an application error context.
 *
 * @author Lev Himmelfarb
 */
public class ApplicationErrorContextSection
	implements Serializable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Section title.
	 */
	private final String title;

	/**
	 * Context properties.
	 */
	private final List<ApplicationErrorContextProperty> properties =
		new ArrayList<>();

	/**
	 * Read-only view of the context properties.
	 */
	private final List<ApplicationErrorContextProperty> propertiesRO =
		Collections.unmodifiableList(this.properties);


	/**
	 * Create new section.
	 *
	 * @param title Section title.
	 */
	ApplicationErrorContextSection(final String title) {

		this.title = title;
	}


	/**
	 * Add context property to the section.
	 *
	 * @param name Property name. The name is supposed to be unique, but the
	 * method does not perform any checks.
	 *
	 * @return The new context property with no values associated with it.
	 */
	public ApplicationErrorContextProperty addProperty(final String name) {

		final ApplicationErrorContextProperty property =
			new ApplicationErrorContextProperty(name);
		this.properties.add(property);

		return property;
	}


	/**
	 * Get section title.
	 *
	 * @return Section title.
	 */
	public String getTitle() {

		return this.title;
	}

	/**
	 * Get context properties contained in the section.
	 *
	 * @return Unmodifiable list of application error context properties.
	 */
	public List<ApplicationErrorContextProperty> getProperties() {

		return this.propertiesRO;
	}
}
