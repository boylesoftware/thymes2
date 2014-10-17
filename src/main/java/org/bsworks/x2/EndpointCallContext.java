package org.bsworks.x2;

import java.util.Date;
import java.util.Set;

import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.Ref;
import org.bsworks.x2.resource.RefsFetchSpec;
import org.bsworks.x2.resource.Resources;
import org.bsworks.x2.services.auth.ActorAuthenticationService;
import org.bsworks.x2.services.persistence.PersistenceTransaction;
import org.bsworks.x2.services.versioning.PersistentResourceVersioningService;


/**
 * Endpoint call context.
 *
 * @author Lev Himmelfarb
 */
public interface EndpointCallContext {

	/**
	 * Get instance runtime context.
	 *
	 * @return The runtime context.
	 */
	RuntimeContext getRuntimeContext();

	/**
	 * Get call HTTP method.
	 *
	 * @return The HTTP method.
	 */
	HttpMethod getRequestMethod();

	/**
	 * Get web-application context path.
	 *
	 * @return Web-application context path, empty string for root context.
	 */
	String getRequestContextPath();

	/**
	 * Get request URI.
	 *
	 * @return Context-relative request URI.
	 */
	String getRequestURI();

	/**
	 * Get positional request URI parameter value.
	 *
	 * @param pos Zero-based placeholder position in the endpoint URI mapping.
	 * Negative position can be used to count the placeholders from the end: -1
	 * is the last parameter, -2 is the one before the last, etc.
	 *
	 * @return The parameter value, or {@code null} if not present.
	 *
	 * @throws IndexOutOfBoundsException If position does not match the number
	 * of placeholders in the endpoint URI mapping.
	 */
	String getRequestURIParam(int pos);

	/**
	 * Get HTTP request parameter value.
	 *
	 * @param name Parameter name.
	 *
	 * @return Parameter value, or {@code null} if not present.
	 */
	String getRequestParam(String name);

	/**
	 * Get multi-valued HTTP request parameter values.
	 *
	 * @param name Parameter name.
	 *
	 * @return The parameter values, or {@code null} if not present.
	 */
	String[] getRequestParamValues(String name);

	/**
	 * Get HTTP request header value.
	 *
	 * @param name Header name.
	 *
	 * @return Header value, or {@code null} if not present.
	 */
	String getRequestHeader(String name);

	/**
	 * Get HTTP request date header value.
	 *
	 * @param name Header name.
	 *
	 * @return Header value, or {@code null} if not present.
	 */
	Date getRequestDateHeader(String name);

	/**
	 * Set context attribute.
	 *
	 * @param name Attribute name.
	 * @param value Attribute value.
	 */
	void setAttribute(String name, Object value);

	/**
	 * Get context attribute.
	 *
	 * @param name Attribute name.
	 * @param valueClass Attribute value class.
	 *
	 * @return Attribute value, or {@code null}.
	 */
	<Y> Y getAttribute(String name, Class<Y> valueClass);

	/**
	 * Get actor making the call.
	 *
	 * @return The actor, or {@code null} if the call is unauthenticated.
	 */
	Actor getActor();

	/**
	 * Assume the current request is made by the specified actor. This method
	 * can be used by the web-application's user login handler after the user
	 * is successfully authenticated via the application's
	 * {@link ActorAuthenticationService} implementation.
	 *
	 * @param actor The authenticated actor, or {@code null} to make context
	 * unauthenticated (e.g. logout).
	 */
	void assumeActor(Actor actor);

	/**
	 * Tell if the actor associated with the context was assumed via
	 * {@link #assumeActor(Actor)} method.
	 *
	 * @return {@code true} if was assumed.
	 */
	boolean isAssumedActor();

