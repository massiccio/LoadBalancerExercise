package org.lb;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.lb.provider.Provider;

public interface LoadBalancer {

	/**
	 * Forwards the request to one provider, if possible
	 * 
	 * @return The provider's id that executed the request, or
	 *         {@link Optional#empty()} if the system cannot execute the request or if
	 *         no provider is available.
	 */
	Optional<String> get();

	/**
	 * Registers a list of providers.
	 * 
	 * @param providers The list of providers to register.
	 * @return The number of registered providers.
	 */
	int register(List<Provider> providers);

	/**
	 * Start this load balancer. There is no side effect in calling this method
	 * multiple times.
	 */
	void start();

	/**
	 * Stops this load balancer. There is no side effect in calling this method
	 * multiple times.
	 */
	void stop();

	/** True if this load balancer was started, false, otherwise. */
	boolean isStarted();

	/**
	 * Include/exclude the provider indentified by the id.
	 * 
	 * @param id      the provider to include/exclude
	 * @param include true to include, false to exclude
	 * @return true if succeeded, false otherwise.
	 */
	boolean include(int id, boolean include);

	/**
	 * Statistics about served requests by each provider.
	 * 
	 * @return a map mapping provider IDs to the number of served requests.
	 */
	Map<Integer, Long> stats();

	/** Get the number of requests successfully served. */
	long getSuccess();

	/** Get the number of rejected requests. */
	long getRejected();

	/**
	 * Get the number of received requests. This is equivalent to
	 * {@link #getSuccess()} + {@link #getRejected()}.
	 */
	long getLoad();

}
