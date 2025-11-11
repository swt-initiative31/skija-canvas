package org.eclipse.swt.graphics;

public class RegionLogProvider {

    Region r ;

    public RegionLogProvider(Region r) {
	this.r = r;
    }

    public RegionLog getLog() {
	return r.getLog();
    }

}
