package org.eclipse.swt.tests.skia;

import java.lang.reflect.Field;

import org.eclipse.swt.widgets.Canvas;

public class DpiScalerUtil {

	public static int[] getSupportedZooms() {

		if (isWindows()) {
			return new int[] { 100, 150, 200, 250, 300 };
		}

		return new int[] { 100 };
	}

	public static void setNativeZoom(Canvas classicalCanvas, int zoom) {

		if (!isWindows()) {
			return; // nativeZoom is only relevant on Windows, so we can skip it on other platforms
		}

		try {
			// Traverse the class hierarchy to find the 'nativeZoom' field
			Class<?> clazz = classicalCanvas.getClass();
			Field nativeZoomField = null;
			while (clazz != null) {
				try {
					nativeZoomField = clazz.getDeclaredField("nativeZoom");
					break;
				} catch (NoSuchFieldException e) {
					clazz = clazz.getSuperclass();
				}
			}
			if (nativeZoomField == null) {
				throw new NoSuchFieldException("Field 'nativeZoom' not found in class hierarchy.");
			}
			nativeZoomField.setAccessible(true);
			nativeZoomField.setInt(classicalCanvas, zoom);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("win");
	}

}