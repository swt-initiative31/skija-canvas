package org.eclipse.swt.widgets;

public class SkiaRasterCanvas extends Canvas{

    public SkiaRasterCanvas(Composite parent, int style) {
	super (parent,  prepare(style));
	SkiaConfiguration.resetCanvasConfiguration();
    }

    private static int prepare(int style) {
	SkiaConfiguration.activateSkiaRaster();
	return style | SkiaConfiguration.SKIA;
    }

}
