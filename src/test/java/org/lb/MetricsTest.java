package org.lb;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsTest {

	private Metrics metrics;

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
		this.metrics = new Metrics();
	}

	@AfterEach
	void tearDown() throws Exception {
		this.metrics = null;
	}

//	@Test
//	void testArrival() {
//		assertEquals(0L, this.metrics.getArrivals());
//
//		int max = 100_000;
//		for (int i = 0; i < max; i++) {
//			this.metrics.arrival();
//		}
//		assertEquals(max, this.metrics.getArrivals());
//	}

	@Test
	void testReject() {
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());
		this.metrics.reject();

		// all other metrics are still at 0
		assertEquals(1L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(1L, this.metrics.getRejected());
	}

	@Test
	void testSuccess() {
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());

		this.metrics.success();
		// all other metrics are still at 0
		assertEquals(1L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(1L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());
	}

	@Test
	void testIncreasePending() {
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());

		this.metrics.increasePending();
		// all other metrics are still at 0
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(1L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());
	}

	@Test
	void testDecreasePending1() {
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());

		this.metrics.increasePending();
		// all other metrics are still at 0
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(1L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());

		this.metrics.decreasePending();
		assertEquals(0L, this.metrics.getArrivals());
		assertEquals(0L, this.metrics.getPending());
		assertEquals(0L, this.metrics.getSuccess());
		assertEquals(0L, this.metrics.getRejected());
	}

	@Test
	void testDecreasePending2() {
		assertThrows(IllegalStateException.class, () -> {
			this.metrics.decreasePending();
		});	
	}
	
	@Test
	void testDecreasePending3() {		
		this.metrics.increasePending();
		this.metrics.decreasePending();
		assertThrows(IllegalStateException.class, () -> {
			this.metrics.decreasePending();
		});
	}

	@Test
	void testGetPending() {
		assertEquals(0, this.metrics.getPending());
		
		for (int i = 1; i <= 100_000; i++) {
			this.metrics.increasePending();
			this.metrics.success();
			this.metrics.decreasePending();
			assertEquals(i, this.metrics.getArrivals());
			assertEquals(i, this.metrics.getSuccess());
			assertEquals(0, this.metrics.getPending());
			assertEquals(0, this.metrics.getRejected());
		}
	}

	@Test
	void testGetSuccess() {
		assertEquals(0, this.metrics.getSuccess());
	}

	@Test
	void testGetRejected() {
		assertEquals(0, this.metrics.getRejected());
		
		for (int i = 1; i <= 100_000; i++) {
			this.metrics.increasePending();
			this.metrics.reject();
			this.metrics.decreasePending();
			assertEquals(i, this.metrics.getArrivals());
			assertEquals(0, this.metrics.getSuccess());
			assertEquals(0, this.metrics.getPending());
			assertEquals(i, this.metrics.getRejected());
		}
	}

}
