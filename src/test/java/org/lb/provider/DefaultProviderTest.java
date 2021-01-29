package org.lb.provider;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultProviderTest {
	
	private DefaultProvider provider;

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
		provider = new DefaultProvider();
	}

	@AfterEach
	void tearDown() throws Exception {
		provider = null;
	}

	@Test
	void testHashCode() {
		var provider1 = new DefaultProvider();
		assertNotEquals(provider.hashCode(), provider1.hashCode());
	}

	@Test
	void testGet() {
		String msg = "provider_" + this.provider.getId();
		assertEquals(msg, this.provider.get());
	}

	@Test
	void testGetId() {
		assertTrue(this.provider.getId() > 0);
	}

	@Test
	void testEnable() {
		assertTrue(this.provider.enable());
		assertTrue(this.provider.disable());
		assertFalse(this.provider.enable());
		assertTrue(this.provider.enable());
		assertTrue(this.provider.enable());
		for (int i = 0; i < Integer.MAX_VALUE; i++) { // no overflow
			assertTrue(this.provider.enable());	
		}
		this.provider.enable();
		assertTrue(this.provider.isEnabled());
	}

	@Test
	void testDisable() {
		assertTrue(this.provider.disable());
		assertTrue(this.provider.disable());
	}

	@Test
	void testIsEnabled() {
		assertTrue(this.provider.isEnabled());
	}

	@Test
	void testCheck() {
		assertTrue(this.provider.check());
	}

	@Test
	void testIsIncluded() {
		assertTrue(this.provider.isIncluded());
		this.provider.include(false);
		assertFalse(this.provider.isIncluded());
		this.provider.include(true);
		assertTrue(this.provider.isIncluded());
	}

	@Test
	void testInclude() {
		assertTrue(this.provider.isIncluded());
		this.provider.include(false);
		assertFalse(this.provider.isIncluded());
	}

	@Test
	void testGetRequests() {
		assertEquals(0, this.provider.getRequests());
		for (int i = 1; i <= 10; i++) {
			this.provider.get();
			assertEquals(i, this.provider.getRequests());
		}
	}

	@Test
	void testEqualsObject() {
		var provider1 = new DefaultProvider();
		assertNotEquals(provider, provider1);
	}

}
