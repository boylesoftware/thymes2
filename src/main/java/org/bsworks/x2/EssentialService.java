package org.bsworks.x2;


/**
 * Marker interface for services that are used by the framework itself. These
 * services are available through individual, specialized getter methods on the
 * {@link RuntimeContext}, as opposed to any application extension services
 * available via {@link RuntimeContext#getService(Class)} method. Essential
 * services are initialized before the application services and the order, in
 * which they are initialized, is pre-determined.
 *
 * @author Lev Himmelfarb
 */
public interface EssentialService {

	// nothing
}
