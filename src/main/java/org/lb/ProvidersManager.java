package org.lb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.lb.policies.LBPolicy;
import org.lb.provider.Provider;

public abstract class ProvidersManager implements LBPolicy {

	protected static Logger logger = Logger.getLogger(ProvidersManager.class.getName());

	/** Map of registered providers. */
	private Map<Integer, Provider> registered;

	private final ReadWriteLock registeredLock;

	/**
	 * List of enabled providers.
	 * <p>
	 * This list gets overwritten after every health check. This enables O(1)
	 * scheduling of providers as well as addition/removal (new providers being
	 * added, providers failing, and providers being included/excluded manually).
	 */
	protected ArrayList<Provider> enabled;

	/** Cache the number of enabled providers. */
	private volatile int enabledProviders;

	protected final ReadWriteLock enabledLock;

	/** Maximum number of providers. */
	protected final int maxProviders;

	protected ProvidersManager(int maxProviders) {
		registered = new HashMap<>(maxProviders);
		enabled = new ArrayList<>();
		enabledProviders = 0;
		registeredLock = new ReentrantReadWriteLock();
		enabledLock = new ReentrantReadWriteLock();

		this.maxProviders = maxProviders;
	}

	private Provider getProvider(int id) {
		this.registeredLock.readLock().lock();
		try {
			return this.registered.get(id);
		} finally {
			this.registeredLock.readLock().unlock();
		}
	}

	boolean include(int id, boolean include) {
		Provider provider = getProvider(id);
		boolean res = false;
		if (provider != null) {
			provider.include(include);
			res = true;
		}
		return res;
	}

	/**
	 * Register the provider, if possible.
	 * 
	 * @return true if the provider was registered, false otherwise.
	 */
	boolean register(Provider provider) {
		this.registeredLock.writeLock().lock();
		try {
			if (this.registered.size() >= this.maxProviders) {
				return false;
			}
			return this.registered.putIfAbsent(provider.getId(), provider) == null;
		} finally {
			this.registeredLock.writeLock().unlock();
		}
	}

	public int enabledProviders() {
//		this.enabledLock.readLock().lock();
//		try {
//			return this.enabled.size();
//		} finally {
//			this.enabledLock.readLock().unlock();
//		}
		return this.enabledProviders; // volatile variable
	}

	List<Provider> getRegisteredProviders() {
		// shallow copy
		this.registeredLock.readLock().lock();
		try {
			return new ArrayList<>(registered.values());
		} finally {
			this.registeredLock.readLock().unlock();
		}
	}

	void setEnabledProviders(ArrayList<Provider> enabled) {
		this.enabledLock.writeLock().lock();
		try {
			this.enabled = enabled;
			this.enabledProviders = this.enabled.size();
		} finally {
			this.enabledLock.writeLock().unlock();
		}

		final int size = enabled.size();
		if (size == 0) {
			logger.warning("There are no enabled providers.");
		} else {
			if (size == 1) {
				logger.info("1 provider is enabled");
			} else {
				logger.info(size + " providers are enabled.");
			}
		}
	}

	int registeredProviders() {
		this.registeredLock.readLock().lock();
		try {
			return this.registered.size();
		} finally {
			this.registeredLock.readLock().unlock();
		}
	}

	Map<Integer, Long> statistics() {
		Map<Integer, Long> map = new HashMap<>();
		this.registeredLock.readLock().lock();
		try {
			this.registered.values().forEach(x -> map.put(x.getId(), x.getRequests()));
		} finally {
			this.registeredLock.readLock().unlock();
		}
		return map;
	}
}
