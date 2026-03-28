/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     SAP SE and others - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.tests.skia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class Test_org_eclipse_swt_widgets_SkiaCanvas_Text {

	final static String TEXT = "T";

	final static int MAX_DIFF = 3;

	@BeforeEach
	public void setUp() {
	}

	@AfterEach
	public void tearDown() {
	}

	@Test
	@Disabled // this test is too big for automatic execution. Use Text_Simple instead
	public void test_org_eclipse_swt_skia_drawText() {

//		for (var zoom : DPIScaler.getSupportedZooms()) {
//

		// other zooms not yet supported...
		int zoom = 100;
//		System.out.println("Letter" + " " + "textHeight" + "  " + "zoom" + " BG-Position  " + "backgroundArea.width"
//				+ "  " + "backgroundArea.height" + " FG-Position " + "foregroundArea.width" + "  "
//				+ "foregroundArea.height");
		for (int size = 5; size <= 40; size += 10) {
			for (var letter : alphabetUpper()) {

//				letter = 'H';
//				size = 23;

				CanvasCompareTool t = new CanvasCompareTool();
				try {
					t.init(new FillLayout(SWT.HORIZONTAL));

					DpiScalerUtil.setNativeZoom(t.classicalCanvas, zoom);
					DpiScalerUtil.setNativeZoom(t.skiaCanvas, zoom);

					executeTextCompareTest(t, letter, zoom, size);
				} finally {
					t.dispose();
				}
			}
		}

//		for (var letter : alphabetLower()) {
//			for (int size = 5; size <= 40; size += 10) {
//				CanvasCompareTool t = new CanvasCompareTool();
//				t.init(new FillLayout(SWT.HORIZONTAL));
//
//				t.classicalCanvas.nativeZoom = zoom;
//				t.skiaCanvas.nativeZoom = zoom;
//
//				executeTextCompareTest(t, letter, zoom, size);
//				t.dispose();
//			}
//		}

	}

	public char[] alphabetLower() {
		char a = 'a';
		var length = 'z' - 'a' + 1;

		char[] alphabet = new char[length];

		for (int i = 0; i < length; i++) {
			alphabet[i] = (char) (a + i);
		}
		return alphabet;
	}

	public char[] alphabetUpper() {

		char a = 'A';
		var length = 'Z' - 'A' + 1;
		char[] alphabet = new char[length];
		for (int i = 0; i < length; i++) {
			alphabet[i] = (char) (a + i);
		}
		return alphabet;
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

//		printFontData(e.gc.getFont().getFontData());

		var fd = e.gc.getFont().getFontData()[0];

		fd.setHeight(textHeight);

		e.gc.setFont(new Font(Display.getDefault(), new FontData[] { fd }));

		e.gc.drawText(text, 10, 10);

	}

	@SuppressWarnings("unused")
	private static void printFontData(FontData[] fontData) {

		for (var f : fontData) {
			System.out.println("FontData----------------------------");
			System.out.println(f.height);
			System.out.println(f.getName());
			System.out.println("------------------------------------");
		}

	}

	private void assertTextsAreEquals(char letter, Image i1, Image i2, Color foregound, Color background, int zoom,
			int textHeight) {

		var data1 = i1.getImageData(100);
		var data2 = i2.getImageData(100);

		var ta1 = new TextAreaPosition(data1);
		var ta2 = new TextAreaPosition(data2);

//		printAreaPosition(ta1, zoom, textHeight, letter);
//		printAreaPosition(ta2, zoom, textHeight, letter);

		assertTextAreaEquals(letter, ta1, ta2, zoom, textHeight);

	}

	@SuppressWarnings("unused")
	private void printAreaPosition(TextAreaPosition ta, int zoom, int textHeight, char letter) {

		System.out.println(
				letter + " " + textHeight + "  " + zoom + "  " + ta.backgroundArea.x + "/" + ta.backgroundArea.y + "   "
						+ ta.backgroundArea.width + "  " + ta.backgroundArea.height + "   " + ta.foregroundArea.x + "/"
						+ ta.foregroundArea.y + "   " + ta.foregroundArea.width + "  " + ta.foregroundArea.height);

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
