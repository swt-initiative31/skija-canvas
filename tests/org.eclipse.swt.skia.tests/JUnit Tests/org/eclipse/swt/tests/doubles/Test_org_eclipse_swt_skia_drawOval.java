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

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.types.Rect;

public class Test_org_eclipse_swt_skia_drawOval {

	@BeforeAll
	static void checkPlatform() {
		assumeTrue(SupportedTestPlatforms.isSupported(), "Test skipped: Platform not supported");
	}

	@Test
	public void testDrawOval() {
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

		gc.drawOval(10, 20, 30, 40);

		PaintData epd = new PaintData();
		epd.color = -1;
		epd.strokeWidth = 1.0f;
		epd.strokeMiter = 4.0f;
		epd.strokeCap = 0;
		epd.strokeJoin = 0;
		epd.style = 1;
		epd.alpha = 255;
		epd.antiAlias = false;
		epd.dither = false;
		epd.shader = null;
		epd.blendMode = BlendMode.SRC_OVER;
		epd.pathEffect = null;
		epd.imageFilter = null;
		epd.colorFilter = null;
		epd.maskFilter = null;

		// drawOval calls drawOval(Rect rect, Paint paint), coordinates are scaled and possibly offset
		assertEquals(MethodCall.get("SkCanvasDouble", "drawOval", new Rect(10.5f, 20.5f, 40.5f, 60.5f), epd),
				canvas.calls.get(0));

		surface.close();
	}
}