package org.lb.provider;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default provider
 */
public class DefaultProvider implements Provider {

	private static Logger logger = LoggerFactory.getLogger(DefaultProvider.class);

	private static final int HEARTBEATS_OK = 2;

	private static AtomicInteger counter = new AtomicInteger();

	/** Provider id. */
	private final int id;

	private final String msg;

	/** True if this provider can serve traffic, false otherwise. */
	private AtomicBoolean enabled;

	/** Number of handled requests. */
	private AtomicLong requests;

	/**
	 * When the provider is enabled this field is set to 3. It only matters after
	 * the transition off -> on
	 */
	private int okChecksAfterFailure;

	/** true if this provider is included, false if it has been taken offline. */
	private volatile boolean included;

	public DefaultProvider() {
		this(counter.incrementAndGet());
	}

	public DefaultProvider(int id) {
		this.id = id;
		msg = "provider_" + this.id;
		enabled = new AtomicBoolean(true);
		requests = new AtomicLong(0L);
		okChecksAfterFailure = HEARTBEATS_OK + 1;
		included = true;
	}

	protected void preGet() {
		// override if needed
	}

	protected void postGet() {
		this.requests.incrementAndGet();
	}

	/**
	 * Get the provider name.
	 */
	public String get() {
		preGet();
		final String res = this.msg; // step 1, page 2
		postGet();
		return res;
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public boolean enable() {
		if (this.isEnabled()) {// if already enabled, do nothing
			return true;
		}

		// not enabled yet, check
		/*
		 * If a node has been previously excluded from the balancing it should be
		 * re-included if it has successfully been "heartbeat checked" for 2 consecutive
		 * times
		 */
		if (this.okChecksAfterFailure >= HEARTBEATS_OK - 1) {
			this.enabled.set(true);
			logger.info("Enabled provider " + getId());
			return true;
		} else {
			this.okChecksAfterFailure++;
			return false;
		}
	}

	@Override
	public boolean disable() {
		this.enabled.set(false);
		this.okChecksAfterFailure = 0;
		logger.warn("Disabled provider " + getId());
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled.get();
	}

	@Override
	public boolean check() {
		return true; // perform health check
	}

	@Override
	public boolean isIncluded() {
		return included;
	}

	/**
	 * Include/exclude this provider.
	 * 
	 * @param include true to include, false to exclude.
	 */
	public void include(boolean include) {
		this.included = include;
		logger.info("Provider " + this.id + " included: " + include);
	}

	@Override
	public long getRequests() {
		return this.requests.get();
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DefaultProvider)) {
			return false;
		}
		DefaultProvider other = (DefaultProvider) obj;
		return id == other.id;
	}

}
