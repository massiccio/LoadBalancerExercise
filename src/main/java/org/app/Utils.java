package org.app;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.lb.LoadBalancer;

public class Utils {

	private static Logger logger = Logger.getLogger(Utils.class.getName());
	
	
	private Utils() {
		// avoid instantiation
	}
	
	public static void printSummary(LoadBalancer lb) {
		StringBuilder sb = new StringBuilder();
		sb.append("Statistics:").append("\n");
		sb.append("- Load: ").append(lb.getLoad()).append("\n");
		sb.append("- Rejected requests: ").append(lb.getRejected()).append("\n");
		sb.append("- Served requests: ").append(lb.getSuccess()).append("\n");
		sb.append("Per provider statistics:").append("\n");
		lb.stats().forEach((k, v) -> {
			sb.append("- Provider ").append(k).append(": ").append(v).append("\n");
		});
		
		logger.info(sb.toString());
	}
	
	
	public static void startThreads(LoadBalancer lb, int nThreads, int requests, CountDownLatch startSignal,
			CountDownLatch doneSignal) {
		for (int i = 0; i < nThreads; i++) {
			String name = "Thread_" + i;
			Thread t = new Thread(new Runnable() {
				
				int success = 0;
				int fail = 0;

				@Override
				public void run() {
					try {
						startSignal.await();
						for (int i = 0; i < requests; i++) {
							if (lb.get().isPresent()) {
								success++;
							} else {
								fail++;
							}
						}
					} catch (InterruptedException e) {
						//
					} finally {
						doneSignal.countDown();
						
						StringBuilder sb = new StringBuilder(Thread.currentThread().getName());
						sb.append(" completed, ").append("success: ").append(success).append(", failed: ").append(fail);
						logger.info(sb.toString());
					}
				}
			}, name);
			t.start();
		}
	}
	
	
	public static void waitCompletion(CountDownLatch doneSignal) {
		while (doneSignal.getCount() > 0L) {
			try {
				doneSignal.await();
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
	}

}