	/**
	 * Get persistence transaction associated with the endpoint call. If
	 * transaction has not been started yet, start it. Otherwise, return
	 * existing transaction.
	 *
	 * <p>The transaction is committed automatically by the framework when the
	 * endpoint call handler returns. If the endpoint call handler throws an
	 * exception, the transaction is automatically rolled back.
	 *
	 * @return Persistence transaction.
	 */
	PersistenceTransaction getPersistenceTransaction();

	/**
	 * Submit a side-task for execution. This is a convenience shortcut method
	 * for {@link RuntimeContext#submitSideTask(Runnable)} with an additional
	 * feature of optional delaying the task submission until successful
	 * transaction commit.
	 *
	 * @param task The task.
	 * @param delayUntilCommit If {@code true}, the task is submitted for
	 * execution just after the persistence transaction associated with the
	 * context is committed and only if it is committed successfully.
	 */
	void submitSideTask(Runnable task, boolean delayUntilCommit);

	/**
	 * Create reference for the specified persistent resource record. This is a
	 * convenience shortcut method for
	 * {@link Resources#createRef(Class, Object)}.
	 *
	 * @param prsrcClass Persistent application resource class.
	 * @param rec The record.
	 *
	 * @return Record reference.
	 *
	 * @see Resources#createRef(Class, Object)
	 */
	<R> Ref<R> createRef(Class<R> prsrcClass, Object rec);

	/**
	 * Register persistent resource collections modification. This is a
	 * convenience shortcut method for
	 * {@link PersistentResourceVersioningService#registerCollectionsModification(PersistenceTransaction, Set)}.
	 *
	 * @param prsrcClasses Persistent resource classes.
	 */
	void registerPersistentResourceModification(Class<?>... prsrcClasses);

	/**
	 * Register persistent resource collections modification. This is a
	 * convenience shortcut method for
	 * {@link PersistentResourceVersioningService#registerCollectionsModification(PersistenceTransaction, Set)}.
	 *
	 * @param prsrcClasses Persistent resource classes.
	 */
	void registerPersistentResourceModification(Set<Class<?>> prsrcClasses);

	/**
	 * Put an exclusive lock on the specified persistent resource collection to
	 * prevent their modification for the duration of the persistence
	 * transaction associated with the context. This is a convenience shortcut
	 * method for
	 * {@link PersistentResourceVersioningService#lockCollections(PersistenceTransaction, Set)}.
	 *
	 * @param prsrcClasses Persistent resource classes.
	 */
	void lockPersistentResourceCollections(Set<Class<?>> prsrcClasses);

	/**
	 * Get empty referred persistent resource records fetch specification
	 * object. This is a convenience shortcut method for
	 * {@link Resources#getRefsFetchSpec(Class)}.
	 *
	 * @param prsrcClass Persistent application resource class.
	 *
	 * @return References fetch specification object.
	 */
	<R> RefsFetchSpec<R> getRefsFetchSpec(Class<R> prsrcClass);

	/**
	 * Get empty persistent resource properties fetch specification object. This
	 * is a convenience shortcut method for
	 * {@link Resources#getPropertiesFetchSpec(Class)}.
	 *
	 * @param prsrcClass Persistent application resource class.
	 *
	 * @return Properties fetch specification object initially in "exclude by
	 * default" mode.
	 */
	<R> PropertiesFetchSpec<R> getPropertiesFetchSpec(Class<R> prsrcClass);

	/**
	 * Get empty persistent resource fetch filter specification object. This is
	 * a convenience shortcut method for {@link Resources#getFilterSpec(Class)}.
	 *
	 * @param prsrcClass Persistent application resource class.
	 *
	 * @return Filter specification object.
	 */
	<R> FilterSpec<R> getFilterSpec(Class<R> prsrcClass);

	/**
	 * Get empty persistent resource fetch order specification object. This is a
	 * convenience shortcut method for {@link Resources#getOrderSpec(Class)}.
	 *
	 * @param prsrcClass Persistent application resource class.
	 *
	 * @return Order specification object.
	 */
	<R> OrderSpec<R> getOrderSpec(Class<R> prsrcClass);
}
