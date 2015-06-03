package org.bsworks.x2.app;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.bsworks.x2.Actor;
import org.bsworks.x2.EndpointCallContext;
import org.bsworks.x2.EndpointCallErrorException;
import org.bsworks.x2.EndpointCallHandler;
import org.bsworks.x2.HttpMethod;
import org.bsworks.x2.resource.FilterSpec;
import org.bsworks.x2.resource.OrderSpec;
import org.bsworks.x2.resource.PersistentResourceHandler;
import org.bsworks.x2.resource.PropertiesFetchSpec;
import org.bsworks.x2.resource.RangeResult;
import org.bsworks.x2.resource.RangeSpec;
import org.bsworks.x2.resource.RefsFetchResult;
import org.bsworks.x2.resource.RefsFetchSpec;
import org.bsworks.x2.resource.Resources;


/**
 * Persistent resource endpoint handler.
 *
 * <p>Any custom implementation of this interface must have a public constructor
 * that takes two arguments: the {@link ServletContext} and the
 * {@link Resources}.
 *
 * @param <R> Handled persistent resource type.
 *
 * @author Lev Himmelfarb
 */
public interface PersistentResourceEndpointHandler<R> {

	/**
	 * Get persistent resource class handler.
	 *
	 * @return The persistent resource class handler.
	 */
	PersistentResourceHandler<R> getPersistentResourceHandler();

	/**
	 * Get call handler for the specified HTTP request method. This method is
	 * called once for every {@link HttpMethod} value during the application
	 * initialization. The returned handlers are then associated with the URI
	 * pattern of the mapping, to which this persistent resource endpoint
	 * handler is assigned.
	 *
	 * @param requestMethod The HTTP request method.
	 *
	 * @return Endpoint call handler, or {@code null} if the endpoint does not
	 * support the specified HTTP request method.
	 */
	EndpointCallHandler<?> getCallHandler(HttpMethod requestMethod);

	/**
	 * Tell if the actor has sufficient permissions to perform a call. The
	 * method is supposed to be invoked from an
	 * {@link EndpointCallHandler#isAllowed(HttpMethod, String, List, Actor)},
	 * so that all endpoint call handlers connected to this handler have a
	 * central place for checking permissions.
	 *
	 * @param requestMethod Request method.
	 * @param requestURI Web-application context-relative request URI.
	 * @param uriParams Unmodifiable list of URI parameters.
	 * @param actor Actor making the call, or {@code null} for an
	 * unauthenticated request.
	 *
	 * @return {@code true} if the call is allowed.
	 */
	boolean isAllowed(HttpMethod requestMethod, String requestURI,
			List<String> uriParams, Actor actor);

	/**
	 * Search persistent resource records collection.
	 *
	 * @param ctx Endpoint call context.
	 * @param propsFetch Record properties fetch specification, or {@code null}
	 * for all properties.
	 * @param filter Records filter specification, or {@code null} for all
	 * records.
	 * @param order Result list order specification, or {@code null} for no
	 * particular order.
	 * @param range Collection range specification, or {@code null} for all
	 * records.
	 * @param rangeResult Object to receive range meta-data after the method
	 * call, or {@code null} if no range meta-data is needed.
	 * @param refsFetch Referred persistent resources fetch specification, or
	 * {@code null} for no referred resources need to be fetched.
	 * @param refsResult Object to received fetched referred resources, or
	 * {@code null} if the {@code refsFetch} argument is {@code null}.
	 *
	 * @return The records, or empty list if none found.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	List<R> search(EndpointCallContext ctx, PropertiesFetchSpec<R> propsFetch,
			FilterSpec<R> filter, OrderSpec<R> order, RangeSpec range,
			RangeResult rangeResult, RefsFetchSpec<R> refsFetch,
			RefsFetchResult refsResult)
		throws EndpointCallErrorException;

	/**
	 * Get filter for selecting a single record identified by the specified
	 * record id. The implementation may add more conditions besides the record
	 * id.
	 *
	 * @param ctx Endpoint call context.
	 * @param recId Record id.
	 *
	 * @return The filter.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	FilterSpec<R> getRecordFilter(EndpointCallContext ctx, Object recId)
		throws EndpointCallErrorException;

	/**
	 * Get existing persistent resource record by record id.
	 *
	 * @param ctx Endpoint call context.
	 * @param recId Record id.
	 * @param recFilter Filter returned by the
	 * {@link #getRecordFilter(EndpointCallContext, Object)} method.
	 * @param propsFetch Record properties fetch specification, or {@code null}
	 * for all properties.
	 * @param lock If {@code true}, the method also places a shared lock on the
	 * returned record.
	 *
	 * @return The record, or {@code null} if does not exists.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	R get(EndpointCallContext ctx, Object recId, FilterSpec<R> recFilter,
			PropertiesFetchSpec<R> propsFetch, boolean lock)
		throws EndpointCallErrorException;

	/**
	 * Create new persistent resource record.
	 *
	 * @param ctx Endpoint call context.
	 * @param recTmpl The record template. The object is updated by the handler,
	 * which, at a minimum, sets the new record id in it.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	void create(EndpointCallContext ctx, R recTmpl)
		throws EndpointCallErrorException;

	/**
	 * Update existing persistent resource record. The existing record is loaded
	 * first, then, those fields that are different, are updated from the
	 * provided new record data.
	 *
	 * @param ctx Endpoint call context.
	 * @param rec The existing record. The object is updated by the handler,
	 * including record version and last modification meta-properties, if any
	 * are defined for the persistent resource.
	 * @param recTmpl New record data.
	 *
	 * @return Names of the record properties that were updated. Empty if
	 * nothing was updated.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	Set<String> update(EndpointCallContext ctx, R rec, R recTmpl)
		throws EndpointCallErrorException;

	/**
	 * Delete existing persistent resource record. If record does not exist,
	 * nothing is done.
	 *
	 * @param ctx Endpoint call context.
	 * @param recId Record id.
	 * @param recFilter Filter returned by the
	 * {@link #getRecordFilter(EndpointCallContext, Object)} method.
	 *
	 * @return Collection of classes of all persistent resources, records of
	 * which were actually deleted. If record existed and was deleted, the set
	 * will include the persistent resource handled by this handler, as well as
	 * any referred dependent resources that were deleted as a result of this
	 * operation. If record did not exist, empty set is returned.
	 *
	 * @throws EndpointCallErrorException To send an error response back to the
	 * client.
	 */
	Set<Class<?>> delete(EndpointCallContext ctx, Object recId,
			FilterSpec<R> recFilter)
		throws EndpointCallErrorException;
}
