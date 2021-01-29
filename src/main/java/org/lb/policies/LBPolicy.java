package org.lb.policies;


import java.util.Optional;

import org.lb.provider.Provider;

/**
 * Load balancing policy.
 */
public interface LBPolicy {
    
    /**
     * Chooses the provider to forward the request to.
     */
    Optional<Provider> getProvider();
}
