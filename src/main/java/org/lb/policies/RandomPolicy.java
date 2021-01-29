package org.lb.policies;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.lb.ProvidersManager;
import org.lb.provider.Provider;

/**
 * Stateless policy that chooses the first enabled provider (step 3, page 4) which can
 * accept traffic.
 */
class RandomPolicy extends ProvidersManager implements LBPolicy {

    /*
     * An alternative may use a random object (possibly bound to a thread local variable)
     * to pick a random value, and stop after len(providers) attempts.
     */ 

    protected RandomPolicy(int maxProviders) {
		super(maxProviders);
		logger.info("Using random policy");
	}

	@Override
    public Optional<Provider> getProvider() {
		super.enabledLock.readLock().lock();
		try {
			final int enabled = super.enabledProviders();
			if (enabled > 0) {
				int index = ThreadLocalRandom.current().nextInt(enabled);
				return Optional.of(super.enabled.get(index));
			}
			return Optional.empty();
		} finally {
			super.enabledLock.readLock().unlock();
		}
    }
}
