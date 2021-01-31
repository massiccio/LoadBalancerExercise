package org.lb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.app.Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.lb.policies.LBPolicyFactory;
import org.lb.provider.DefaultProvider;
import org.lb.provider.Provider;

class LoadBalancerImplTest {
	
	private static Logger logger = Logger.getLogger(LoadBalancerImplTest.class.getName());

	private static final int MAX_PROVIDERS = 10;

	private static final int MAX_LOAD = 3;

	private static final long SLOW_PROVIDER_GET_TIMEOUT = 1_000L;

	private LoadBalancerImpl lb;

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
		this.lb = new LoadBalancerImpl(MAX_LOAD, LBPolicyFactory.createRoundRobinPolicy(MAX_PROVIDERS));
	}

	@AfterEach
	void tearDown() throws Exception {
		this.lb.stop();
		this.lb = null;
	}
		

	@Test
	void wrongConstructor1() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new LoadBalancerImpl(0);
		});
	}

	@Test
	void wrongConstructor2() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			new LoadBalancerImpl(2, null);
		});
	}
	
	
	@Test
	void testFaultyProvider1() throws InterruptedException {
		var list = new ArrayList<Provider>(1);
		var faulty = new FaultyProvider();
		list.add(faulty);
		
		this.lb.register(list);
		this.lb.start();
		Thread.sleep(500L);
		
		assertFalse(lb.get().isPresent());
		
		faulty.checkValue = true;
		Thread.sleep(2500L);
		assertFalse(lb.get().isPresent());
		Thread.sleep(2500L);
//		assertFalse(lb.get().isPresent());
//		Thread.sleep(2500L);
		assertTrue(lb.get().isPresent());
	}

	@Test
	void testGetNotStarted() {
		// lb not started
//		Assertions.assertThrows(IllegalStateException.class, () -> {
//			this.lb.get();
//		});
		assertTrue(this.lb.get().isEmpty());
	}

	@Test
	void testGetNoProviders() {
		this.lb.start();
		// no providers enabled
//		Assertions.assertThrows(OverloadException.class, () -> {
//			this.lb.get();
//		});
		assertTrue(this.lb.get().isEmpty());
	}

	@Test
	@Timeout(20)
	void testOverload() throws InterruptedException {
		ProvidersManager policy = LBPolicyFactory.createRandomPolicy(1);
		LoadBalancerImpl myLb = new LoadBalancerImpl(1, policy);
		myLb.register(createListWithOneSlowProvider(SLOW_PROVIDER_GET_TIMEOUT));
		myLb.start();

		Thread.sleep(500L); // wait till heartbeat completes

		CountDownLatch startSignal = new CountDownLatch(1);
		CountDownLatch doneSignal = new CountDownLatch(3);
		
		Utils.startThreads(myLb, 3, 10, startSignal, doneSignal);

		// start all threads at the same time
		startSignal.countDown();
		logger.info("Threads started, waiting for them to complete");
		Utils.waitCompletion(doneSignal);
		logger.info("Threads completed");
		myLb.stop();
		
		assertTrue(myLb.getLoad() > 0);
		assertEquals(myLb.getLoad(), myLb.getSuccess() + myLb.getRejected());
		assertTrue(myLb.getRejected() > 0);
		assertTrue(myLb.getSuccess() > 0);
		Utils.printSummary(myLb);

	}

	private static ArrayList<Provider> createListWithOneProvider() {
		ArrayList<Provider> list = new ArrayList<>();
		list.add(new DefaultProvider());
		return list;
	}

	private static ArrayList<Provider> createListWithOneSlowProvider(long timeout) {
		ArrayList<Provider> list = new ArrayList<>();
		list.add(new SlowProvider(timeout));
		return list;
	}

	@Test
	void testGet() throws InterruptedException {
		var list = createListWithOneProvider();
		this.lb.register(list);

		this.lb.start();

		Thread.sleep(500L); // wait till hearbeat completes

		assertEquals(this.lb.get().get(), list.get(0).get());
	}

	@Test
	void testRegister() {
		List<Provider> list = new ArrayList<>(MAX_PROVIDERS);
		for (int i = 0; i < MAX_PROVIDERS; i++) {
			list.add(new DefaultProvider());
		}
		assertEquals(MAX_PROVIDERS, this.lb.register(list));
		// can't register any more providers
		list.clear();
		for (int i = 0; i < 3; i++) {
			list.add(new DefaultProvider());
		}
		assertEquals(0, this.lb.register(list));
	}

	@Test
	void testSetEnabledProviders() throws InterruptedException {
		this.lb.start(); // prevent illegal state exception
		Thread.sleep(500L); // ensure the background task doesn't remove the enabled providers
//		assertThrows(OverloadException.class, () -> {
//			this.lb.get(); // no enabled provider
//		});
		assertTrue(this.lb.get().isEmpty());
		
		var list = createListWithOneProvider();
		assertEquals(1, list.size());
		this.lb.setEnabledProviders(list);
		assertTrue(this.lb.get().isPresent());
	}

	@Test
	void testGetProviders1() {
		assertEquals(0, this.lb.getProviders().size());
	}
	
	@Test
	void testGetProviders2() {
		var list1 = createListWithOneProvider(); // shallow copy only
		this.lb.register(list1);
		var list2 = this.lb.getProviders();
		assertEquals(1, list2.size());
		assertEquals(list1, list2);
	}

	@Test
	void testStart() throws InterruptedException {
		this.lb.register(createListWithOneProvider());
		assertFalse(this.lb.isStarted());
		this.lb.start();
		Thread.sleep(500L); // wait till provider is enabled
		
		assertTrue(this.lb.isStarted());
		assertTrue(this.lb.get().isPresent()); // try to get one request through the system
		this.lb.start(); // no side effect
		this.lb.start();
		this.lb.start();
		assertTrue(this.lb.isStarted());
	}

	@Test
	void testStop() throws InterruptedException {
		assertFalse(this.lb.isStarted());
		this.lb.register(createListWithOneProvider());
		this.lb.start();
		Thread.sleep(500L); // wait till provider is enabled
		
		assertTrue(this.lb.isStarted());
		this.lb.stop();
		assertFalse(this.lb.isStarted());
		this.lb.stop(); // no side effect
		this.lb.stop();
		this.lb.stop();
		assertFalse(this.lb.isStarted());
		assertEquals(Optional.empty(), this.lb.get());
//		assertThrows(IllegalStateException.class, () -> {
//			this.lb.get(); // system is stopped
//		});
	}

	@Test
	void testInclude() {
		assertFalse(this.lb.include(1, true)); // not found
		
		var list = createListWithOneProvider();
		this.lb.register(list);
		assertTrue(this.lb.include(list.get(0).getId(), false));
		assertTrue(this.lb.include(list.get(0).getId(), true));
		
		assertFalse(this.lb.include(-1, false)); // using negative id, so for sure it won't find it
		assertFalse(this.lb.include(-2, true));
	}

	@Test
	void testStats1() {
		assertNotNull(this.lb.stats());
	}
	
	@Test
	void testStats2() {
		this.lb.register(createListWithOneProvider());
		assertEquals(1, this.lb.stats().size());
	}

}
