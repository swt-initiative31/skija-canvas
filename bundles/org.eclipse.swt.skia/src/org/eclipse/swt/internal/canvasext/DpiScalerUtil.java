package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.internal.canvasext.DpiScaler;
import org.eclipse.swt.widgets.Canvas;
/**
 * Scaling utility for canvases.
 */
public class DpiScalerUtil {

	public static int[] getSupportedZooms() {
		return DpiScaler.getSupportedZooms();
	}

	public static void setNativeZoom(Canvas classicalCanvas, int zoom) {
		DpiScaler.setNativeZoom(classicalCanvas, zoom);
	}

}
