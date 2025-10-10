package org.eclipse.swt.widgets;

import io.github.humbleui.skija.Surface;

public interface ISkiaCanvas {

	Surface getSurface();

	SkiaResources getResources();

	Surface createSupportSurface(int width, int height);

}
