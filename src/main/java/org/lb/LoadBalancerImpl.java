package org.lb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.lb.policies.LBPolicyFactory;
import org.lb.provider.Provider;

public class LoadBalancerImpl implements LoadBalancer {

	/** Maximum number of providers. */
	private static final int MAX_PROVIDERS = 10;

	private static final int HEARTBEAT_INTERVAL_SECONDS = 2;

	private static Logger logger = Logger.getLogger(LoadBalancerImpl.class.getName());

	/** Capacity of each provider. */
	private final int maxLoad;

	/** Load balancing policy */
	private final ProvidersManager manager;

	/** Track metrics. */
	private Metrics metrics;

	private ScheduledExecutorService heartBeatExecutorService;

	/** True if this load balancer was started, false otherwise. */
	private AtomicBoolean started;

	/**
	 * Create a load balancer with a maximum capacity of 10 providers.
	 * 
	 * @param maxJobsPerProvider Provider capacity.
	 * @throws IllegalArgumentException If maxJobsPerProvider <= 0.
	 */
	public LoadBalancerImpl(int maxLoad) {
		this(MAX_PROVIDERS, maxLoad);
	}

	public LoadBalancerImpl(int maxLoad, ProvidersManager lbPolicy) {
		if (maxLoad < 1) {
			throw new IllegalArgumentException("maxJobsPerProvider must be positive");
		}
		if (lbPolicy == null) {
			throw new NullPointerException("Null policy");
		}
		this.maxLoad = maxLoad;
		this.manager = lbPolicy;
		init();
	}

	private void init() {
		metrics = new Metrics();

		heartBeatExecutorService = Executors.newSingleThreadScheduledExecutor();
		started = new AtomicBoolean(false);
	}

	/**
	 * Create a load balancer with the desired capacity using round robin policy.
	 * 
	 * @param maxProviders The capacity. Must be strictly positive.
	 * @param maxLoad      Provider capacity.
	 * @param lbPolicy     The load balancing policy. If null, use a round robin
	 *                     policy.
	 * @throws IllegalArgumentException If maxProviders <= 0 or maxJobsPerProvider
	 *                                  <= 0.
	 */
	protected LoadBalancerImpl(int maxProviders, int maxLoad) {
		if (maxProviders < 1) {
			throw new IllegalArgumentException("maxProviders must be positive");
		}
		if (maxLoad < 1) {
			throw new IllegalArgumentException("maxJobsPerProvider must be positive");
		}
		this.maxLoad = maxLoad;
		this.manager = LBPolicyFactory.createRoundRobinPolicy(maxProviders);

		init();
	}

	private boolean isSystemOverloaded(long pending) {
		// In order to reduce contention this check might be slightly off as the provider manager
		// uses a cached value inside enabledProviders()
		final int enabled = this.manager.enabledProviders();
		if (pending >= enabled * this.maxLoad)  {
			return true;
		}
		return false;
	}

	/**
	 * Serve a request.
	 * 
	 * @throws IllegalStateException If the load balancer was not started first. See {@link #start()
	 * @throws OverloadException If the system cannot handle the request.
	 */
	@Override
	public Optional<String> get() {
		if (isStarted() == false) {
			this.metrics.reject();
			logger.severe("Load balancer not started yet. Call start() first");
//			throw new IllegalStateException();
			return Optional.empty();
		}

		final long curPending = this.metrics.getPending();
		if (isSystemOverloaded(curPending)) {
			// step 8, page 9
			this.metrics.reject();
//			throw new OverloadException("System overloaded, pending requests: " + curPending);
			return Optional.empty();
		}
		// The load balancing policy tries to get a provider.
		// Deal with scenario when some providers are disabled
		Optional<Provider> provider = this.manager.getProvider();
		if (provider.isEmpty()) {
			// step 8, page 9: deal with scenario where the hearbeat removes all nodes after
			// the check above
			this.metrics.reject();
			logger.warning("No provider found");
//			throw new OverloadException("System overloaded, no provider available.");
			return Optional.empty();
		}

		// increment # of jobs in the system, handle the request, and decrese # of jobs
		// in the system
		this.metrics.increasePending();
		String result = provider.get().get();
		// request succeeded
		this.metrics.success();
		this.metrics.decreasePending();
		// return result
		return Optional.of(result);
	}

	/**
	 * Register the providers.
	 * <p>
	 * This implementation may register only a sublist of the providers, for example
	 * when the number of providers is larger than the maximum number of allowed
	 * providers.
	 * <p>
	 * Other implementations may decide to either succeed (all providers can be
	 * registered) or fail (no provider is registered) instead.
	 * 
	 * @return The number of registered providers, a number >= 0.
	 */
	public int register(List<Provider> providers) {
		if (providers == null || providers.isEmpty()) {
			return 0;
		}

		int counter = 0;
		for (Provider provider : providers) {
			if (this.manager.register(provider)) {
				counter++;
			}
		}
		return counter;
	}

	void setEnabledProviders(ArrayList<Provider> enabledProviders) {
		this.manager.setEnabledProviders(enabledProviders);
	}

	/**
	 * Get a shallow copy of the registered providers.
	 * <p>
	 * If {@link #register(List)} is invoked after {@link #getProviders()}, the new
	 * provider(s) won't be included into this list.
	 */
	List<Provider> getProviders() {
		// return a shallow copy.
		return this.manager.getRegisteredProviders();
	}

	public void start() {
		// Steps 6 and 7
		if (this.started.compareAndSet(false, true)) {
			this.heartBeatExecutorService.scheduleWithFixedDelay(new HeartBeatChecker(this), 0L,
					HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

//			Runtime.getRuntime().addShutdownHook(new Thread() {
//				@Override
//				public void run() {
//					logger.info("Shutdown hook cleaning");
//					LoadBalancerImpl.this.stop();
//				}
//			});

			logger.info("Load balancer started.");
		}
	}

	public void stop() {
		if (this.started.compareAndSet(true, false)) {
			this.heartBeatExecutorService.shutdown();

			logger.warning("Load balancer stopped.");
		}
	}

	@Override
	public boolean include(int id, boolean include) {
		return this.manager.include(id, include);
	}

	@Override
	public Map<Integer, Long> stats() {
		return this.manager.statistics();
	}
	
	@Override
	public long getLoad() {
		return this.metrics.getArrivals();
	}
	
	@Override
	public long getRejected() {
		return this.metrics.getRejected();
	}
	
	@Override
	public long getSuccess() {
		return this.metrics.getSuccess();
	}

	@Override
	public boolean isStarted() {
		return this.started.get();
	}
}
