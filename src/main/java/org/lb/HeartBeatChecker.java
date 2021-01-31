package org.lb;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.lb.provider.Provider;

class HeartBeatChecker implements Runnable {
	
	private static Logger logger = Logger.getLogger(HeartBeatChecker.class.getName());

	private final LoadBalancerImpl lb;

	HeartBeatChecker(LoadBalancerImpl lb) {
		if (lb == null) {
			throw new IllegalArgumentException("Null load balancer");
		}
		this.lb = lb;
	}

	@Override
	public void run() {
		logger.info("Heartbeat task...");
		ArrayList<Provider> enabled = new ArrayList<>();
		
		// excluded providers are not considered
		this.lb.getProviders().stream().filter(x -> x.isIncluded()).forEach(x -> {
			if (x.check()) {
				// if the check succeeds, try to mark it as enabled
				if (x.enable()) {
					enabled.add(x);
				}
			} else { // health check failed, mark as disabled
				x.disable();
			}
		});
		
		// update the number of enabled providers
		this.lb.setEnabledProviders(enabled);
	}

}
