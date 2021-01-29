package org.lb;

import org.lb.provider.DefaultProvider;

public class FaultyProvider extends DefaultProvider {
	
	boolean checkValue = false;

	public FaultyProvider() {
		super();
	}

	public FaultyProvider(int id) {
		super(id);
	}

		
	public boolean check() {
		return this.checkValue;
	}
		
}
