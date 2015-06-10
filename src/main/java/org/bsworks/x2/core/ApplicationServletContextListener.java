package org.bsworks.x2.core;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.bsworks.x2.Actor;
import org.bsworks.x2.InitializationException;
import org.bsworks.x2.RuntimeContext;


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

	/**
	 * Get application runtime context. This method can be used to get access to
	 * the framework from outside of endpoint call handlers: for example, from
	 * servlets and filters.
	 *
	 * @param sc Servlet context.
	 *
	 * @return Runtime context, or {@code null} if the framework has not been
	 * initialized.
	 */
	public static RuntimeContext getRuntimeContext(final ServletContext sc) {

		final Application app = getApplication(sc);
		if (app == null)
			return null;

		return app.getRuntimeContext();
	}

	/**
	 * Get actor making the request. This method is intended to be solely used
	 * from application components working outside of the framework stack
	 * (endpoint call handlers), such as custom application servlets and
	 * filters. Together with {@link #getRuntimeContext(ServletContext)} this
	 * method provides such servlets and filters access to the framework
	 * functionality, including the persistent storage, etc.
	 *
	 * @param httpRequest The HTTP request.
	 *
	 * @return The actor, or {@code null} if the request does not contain
	 * authentication information, or the authentication information is invalid,
	 * expired, or refers to a nonexistent or inactive actor.
	 */
	public static Actor getActor(final HttpServletRequest httpRequest) {

		final Application app = getApplication(httpRequest.getServletContext());
		if (app == null)
			return null;

		return app.getRuntimeContext().getAuthTokenHandler().getActor(
				httpRequest);
	}
}
