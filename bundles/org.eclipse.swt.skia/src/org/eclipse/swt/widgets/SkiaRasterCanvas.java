package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;

public class SkiaRasterCanvas extends Canvas{

	public SkiaRasterCanvas(Composite parent, int style) {
		super (parent,  prepare(style));
		SkiaConfiguration.resetCanvasConfiguration();
	}

	private static int prepare(int style) {
		SkiaConfiguration.activateSkiaRaster();
		return style | SWT.EDGE;
	}

}
