package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.widgets.*;

/**
 *
 *	Provides utility methods for the canvas extension to scale values according to the current DPI settings of the OS.
 *	This is used internally to scale all values that are used for drawing and layout calculations.
 */
public class DpiScaler {

	private Canvas canvas;

	/**
	 * INTERNAL ONLY for tests only, do not use this for your applications
	 *
	 * @return possible zooms of the os.
	 */
	public static int[] getSupportedZooms() {
		return new int[] { 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600 };
	}

	public DpiScaler(Canvas canvas) {
		this.canvas = canvas;
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

	public static int scaleUp(int o, int zoom) {
		return Math.round(o * ((float) zoom) / (getDeviceZoom() * 1f));
	}

	public static float autoScaleUp(float o) {
		return o * getFactor();
	}

	public static float[] autoScaleDown(float[] o) {
		if (o == null) {
			return null;
		}
		final float[] res = new float[o.length];

		for (int i = 0; i < o.length; i++) {
			res[i] = o[i] * getFactor();
		}

		return res;
	}

	private static float getFactor() {
		return (float) (getDeviceZoom() / 100.0);
	}

	public static Point autoScaleUp(Point p) {
		return new Point(autoScaleUp(p.x), autoScaleUp(p.y));
	}

	public static int autoScaleDownToInt(float value) {
		return Math.round(autoScaleDown(value));
	}

	public int getZoomedFontSize(int fontSize) {
		return (fontSize * canvas.nativeZoom) / 100;
	}

	public int scaleTextMargin(int margin) {

		return (margin * canvas.nativeZoom) / 100;
	}

	public int scaleAscent(int asc) {
		return (asc * canvas.nativeZoom) / 100;
	}

	public Rectangle scaleBounds(Rectangle rectangle, int deviceZoom) {

		int zoomFactor = (canvas.nativeZoom / 100); // zoom on windows works in 100 % steps for rectangles
		return Win32DPIUtils.scaleBounds(rectangle, zoomFactor * 100,deviceZoom);
	}

	public Point scaleSize(int x, int y) {
		return new Point(x * canvas.nativeZoom / 100, y * canvas.nativeZoom / 100);
	}

	public static void setNativeZoom(Canvas canvas, int zoom) {
		canvas.nativeZoom = zoom;
	}

}
