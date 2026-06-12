package org.eclipse.swt.tests.doubles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.internal.graphics.SkiaGC;
import org.eclipse.swt.tests.skia.SupportedTestPlatforms;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.types.Rect;

public class Test_org_eclipse_swt_skia_drawText {
	@BeforeAll
	static void checkPlatform() {
		assumeTrue(SupportedTestPlatforms.isSupported(), "Test skipped: Platform not supported");
	}

	@Test
	public void testDrawText() {
		SkiaResourcesDouble resources = new SkiaResourcesDouble();
		Color foregroundColor = new Color(null, 255, 255, 255);
		resources.setForeground(foregroundColor);
		Color backgroundColor = new Color(null, 0, 0, 0);
		resources.setBackground(backgroundColor);

		// Use a default Skia font (should be available everywhere)
		final FontMgr fm = FontMgr.getDefault();
		var fontStyle = fm.matchFamilyStyle(fm.getFamilyName(0), FontStyle.NORMAL);
		resources.skiaFont = new Font(fontStyle);

		FontData data = new FontData();
		data.setName("Arial");
		data.setHeight(12);
		data.setStyle(SWT.NORMAL);
		resources.fontData = data;

		final SkSurfaceDouble surface = new SkSurfaceDouble(100, 100, "SurfaceDouble", null);
		final CanvasExtensionDouble ext = new CanvasExtensionDouble();
		var canvas = surface.canvas;
		ext.surface = surface;
		ext.resources = resources;
		final SkiaGC gc = new SkiaGC(ext, SWT.NONE);

		gc.drawText("World", 10, 20);

		var calls = surface.calls;

		System.out.println(calls);

		// Assert that exactly 2 method calls were made
		assertEquals(2, calls.size(), "Expected 2 method calls");

		// Call 0: drawRect() - draw the background rectangle
		MethodCall call0 = calls.get(0);
		assertEquals("SurfaceDouble-canvas", call0.className);
		assertEquals("drawRect", call0.methodName);
		assertEquals(2, call0.params.length);
		// First param: Rect with bounds [10, 20, 44, 35]
		Rect rect = (Rect) call0.params[0];
		assertEquals(10.0f, rect.getLeft(), "drawRect() left should be 10");
		assertEquals(20.0f, rect.getTop(), "drawRect() top should be 20");
		assertEquals(44.0f, rect.getRight(), "drawRect() right should be 44");
		assertEquals(35.0f, rect.getBottom(), "drawRect() bottom should be 35");
		// Second param: PaintData for the background rectangle
		PaintData paintCall0 = (PaintData) call0.params[1];
		assertEquals(-16777216, paintCall0.color, "drawRect() background color should be black (-16777216)");
		assertEquals(0.0f, paintCall0.strokeWidth, "drawRect() stroke width should be 0");

		// Call 1: drawString() - draw the string "World"
		MethodCall call1 = calls.get(1);
		assertEquals("SurfaceDouble-canvas", call1.className);
		assertEquals("drawString", call1.methodName);
		assertEquals(5, call1.params.length);
		// Params: String, x, y, FontData, PaintData
		assertEquals("World", call1.params[0], "drawString() should draw 'World'");
		assertEquals(10, call1.params[1], "drawString() x position should be 10");
		assertEquals(31, call1.params[2], "drawString() y position should be 31");
		// FontData validation
		org.eclipse.swt.tests.doubles.FontData fontData = (org.eclipse.swt.tests.doubles.FontData) call1.params[3];
		assertEquals("Arial", fontData.name, "Font name should be Arial");
		assertEquals(12.0f, fontData.size, "Font size should be 12");
		assertEquals(400, fontData.weight, "Font weight should be 400");
		assertEquals(5, fontData.width, "Font width should be 5");
		assertEquals(0, fontData.slant, "Font slant should be 0");
		assertEquals(2, fontData.ediging, "Font ediging should be 2");
		assertEquals(1.0f, fontData.scaleX, "Font scaleX should be 1.0");
		assertEquals(0.0f, fontData.skewX, "Font skewX should be 0.0");
		// PaintData for text
		PaintData paintCall1 = (PaintData) call1.params[4];
		assertEquals(-1, paintCall1.color, "drawString() text color should be white (-1)");
		assertEquals(1.0f, paintCall1.strokeWidth, "drawString() stroke width should be 1.0");
		assertEquals(4.0f, paintCall1.strokeMiter, "drawString() stroke miter should be 4.0");
		assertEquals(0, paintCall1.strokeCap, "drawString() stroke cap should be 0");
		assertEquals(0, paintCall1.strokeJoin, "drawString() stroke join should be 0");
		assertEquals(0, paintCall1.style, "drawString() style should be 0");
		assertEquals(255, paintCall1.alpha, "drawString() alpha should be 255");
		assertEquals(true, paintCall1.antiAlias, "drawString() antiAlias should be true");
		assertEquals(false, paintCall1.dither, "drawString() dither should be false");
		assertEquals(BlendMode.SRC_OVER, paintCall1.blendMode, "drawString() blendMode should be SRC_OVER");

		surface.close();
	}

