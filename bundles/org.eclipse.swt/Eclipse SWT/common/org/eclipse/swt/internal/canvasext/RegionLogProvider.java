package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.graphics.*;

public class RegionLogProvider {

	private Region r;

	public RegionLogProvider(Region r) {
		this.r = r;
	}

	public RegionLog getLog() {
		return r.getLog();
	}

}
