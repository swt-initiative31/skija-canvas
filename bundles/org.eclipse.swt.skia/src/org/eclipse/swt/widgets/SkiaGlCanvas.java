package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;

public class SkiaGlCanvas extends Canvas {

	public SkiaGlCanvas(Composite parent, int style) {
		super (parent,  prepare(style));
		SkiaConfiguration.resetCanvasConfiguration();
	}

	private static int prepare(int style) {
		SkiaConfiguration.activateSkiaGl();
		return style | SWT.EDGE;
	}


}
