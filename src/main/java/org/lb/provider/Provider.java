package org.lb.provider;

public interface Provider {

	/** Get the id of this provider, as string. */
	String get();

	/** Get the id of this provider. */
	int getId();

	/**
	 * Try to enable this provider.
	 * 
	 * @return true if enabled, false otherwise.
	 */
	boolean enable();

	/**
	 * Try to disable this provider.
	 * 
	 * @return true if disabled, false otherwise.
	 */
	boolean disable();

	/**
	 * Get the enabled status.
	 * 
	 * @return true if enabled, false otherwise.
	 */
	boolean isEnabled();

	/** Heartbeat. */
	boolean check();

	/**
	 * Include/exclude this provider.
	 * 
	 * @param include true to include, false to exclude.
	 */
	void include(boolean include);

	/** Check whether this provider is included. */
	boolean isIncluded();

	/** Get the number of handled requests. */
	long getRequests();
}
