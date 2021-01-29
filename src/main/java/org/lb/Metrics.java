package org.lb;

import java.util.concurrent.atomic.AtomicLong;

class Metrics {

	/** Number of jobs in the system. */
	private AtomicLong pending;

	/** Number of received requests. */
	private AtomicLong arrivals;

	/** Number of served requests. */
	private AtomicLong success;

	/** Number of rejected requests. */
	private AtomicLong rejected;

	public Metrics() {
		pending = new AtomicLong();
		arrivals = new AtomicLong();
		success = new AtomicLong();
		rejected = new AtomicLong();
	}

	void reject() {
		this.arrivals.incrementAndGet();
		this.rejected.incrementAndGet();
	}

	void success() {
		this.arrivals.incrementAndGet();
		this.success.incrementAndGet();
	}

	void increasePending() {
		this.pending.incrementAndGet();
	}

	void decreasePending() {
		if (this.pending.decrementAndGet() < 0L) {
			throw new IllegalStateException("Pending cannot be negative.");
		}
	}

	public long getPending() {
		return pending.get();
	}

	public long getArrivals() {
		return arrivals.get();
	}

	public long getSuccess() {
		return success.get();
	}

	public long getRejected() {
		return rejected.get();
	}

}
