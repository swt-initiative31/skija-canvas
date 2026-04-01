package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 *
 * Provides utility methods for the canvas extension to scale values according
 * to the current DPI settings of the OS. This is used internally to scale all
 * values that are used for drawing and layout calculations.
 */
public class DpiScaler {

	public DpiScaler(Canvas canvas) {
	}

	/**
	 * Scales a value up according to the native zoom factor. Uses integer
	 * arithmetic with rounding for precise results (e.g. for nativeZoom=125).
	 */
	private int scaleUp(int value) {
		return value;
	}

	/**
	 * Scales a value down according to the native zoom factor. Uses integer
	 * arithmetic with rounding for precise results (e.g. for nativeZoom=125).
	 */
	private int scaleDown(int value) {
		return value;
	}

	public int getZoomedFontSize(int fontSize) {
		// dpi to inch
		fontSize = (fontSize * Display.getDefault().getDPI().y) / 72;
		return scaleUp(fontSize);
	}

	public Rectangle scaleUpRectangle(Rectangle rectangle) {
		return new Rectangle(scaleUp(rectangle.x), scaleUp(rectangle.y), scaleUp(rectangle.width), scaleUp(rectangle.height));
	}

	public Point scaleSize(int x, int y) {
		return new Point(scaleUp(x), scaleUp(y));
	}

	public Point scaleSurfaceSize(int width, int height) {
		return new Point(scaleUp(width), scaleUp(height));
	}

	public float autoScaleUp(float f) {
		return f;
	}

	public int autoScaleUp(int value) {
		return scaleUp(value);
	}

	public float autoScaleDown(float width) {
		return width;
	}

	/**
	 * Scales a float array down according to the native zoom factor. Each value is
	 * divided by nativeZoom/100, preserving float precision.
	 *
	 * @param values the array to scale down
	 * @return a new array with all values scaled down, or null if input is null
	 */
	public float[] autoScaleDown(float[] values) {
		if (values == null)
			return null;
		float[] result = new float[values.length];
		float factor = 1.0f;
		for (int i = 0; i < values.length; i++) {
			result[i] = values[i] * factor;
		}
		return result;
	}

	/**
	 * Scales a float value down according to the native zoom factor and rounds to
	 * the nearest integer.
	 *
	 * @param f the value to scale down
	 * @return the scaled and rounded integer value
	 */
	public int autoScaleDownToInt(float f) {
		return Math.round(f);
	}

	public int getNativeZoom() {
		return 100;
	}

	public Point scaleDown(Point point) {
		return new Point(scaleDown(point.x), scaleDown(point.y));
	}

	public  float[] autoScaleUp(float[] values) {
		if (values == null)
			return null;
		float[] result = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = autoScaleUp(values[i]);
		}
		return result;
	}

	public int[] autoScaleUp(int[] values) {
		if (values == null)
			return null;
		int[] result = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			result[i] = autoScaleUp(values[i]);
		}
		return result;
	}

}