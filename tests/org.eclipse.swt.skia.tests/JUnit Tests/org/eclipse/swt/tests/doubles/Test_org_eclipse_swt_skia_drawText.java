package org.eclipse.swt.tests.doubles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.internal.graphics.SkiaGC;
import org.eclipse.swt.internal.skia.ISkiaResources;
import org.eclipse.swt.tests.skia.SupportedTestPlatforms;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Test_org_eclipse_swt_skia_drawText {
	@BeforeAll
	static void checkPlatform() {
		assumeTrue(SupportedTestPlatforms.isSupported(), "Test skipped: Platform not supported");
	}

	@Test
	public void testDrawText() {
		ISkiaResources resources = new SkiaResourcesDouble();
		Color foregroundColor = new Color(null, 255, 255, 255);
		resources.setForeground(foregroundColor);
		Color backgroundColor = new Color(null, 0, 0, 0);
		resources.setBackground(backgroundColor);

		final SkCanvasDouble canvas = new SkCanvasDouble();
		final SkSurfaceDouble surface = new SkSurfaceDouble();
		surface.width = 100;
		surface.height = 100;
		surface.canvas = canvas;
		final CanvasExtensionDouble ext = new CanvasExtensionDouble();
		ext.surface = surface;
		ext.resources = resources;
		final SkiaGC gc = new SkiaGC(ext, SWT.NONE);

		gc.drawText("Hello", 10, 20);

		// Assumption: drawText creates a drawText call with the given parameters
		assertEquals("drawText", canvas.calls.get(0).methodName);
		assertEquals("Hello", canvas.calls.get(0).params[0]);
		assertEquals(10, canvas.calls.get(0).params[1]);
		assertEquals(20, canvas.calls.get(0).params[2]);

		surface.close();
	}
}