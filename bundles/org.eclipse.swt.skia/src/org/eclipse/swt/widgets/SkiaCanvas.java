package org.eclipse.swt.widgets;

public class SkiaCanvas extends Canvas {

	public static final int SKIA = 1 << 23;

	public SkiaCanvas(Composite parent, int style) {
		super (parent,  prepare(style));
	}

	private static int prepare(int style) {
		return style | SKIA;
	}


}
