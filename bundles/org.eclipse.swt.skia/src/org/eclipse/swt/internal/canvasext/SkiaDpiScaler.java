package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.widgets.Canvas;

public class SkiaDpiScaler {

	public static int[] getSupportedZooms() {
		return DPIScaler.getSupportedZooms();
	}

	public static void setNativeZoom(Canvas classicalCanvas, int zoom) {
		DPIScaler.setNativeZoom(classicalCanvas, zoom);
	}

}
