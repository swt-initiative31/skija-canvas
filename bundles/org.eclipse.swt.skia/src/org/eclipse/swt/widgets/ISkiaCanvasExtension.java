package org.eclipse.swt.widgets;

import org.eclipse.swt.graphics.DPIScaler;

import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Surface;

public interface ISkiaCanvasExtension {

	Surface getSurface();

	SkiaResources getResources();

	Surface createSupportSurface(int width, int height);

	default Matrix33 getTransformation() {

		return null;

	}

	DPIScaler getScaler();

}
