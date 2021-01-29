package org.lb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lb.policies.LBPolicyFactory;
import org.lb.provider.DefaultProvider;
import org.lb.provider.Provider;

class RandomPolicyTest {
	
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
		this.policy = LBPolicyFactory.createRandomPolicy(3);
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
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		list.add(provider2);
		this.policy.setEnabledProviders(list);

		assertEquals(2, this.policy.enabledProviders());
		assertTrue(this.policy.getProvider().isPresent());
		
		// test that the random does not always choose the same provider
		final int reps = 1000;
		int prov1 = 0;
		int prov2 = 0;
		for (int i = 0; i < reps; i++) {
			var tmp = this.policy.getProvider();
			int id = tmp.orElseThrow().getId();
			if (id == provider1.getId()) {
				prov1++;
			} else {
				prov2++;
			}
		}
		
		assertTrue(prov1  > 0);
		assertTrue(prov2  > 0);
		assertEquals(prov1 + prov2, reps);
	}

	@Test
	void testInclude1() {
		assertFalse(this.policy.include(1, false));
		assertFalse(this.policy.include(1, true));
	}
	
	@Test
	void testInclude2() {
		var provider = new DefaultProvider();
		this.policy.register(provider);
		assertTrue(this.policy.include(provider.getId(), false));
		assertTrue(this.policy.include(provider.getId(), true));
	}

	@Test
	void testRegister() {
		var provider1 = new DefaultProvider();
		var provider2 = new DefaultProvider();
		assertTrue(this.policy.register(provider1));
		assertTrue(this.policy.register(provider2));
		assertFalse(this.policy.register(provider1), "Cannot register the same provider twice!");
		
		assertTrue(this.policy.register(new DefaultProvider()));
		assertFalse(this.policy.register(new DefaultProvider())); // only 3 providers
	}

	@Test
	void testEnabledProviders() {
		assertEquals(0, this.policy.enabledProviders());
		var provider1 = new DefaultProvider();
		var provider2 = new DefaultProvider();
		assertTrue(this.policy.register(provider1));
		assertTrue(this.policy.register(provider2));
		assertEquals(0, this.policy.enabledProviders()); // the health check triggers enablement!
		
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		this.policy.setEnabledProviders(list);
		assertEquals(1, this.policy.enabledProviders());
	}

	@Test
	void testGetRegisteredProviders() {
		assertEquals(0, this.policy.getRegisteredProviders().size());
		
		var provider1 = new DefaultProvider();
		var provider2 = new DefaultProvider();
		assertTrue(this.policy.register(provider1));
		assertTrue(this.policy.register(provider2));
		var list = this.policy.getRegisteredProviders();
		assertTrue(list.contains(provider1));
		assertTrue(list.contains(provider2));
		assertEquals(2, list.size());
		
		this.policy.setEnabledProviders(new ArrayList<>());
		assertEquals(0, this.policy.enabledProviders());
		assertEquals(2, this.policy.registeredProviders());
	}

	@Test
	void testSetEnabledProviders() {
		assertEquals(0, this.policy.enabledProviders());
		assertEquals(0, this.policy.registeredProviders());
		
		Provider provider1 = new DefaultProvider();
		ArrayList<Provider> list = new ArrayList<>();
		list.add(provider1);
		this.policy.setEnabledProviders(list);
		assertEquals(1, this.policy.enabledProviders());
		assertEquals(0, this.policy.registeredProviders());
		
	}

	@Test
	void testRegisteredProviders() {
		var provider = new DefaultProvider();
		assertEquals(0, this.policy.registeredProviders());
		this.policy.register(provider);
		assertEquals(1, this.policy.registeredProviders());
		this.policy.register(provider); // cannot register twice the same provider
		assertEquals(1, this.policy.registeredProviders());
	}

	@Test
	void testStatistics() {
		assertNotNull(this.policy.statistics());
		
	}

}
