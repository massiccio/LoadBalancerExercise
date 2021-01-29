package org.lb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lb.policies.LBPolicyFactory;
import org.lb.provider.DefaultProvider;
import org.lb.provider.Provider;

class RoundRobinPolicyTest {
	
	private ProvidersManager policy;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		//
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		//
	}

	@BeforeEach
	void setUp() throws Exception {
		this.policy = LBPolicyFactory.createRoundRobinPolicy(3);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.policy = null; 
	}

	@Test
	void testGetProvider1() {
		assertTrue(this.policy.getProvider().isEmpty());
		// registering a provider is not enough, it must be enabled!
		this.policy.register(new DefaultProvider());
		assertTrue(this.policy.getProvider().isEmpty());
	}
	
	@Test
	void testGetProvider2() {
		var provider1 = new DefaultProvider();
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		this.policy.setEnabledProviders(list);
		assertTrue(this.policy.getProvider().isPresent());
	}
	
	
	@Test
	void testGetProvider3() {
		var provider1 = new DefaultProvider();
		var provider2 = new DefaultProvider();
		var provider3 = new DefaultProvider();
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		list.add(provider2);
		list.add(provider3);
		this.policy.setEnabledProviders(list);
		assertEquals(3, this.policy.enabledProviders());
		assertTrue(this.policy.getProvider().isPresent());
		
		// test that the random does not always choose the same provider
		final int reps = 3000;
		int prov1 = 0;
		int prov2 = 0;
		int prov3 = 0;
		for (int i = 0; i < reps; i++) {
			var tmp = this.policy.getProvider();
			int id = tmp.orElseThrow().getId();
			if (id == provider1.getId()) {
				prov1++;
			} else if (id == provider2.getId()) {
				prov2++;
			} else {
				prov3++;
			}
		}
		
		assertEquals(reps / 3, prov1);
		assertEquals(reps / 3, prov2);
		assertEquals(reps / 3, prov3);
	}
	
	@Test
	void testGetProvider4() {
		var provider1 = new DefaultProvider();
		var provider2 = new DefaultProvider();
		var provider3 = new DefaultProvider();
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		list.add(provider2);
		list.add(provider3);
		this.policy.setEnabledProviders(list);
		assertEquals(3, this.policy.enabledProviders());
		assertTrue(this.policy.getProvider().isPresent());
		
		final int nThreads = 5;
		CountDownLatch startSignal = new CountDownLatch(1);
		CountDownLatch doneSignal = new CountDownLatch(nThreads);
		
		for (int i = 0; i < nThreads; i++) {
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						startSignal.await();
						for (int i = 0; i < 100_000; i++) {
							policy.getProvider().orElseThrow().get();
						}
						doneSignal.countDown();
					} catch (InterruptedException e) {
						//
					}
					
					
				}
			});
			t.start();
		}
		
		// start all threads at the same time
		startSignal.countDown();
		while (doneSignal.getCount() > 0L) {
			try {
				doneSignal.await();
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
		// check that load balancing works as expected, 1% margin of error
		long prov1 = provider1.getRequests();
		long prov2 = provider2.getRequests();
		long prov3 = provider3.getRequests();
		
		long min = Math.min(prov1, prov2);
		min = Math.min(min, prov3);
		long max = Math.max(prov1, prov2);
		max = Math.max(max, prov3);
		
		assertTrue(min >= 0.99 * max);
	}

}