	@Test
	public void testDrawText_Zoom150() {
		SkiaResourcesDouble resources = new SkiaResourcesDouble(150);
		Color foregroundColor = new Color(null, 255, 255, 255);
		resources.setForeground(foregroundColor);
		Color backgroundColor = new Color(null, 0, 0, 0);
		resources.setBackground(backgroundColor);

		// Use a default Skia font (should be available everywhere)
		final FontMgr fm = FontMgr.getDefault();
		var fontStyle = fm.matchFamilyStyle(fm.getFamilyName(0), FontStyle.NORMAL);
		resources.skiaFont = new Font(fontStyle);

		FontData data = new FontData();
		data.setName("Arial");
		data.setHeight(18); // 12 * 1.5 = 18
		data.setStyle(SWT.NORMAL);
		resources.fontData = data;

		final SkSurfaceDouble surface = new SkSurfaceDouble(150, 150, "SurfaceDouble", null);
		final CanvasExtensionDouble ext = new CanvasExtensionDouble();
		var canvas = surface.canvas;
		ext.surface = surface;
		ext.resources = resources;
		final SkiaGC gc = new SkiaGC(ext, SWT.NONE);

		gc.drawText("World", 10, 20);

		var calls = surface.calls;

		System.out.println(calls);

		// Assert that exactly 2 method calls were made
		assertEquals(2, calls.size(), "Expected 2 method calls");

		// Call 0: drawRect() - draw the background rectangle at 150% zoom
		MethodCall call0 = calls.get(0);
		assertEquals("SurfaceDouble-canvas", call0.className);
		assertEquals("drawRect", call0.methodName);
		assertEquals(2, call0.params.length);
		// At 150% zoom: 10*1.5=15, 20*1.5=30, 44*1.5=66, 35*1.5=52.5
		Rect rect = (Rect) call0.params[0];
		assertEquals(15.0f, rect.getLeft(), "drawRect() left should be 15");
		assertEquals(30.0f, rect.getTop(), "drawRect() top should be 30");
		assertEquals(66.0f, rect.getRight(), "drawRect() right should be 66");
		assertEquals(52.5f, rect.getBottom(), "drawRect() bottom should be 52.5");
		// Second param: PaintData for the background rectangle
		PaintData paintCall0 = (PaintData) call0.params[1];
		assertEquals(-16777216, paintCall0.color, "drawRect() background color should be black (-16777216)");

		// Call 1: drawString() - draw the string "World"
		MethodCall call1 = calls.get(1);
		assertEquals("SurfaceDouble-canvas", call1.className);
		assertEquals("drawString", call1.methodName);
		assertEquals(5, call1.params.length);
		// Params: String, x, y, FontData, PaintData
		assertEquals("World", call1.params[0], "drawString() should draw 'World'");
		// At 150% zoom: 10*1.5=15, y position will be adjusted accordingly
		assertEquals(15, call1.params[1], "drawString() x position should be 15 (10*1.5)");
		// FontData validation at 150% zoom
		org.eclipse.swt.tests.doubles.FontData fontData = (org.eclipse.swt.tests.doubles.FontData) call1.params[3];
		assertEquals("Arial", fontData.name, "Font name should be Arial");
		assertEquals(18.0f, fontData.size, "Font size should be 18 at 150% zoom");
		// PaintData for text
		PaintData paintCall1 = (PaintData) call1.params[4];
		assertEquals(-1, paintCall1.color, "drawString() text color should be white (-1)");
		assertEquals(1.5f, paintCall1.strokeWidth, "drawString() stroke width should be 1.5 (1.0*1.5)");
		assertEquals(BlendMode.SRC_OVER, paintCall1.blendMode, "drawString() blendMode should be SRC_OVER");

		surface.close();
	}
}
