package org.eclipse.swt.internal.skia;

import org.eclipse.swt.internal.canvasext.DpiScaler;

import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Surface;

public interface ISkiaCanvasExtension {

	Surface getSurface();

	SkiaResources getResources();

	Surface createSupportSurface(int width, int height);

	default Matrix33 getTransformation() {
		return null;
	}

	DpiScaler getScaler();

}
