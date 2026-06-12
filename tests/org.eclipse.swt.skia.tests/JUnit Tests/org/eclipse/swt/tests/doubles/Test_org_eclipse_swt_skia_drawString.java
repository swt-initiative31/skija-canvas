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

public class Test_org_eclipse_swt_skia_drawString {
	@BeforeAll
	static void checkPlatform() {
		assumeTrue(SupportedTestPlatforms.isSupported(), "Test skipped: Platform not supported");
	}

	@Test
	public void testDrawString() {
		SkiaResourcesDouble resources = new SkiaResourcesDouble();
		Color foregroundColor = new Color(null, 255, 255, 255);
		resources.setForeground(foregroundColor);
		Color backgroundColor = new Color(null, 0, 0, 0);
		resources.setBackground(backgroundColor);
		
		// Use a default Skia font (should be available everywhere)
		final FontMgr fm = FontMgr.getDefault();
		var fontStyle = fm.matchFamilyStyle(fm.getFamilyName(0),FontStyle.NORMAL);
		resources.skiaFont = new Font(fontStyle); // Default constructor uses Skia's default typeface
		
		
		FontData data = new FontData();
		data.setName("Arial");
		data.setHeight(12);
		data.setStyle(SWT.NORMAL);
		resources.fontData = data;
		
		System.out.println("Testing drawString with text: 'World' at (15, 25)" + " using font: " + resources.skiaFont.getTypeface().getFamilyName());

		final SkCanvasDouble canvas = new SkCanvasDouble();
		final SkSurfaceDouble surface = new SkSurfaceDouble();
		surface.width = 100;
		surface.height = 100;
		surface.canvas = canvas;
		final CanvasExtensionDouble ext = new CanvasExtensionDouble();
		ext.surface = surface;
		ext.resources = resources;
		final SkiaGC gc = new SkiaGC(ext, SWT.NONE);

		gc.drawString("World", 15, 25);

		// Assumption: drawString creates a drawString call with the given parameters
		assertEquals("drawString", canvas.calls.get(0).methodName);
		assertEquals("World", canvas.calls.get(0).params[0]);
		assertEquals(15, canvas.calls.get(0).params[1]);
		assertEquals(25, canvas.calls.get(0).params[2]);

		surface.close();
	}
}