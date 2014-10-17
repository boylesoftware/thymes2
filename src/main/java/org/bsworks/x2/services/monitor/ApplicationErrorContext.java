package org.bsworks.x2.services.monitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Application error context.
 *
 * @author Lev Himmelfarb
 */
public class ApplicationErrorContext
	implements Serializable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Process name.
	 */
	private final String processName;

	/**
	 * Context sections.
	 */
	private final List<ApplicationErrorContextSection> sections =
		new ArrayList<>();

	/**
	 * Read-only view of the context sections.
	 */
	private final List<ApplicationErrorContextSection> sectionsRO =
		Collections.unmodifiableList(this.sections);


	/**
	 * Create new empty context.
	 */
	public ApplicationErrorContext() {

		this.processName = Thread.currentThread().getName();
	}


	/**
	 * Add context properties section.
	 *
	 * @param title Section title.
	 *
	 * @return The new section with no properties associated with it.
	 */
	public ApplicationErrorContextSection addSection(final String title) {

		final ApplicationErrorContextSection section =
			new ApplicationErrorContextSection(title);
		this.sections.add(section);

		return section;
	}


	/**
	 * Get name of the internal process, in which the error happened. The
	 * process name is the name of the thread, that called the context
	 * constructor.
	 *
	 * @return The process name.
	 */
	public String getProcessName() {

		return this.processName;
	}

	/**
	 * Get context sections.
	 *
	 * @return Unmodifiable list of the sections.
	 */
	public List<ApplicationErrorContextSection> getSections() {

		return this.sectionsRO;
	}
}
