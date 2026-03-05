package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;

public class SkiaGlCanvas extends Canvas {

	public SkiaGlCanvas(Composite parent, int style) {
		super (parent,  prepare(style));
	}

	private static int prepare(int style) {
		return style | SWT.SKIA;
	}


}
