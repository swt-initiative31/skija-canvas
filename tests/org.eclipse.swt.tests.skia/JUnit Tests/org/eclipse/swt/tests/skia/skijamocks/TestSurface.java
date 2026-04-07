package org.eclipse.swt.tests.skia.skijamocks;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Surface;

public class TestSurface extends Surface {

	private Surface surface;
	private TestCanvas canvas;

	public TestSurface(Surface s) {
		super(s._ptr);
		this.surface = s;
		this.canvas = new TestCanvas(surface.getCanvas());

	}

	public TestCanvas getTestCanvas() {
		return canvas;
	}

	@Override
	public Canvas getCanvas() {
		return this.canvas;
	}

}
