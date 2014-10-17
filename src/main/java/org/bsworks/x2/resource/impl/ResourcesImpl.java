package org.bsworks.x2.resource.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.bsworks.x2.InitializationException;
import org.bsworks.x2.resource.InvalidResourceDataException;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.ResourcePropertyHandler;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.resource.annotations.PersistentResource;


/**
 * Application resources manager implementation.
 *
 * @author Lev Himmelfarb
 */
public class ResourcesImpl
	implements Resources {

	/**
	 * Persistent application resource classes.
	 */
	private final Set<Class<?>> prsrcClasses;

	/**
	 * Application resource handlers by resource classes.
	 */
	private final ConcurrentMap<Class<?>, AbstractResourceHandlerImpl<?>>
	resources;

	/**
	 * Persistent application resource handlers by resource class simple names.
	 */
	private final Map<String, PersistentResourceHandlerImpl<?>>
	persistentResources;


	/**
	 * Create and initialize resources manger.
	 *
	 * @param sc Web-application servlet context.
	 *
	 * @throws InitializationException If an error happens during
	 * initialization.
	 */
	public ResourcesImpl(final ServletContext sc)
		throws InitializationException {

		final Log log = LogFactory.getLog(this.getClass());
		final boolean debug = log.isDebugEnabled();

		// load persistent resources
		this.prsrcClasses = getPersistentResourceClasses(log, sc);
		final int numPRsrcs = this.prsrcClasses.size();
		if (numPRsrcs > 0) {
			this.resources = new ConcurrentHashMap<>(numPRsrcs + numPRsrcs/2);
			this.persistentResources = new HashMap<>(numPRsrcs);
			for (final Class<?> prsrcClass : this.prsrcClasses) {
				final PersistentResource prsrcAnno =
					prsrcClass.getAnnotation(PersistentResource.class);
				if (prsrcAnno == null)
					throw new InitializationException("Persistent resource"
							+ " class " + prsrcClass.getName()
							+ " does not have a @"
							+ (PersistentResource.class).getSimpleName()
							+ " annotation.");
				if (debug)
					log.debug("found persistent resource "
							+ prsrcClass.getName());
				final PersistentResourceHandlerImpl<?> rsrcHandler =
					new PersistentResourceHandlerImpl<>(this, this.prsrcClasses,
							prsrcClass, prsrcAnno);
				this.resources.put(prsrcClass, rsrcHandler);
				if (this.persistentResources.put(prsrcClass.getSimpleName(),
						rsrcHandler) != null)
					throw new InitializationException("More than one persistent"
							+ " application resource class share simple name "
							+ prsrcClass.getSimpleName() + ".");
			}
		} else {
			if (debug)
				log.debug("no persistent application resources found");
			this.resources = new ConcurrentHashMap<>();
			this.persistentResources = Collections.emptyMap();
		}

		// setup dependent resource references persistence
		for (final PersistentResourceHandlerImpl<?> prsrcHandler :
				this.persistentResources.values()) {
			for (final DependentRefPropertyHandlerImpl propHandler :
					prsrcHandler.getDependentRefProperties()) {
				propHandler.setPersistence(this.persistentResources.get(
						propHandler.getReferredResourceClass()
							.getSimpleName()));
			}
		}
	}

	/**
	 * Find all persistent application resource classes.
	 *
	 * @param sc Web-application servlet context.
	 * @param log The log.
	 *
	 * @return Unmodifiable collection of all found persistent application
	 * resource classes. May be empty, but never {@code null}. All returned
	 * classes have {@link PersistentResource} annotation.
	 *
	 * @throws InitializationException If an error happens searching for the
	 * persistent resources.
	 */
	private static Set<Class<?>> getPersistentResourceClasses(final Log log,
			final ServletContext sc)
		throws InitializationException {

		final boolean debug = log.isDebugEnabled();

		// classes collector
		final Set<Class<?>> prsrcClasses = new HashSet<>();

		// get and validate resource packages
		final String[] prsrcPackages =
			sc.getInitParameter(PERSISTENT_RESOURCE_PACKAGES_INITPARAM)
				.trim().split("\\s+");
		if ((prsrcPackages.length == 0)
				|| ((prsrcPackages.length == 1) && prsrcPackages[0].isEmpty()))
			return prsrcClasses;
		final Pattern packagePattern = Pattern.compile(
				"[a-z]\\w*(?:\\.[a-z]\\w*)*", Pattern.CASE_INSENSITIVE);
		final Matcher m = packagePattern.matcher("");
		final Set<String> packageNames = new HashSet<>();
		for (final String packageName : prsrcPackages) {
			if (!m.reset(packageName).matches())
				throw new InitializationException("Invalid persistent resources"
						+ " package name " + packageName + ".");
			packageNames.add(packageName);
		}

		// scan the jars
		final String jarsPatternStr =
			sc.getInitParameter(PERSISTENT_RESOURCE_JARS_PATTERN_INITPARAM)
				.trim();
		if (!jarsPatternStr.isEmpty()) {
			final Pattern jarsPattern = Pattern.compile(jarsPatternStr);
			final Matcher jarsPatternMatcher = jarsPattern.matcher("");
			for (final String path : sc.getResourcePaths("/WEB-INF/lib")) {
				if (!path.endsWith(".jar") || !jarsPatternMatcher.reset(
						path.substring("/WEB-INF/lib/".length())).matches())
					continue;
				if (debug)
					log.debug("scanning " + path);
				try (final JarInputStream jar = new JarInputStream(
						sc.getResourceAsStream(path))) {
					for (JarEntry entry = jar.getNextJarEntry(); entry != null;
							entry = jar.getNextJarEntry()) {
						final String entryName = entry.getName();
						if (!entryName.endsWith(".class")
								|| (entryName.indexOf('$') >= 0))
							continue;
						final String className = entryName
							.substring(0,
									entryName.length() - ".class".length())
							.replace('/', '.');
						final int dotInd = className.lastIndexOf('.');
						if (dotInd <= 0)
							continue;
						final String packageName =
							className.substring(0, dotInd);
						if (!packageNames.contains(packageName))
							continue;
						final Class<?> cls = Class.forName(className);
						if (cls.getAnnotation(PersistentResource.class) == null)
							continue;
						prsrcClasses.add(cls);
					}
				} catch (final IOException | ClassNotFoundException e) {
					throw new InitializationException("Error scanning " + path
							+ " for persistent application resources.", e);
				}
			}
		}

		// scan the classes
		if (debug)
			log.debug("scanning classes");
		for (final String packageName : packageNames) {
			final Set<String> paths = sc.getResourcePaths("/WEB-INF/classes/"
					+ packageName.replace('.', '/'));
			if (paths == null)
				continue;
			for (final String path : paths) {
				if (!path.endsWith(".class") || (path.indexOf('$') >= 0))
					continue;
				final String className = path
					.substring("/WEB-INF/classes/".length(),
							path.length() - ".class".length())
					.replace('/', '.');
				final Class<?> cls;
				try {
					cls = Class.forName(className);
				} catch (final ClassNotFoundException e) {
					throw new InitializationException("Error loading persistent"
							+ " application resources.", e);
				}
				if (cls.getAnnotation(PersistentResource.class) == null)
					continue;
				prsrcClasses.add(cls);
			}
		}

		// return the results
		return prsrcClasses;
	}


	/**
	 * Get registered application resource handler.
	 *
	 * @param rsrcClass Application resource class.
	 *
	 * @return Application resource handler, or {@code null} if not registered.
	 */
	<R> AbstractResourceHandlerImpl<R> getRegisteredResourceHandler(
			final Class<R> rsrcClass) {

		@SuppressWarnings("unchecked") // we know it's for the same class
		AbstractResourceHandlerImpl<R> rsrcHandler =
			(AbstractResourceHandlerImpl<R>) this.resources.get(rsrcClass);

		return rsrcHandler;
	}

	/**
	 * Convert property chain to property path.
	 *
	 * @param chain Property chain.
	 * @param lastProp Last property to include in the path, or {@code null} to
	 * get the full path.
	 *
	 * @return Property path.
	 */
	static String chainToPath(
			final Deque<? extends ResourcePropertyHandler> chain,
			final ResourcePropertyHandler lastProp) {

		final StringBuilder res = new StringBuilder(128);
		for (final ResourcePropertyHandler prop : chain) {
			if (res.length() > 0)
				res.append(".");
			res.append(prop.getName());
			if (prop == lastProp)
				break;
		}

		return res.toString();
	}


	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> AbstractResourceHandlerImpl<R> getResourceHandler(
			final Class<R> rsrcClass) {

		AbstractResourceHandlerImpl<R> rsrcHandler =
			this.getRegisteredResourceHandler(rsrcClass);
		if (rsrcHandler == null) {
			rsrcHandler = new TransientResourceHandlerImpl<>(this,
					this.prsrcClasses, rsrcClass);
			this.resources.putIfAbsent(rsrcClass, rsrcHandler);
		}

		return rsrcHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public boolean isPersistentResource(final Class<?> prsrcClass) {

		return this.persistentResources.containsKey(prsrcClass.getSimpleName());
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> PersistentResourceHandlerImpl<R> getPersistentResourceHandler(
			final Class<R> prsrcClass) {

		@SuppressWarnings("unchecked") // we know it's for the same class
		final PersistentResourceHandlerImpl<R> prsrcHandler =
			(PersistentResourceHandlerImpl<R>) this.persistentResources.get(
					prsrcClass.getSimpleName());
		if (prsrcHandler == null)
			throw new IllegalArgumentException("Class " + prsrcClass.getName()
					+ " is not a valid persistent resource class.");

		return prsrcHandler;
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> RefsFetchSpecImpl<R> getRefsFetchSpec(
			final Class<R> prsrcClass) {

		return new RefsFetchSpecImpl<>(this, prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> PropertiesFetchSpecImpl<R> getPropertiesFetchSpec(
			final Class<R> prsrcClass) {

		return new PropertiesFetchSpecImpl<>(this, prsrcClass);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> FilterSpecImpl<R> getFilterSpec(final Class<R> prsrcClass) {

		return new FilterSpecImpl<>(
				this.getPersistentResourceHandler(prsrcClass), false, null);
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> OrderSpec<R> getOrderSpec(final Class<R> prsrcClass) {

		return new OrderSpecImpl<>(
				this.getPersistentResourceHandler(prsrcClass));
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public RefImpl<?> parseRef(final String refStr) {

		// parse the reference string
		final int hashInd = refStr.indexOf('#');
		if ((hashInd <= 0) || (hashInd >= refStr.length() - 1))
			throw new IllegalArgumentException("Invalid reference " + refStr
					+ ".");
		final String prsrcType = refStr.substring(0, hashInd);
		final String recIdStr = refStr.substring(hashInd + 1);

		// get the referred resource handler
		final PersistentResourceHandlerImpl<?> prsrcHandler =
			this.persistentResources.get(prsrcType);
		if (prsrcHandler == null)
			throw new IllegalArgumentException("Invalid reference " + refStr
					+ ".");

		// create and return the reference
		try {
			return new RefImpl<>(
					prsrcHandler.getResourceClass(),
					prsrcHandler.getIdProperty().getValueHandler().valueOf(
							recIdStr),
					refStr);
		} catch (final InvalidResourceDataException e) {
			throw new IllegalArgumentException("Invalid reference " + refStr
					+ ".", e);
		}
	}

	/* (non-Javadoc)
	 * See overridden method.
	 */
	@Override
	public <R> Ref<R> createRef(final Class<R> prsrcClass, final Object rec) {

		// check if null
		if (rec == null)
			throw new NullPointerException("Cannot create reference for null.");

		// get resource handler
		final PersistentResourceHandlerImpl<?> prsrcHandler =
			this.persistentResources.get(prsrcClass.getSimpleName());
		if (prsrcHandler == null)
			throw new IllegalArgumentException("Class "
					+ prsrcClass.getName()
					+ " is not a persistent resource class.");

		// check the object class
		if (!prsrcClass.isInstance(rec))
			throw new IllegalArgumentException("Object of "
					+ rec.getClass().getName()
					+ " is not an record of persistent resource "
					+ prsrcClass.getName() + ".");

		// get record id
		final Object recId = prsrcHandler.getIdProperty().getValue(rec);

		// create and return the reference
		return new RefImpl<>(prsrcClass, recId,
				prsrcClass.getSimpleName() + "#" + recId);
	}
}
