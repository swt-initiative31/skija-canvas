package org.eclipse.swt.graphics;

/**
 * 
 * original calls come from DPIUtil
 * 
 */
public class DPIScaler {

	public static int autoScaleUp(int o) {
		return o;
	}

	public static int getDeviceZoom() {
		return 100;
	}

	public static float autoScaleDown(float o) {
		return o;
	}
	
	public static int autoScaleDown(int o) {
		return o;
	}

	public static int getNativeDeviceZoom() {
		return 100;
	}

	public static int scaleUp(int o, int zoom ) {
		return o;
	}

	public static float autoScaleUp(float o) {
		return o;
	}

	public static float[] autoScaleDown(float[] o ) {
		return o;
	}

	public static Point autoScaleUp(Point p) {
		return p;
	}

}
