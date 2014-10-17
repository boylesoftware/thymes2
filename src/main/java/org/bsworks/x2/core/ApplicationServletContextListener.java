package org.bsworks.x2.core;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.bsworks.x2.InitializationException;


/**
 * Servlet context listener responsible for initialization and shutdown of the
 * application that uses the framework.
 *
 * @author Lev Himmelfarb
 */
@WebListener
public class ApplicationServletContextListener
	implements ServletContextListener {

	/**
	 * Name of servlet context attribute used to store the application instance.
	 */
	private static final String APP_ATTNAME =
		(ApplicationServletContextListener.class).getName() + ".APP";


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void contextInitialized(final ServletContextEvent sce) {

		final ServletContext sc = sce.getServletContext();
		try {
			sc.setAttribute(APP_ATTNAME, new Application(sc));
		} catch (final InitializationException e) {
			throw new RuntimeException("Application initialization error.", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent sce) {

		final ServletContext sc = sce.getServletContext();
		final Application app = (Application) sc.getAttribute(APP_ATTNAME);
		if (app != null) {
			sc.removeAttribute(APP_ATTNAME);
			app.shutdown(sc);
		}
	}


	/**
	 * Get application instance.
	 *
	 * @param sc Servlet context.
	 *
	 * @return Application instance, or {@code null} if has not been
	 * initialized.
	 */
	static Application getApplication(final ServletContext sc) {

		return (Application) sc.getAttribute(APP_ATTNAME);
	}
}
