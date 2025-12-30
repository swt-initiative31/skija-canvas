package org.eclipse.swt.tests.skia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DPIScaler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class Test_org_eclipse_swt_widgets_SkiaCanvas_Text_Simple {

	int zoom = 100;
	int size = 10;
	char letter = 'T';

	final static int MAX_DIFF = 3;

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	public void test_org_eclipse_swt_skia_drawText() {

		if (SWT.getPlatform().startsWith("win32")) {
			// This test does not seem to work on windows.
			return;
		}


		CanvasCompareTool t = new CanvasCompareTool();
		t.init(new FillLayout(SWT.HORIZONTAL));

		DPIScaler.setNativeZoom(t.classicalCanvas, zoom);
		DPIScaler.setNativeZoom(t.skiaCanvas, zoom);

		executeTextCompareTest(t, letter, zoom, size);
		t.dispose();
	}

	private void executeTextCompareTest(CanvasCompareTool t, char letter, int zoom, int textHeight) {
		var d = Display.getDefault();
		Color col1 = d.getSystemColor(SWT.COLOR_RED);
		Color col2 = d.getSystemColor(SWT.COLOR_GREEN);

		t.addPaintListener(p -> drawText(letter + "", p, col1, col2, textHeight));
		t.waitForExecution();

		Image i1 = t.extractImageFromClassic();
		Image i2 = t.extractImageFromSkia();

		assertTextsAreEquals(letter, i1, i2, col1, col2, zoom, textHeight);

		i1.dispose();
		i2.dispose();
	}

	private static void drawText(String text, PaintEvent e, Color col1, Color col2, int textHeight) {

		e.gc.setBackground(col1);
		e.gc.setForeground(col2);

		var fd = new FontData("Arial", textHeight, SWT.NORMAL);
		Font f = new Font(Display.getDefault(), fd);
		e.gc.setFont(f);

		e.gc.drawText(text, 10, 10);

	}

	private void assertTextsAreEquals(char letter, Image i1, Image i2, Color foregound, Color background, int zoom,
			int textHeight) {

		var data1 = i1.getImageData(100);
		var data2 = i2.getImageData(100);

		var ta1 = new TextAreaPosition(data1);
		var ta2 = new TextAreaPosition(data2);

		assertTextAreaEquals(letter, ta1, ta2, zoom, textHeight);

	}


	private void assertTextAreaEquals(char letter, TextAreaPosition ta1, TextAreaPosition ta2, int zoom,
			int textHeight) {

		String msg = "zoom:  " + zoom + " for letter: " + letter + " with textheight: " + textHeight;

		assertEquals(ta1.canvasBackground, ta2.canvasBackground, "Compare CanvasColor failed, " + msg);
		assertEquals(ta1.bgColor, ta2.bgColor, "Compare background color failed, " + msg);
		approximatelyEquals(ta1.backgroundArea, ta2.backgroundArea, msg);
		approximatelyEquals(ta1.foregroundArea, ta2.foregroundArea, msg);

	}

	private void approximatelyEquals(Rectangle fa1, Rectangle fa2, String string) {

		int diffX = fa1.x - fa2.x;
		int diffY = fa1.y - fa2.y;

		int diffWidth = fa1.width - fa2.width;
		int diffHeight = fa1.height - fa2.height;

		if (Math.abs(diffWidth) > MAX_DIFF || Math.abs(diffHeight) > MAX_DIFF || Math.abs(diffX) > MAX_DIFF
				|| Math.abs(diffY) > MAX_DIFF) {
			assertEquals(fa1, fa2, "The difference is too high: " + string);
		}

	}

}
