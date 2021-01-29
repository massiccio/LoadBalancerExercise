package org.lb.policies;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.lb.ProvidersManager;
import org.lb.provider.Provider;

/**
 * Round robin policy.
 */
class RoundRobinPolicy extends ProvidersManager implements LBPolicy {

	private AtomicLong counter;

	public RoundRobinPolicy(int maxProviders) {
		super(maxProviders);
		counter = new AtomicLong(0L);
		logger.info("Using round robin policy");
	}

	@Override
	public Optional<Provider> getProvider() {
		super.enabledLock.readLock().lock();
		try {
			final int enabled = super.enabledProviders();
			if (enabled > 0) {
				int index = (int) (this.counter.incrementAndGet() % enabled);
				var provider = super.enabled.get(index);
				return Optional.of(provider);
			}
			return Optional.empty();
		} finally {
			super.enabledLock.readLock().unlock();
		}
	}

}
