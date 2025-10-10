package org.eclipse.swt.graphics;

import org.eclipse.swt.internal.DPIUtil;

/**
 *
 * original calls come from DPIUtil
 *
 */
public class DPIScaler {


	public DPIScaler() {
	}

	public static int autoScaleUp(int o) {
		return Math.round(o * getFactor());
	}

	public static int getDeviceZoom() {
		return DPIUtil.getDeviceZoom();
	}

	public static float autoScaleDown(float o) {
		return o / getFactor();
	}

	public static int autoScaleDown(int o) {
		return Math.round(o / getFactor());
	}

	public static int getNativeDeviceZoom() {
		return DPIUtil.getNativeDeviceZoom();
	}

	public static int scaleUp(int o, int zoom ) {
		return Math.round(o   *   ((float)zoom) / (getDeviceZoom() * 1f)) ;
	}

	public static float autoScaleUp(float o) {
		return o * getFactor();
	}

	public static float[] autoScaleDown(float[] o ) {
		if(o == null) {
			return null;
		}
		final float[] res = new float[o.length];

		for(int i = 0 ; i < o.length ; i++) {
			res[i] = o[i] * getFactor();
		}

		return res;
	}

	private static float getFactor() {
		return (float)(getDeviceZoom() / 100.0) ;
	}

	public static Point autoScaleUp(Point p) {
		return new Point(autoScaleUp(p.x) , autoScaleUp(p.y));
	}

	public static int autoScaleDownToInt(float value) {
		return Math.round(autoScaleDown(value));
	}

}
