package org.lb;

import org.lb.provider.DefaultProvider;

public class SlowProvider extends DefaultProvider {

	private final long timeoutMs;

	public SlowProvider(long timeoutMs) {
		super();
		this.timeoutMs = timeoutMs;
	}

	@Override
	public String get() {
		if (this.timeoutMs > 0L) {
			try {
				Thread.sleep(timeoutMs);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
		return super.get();
	}

}
