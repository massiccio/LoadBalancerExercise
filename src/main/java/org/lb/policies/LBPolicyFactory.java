package org.lb.policies;

import org.lb.ProvidersManager;

public final class LBPolicyFactory {

	private LBPolicyFactory() {
		// prevents instantiation
	}
	
	public static ProvidersManager createRandomPolicy(int maxProviders) {
		return new RandomPolicy(maxProviders);
	}
	
	public static ProvidersManager createRoundRobinPolicy(int maxProviders) {
		return new RoundRobinPolicy(maxProviders);
	}
}
