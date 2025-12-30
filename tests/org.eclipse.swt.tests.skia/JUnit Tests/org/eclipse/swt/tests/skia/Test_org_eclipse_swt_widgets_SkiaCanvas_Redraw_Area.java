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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DPIScaler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class Test_org_eclipse_swt_widgets_SkiaCanvas_Redraw_Area {

	Rectangle r = null;
	private int zoom;

	static void fillRectangles(PaintEvent e, Color col) {

//		Rectangle eventRect = new Rectangle(e.x, e.y, e.width, e.height);
//		System.out.println("fillRectangles");
//		System.out.println("GC: " + e.gc);
//		System.out.println("Area: " + eventRect);

		Canvas c = (Canvas) e.widget;
		var s = c.getSize();

		e.gc.setBackground(col);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x, s.y));

	}

	private void fillRectangles2(PaintEvent e, Color col1, Color col2) {

		Rectangle eventRect = new Rectangle(e.x, e.y, e.width, e.height);

//		System.out.println("fillRectangles2");
//		System.out.println("GC: " + e.gc);
//		System.out.println("Area: " + eventRect);

		Canvas c = (Canvas) e.widget;
		var s = c.getSize();

		if (this.r != null) {

			var ca = c.getClientArea();

			ca.intersect(this.r);

			assertEquals(ca, eventRect, "CurrentZoom: " + this.zoom);
			assertNotEquals(0, e.width, "CurrentZoom: " + this.zoom);
			assertNotEquals(0, e.height, "CurrentZoom: " + this.zoom);

		}

		e.gc.setBackground(col1);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x, s.y));

		e.gc.setClipping(new Rectangle(50, 50, 100, 100));

		e.gc.setBackground(col2);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x, s.y));

	}

	@Test
	public void test_org_eclipse_swt_skia_redraw_rectangle() {

		for (var zoom : DPIScaler.getSupportedZooms()) {

			this.r = null;

			CanvasCompareTool t = new CanvasCompareTool();
			try {
				t.init(null);

				DPIScaler.setNativeZoom(t.classicalCanvas, zoom);
				DPIScaler.setNativeZoom(t.skiaCanvas, zoom);

				Display d = t.display;

				Color col1 = d.getSystemColor(SWT.COLOR_RED);
				Color col2 = d.getSystemColor(SWT.COLOR_GREEN);

				t.addPaintListener(p -> fillRectangles(p, col1));
				t.waitForExecution();

				t.addPaintListener(p -> fillRectangles(p, col2));
				t.waitForExecution(new Rectangle(30, 30, 50, 50), true);

				Image i1 = t.extractImageFromClassic(); // extractImage(classicalCanvas);
				Image i2 = t.extractImageFromSkia(); // extractImage(skiaCanvas);

				t.assertImagesEqual(zoom, i1, i2);

				i1.dispose();
				i2.dispose();
			} finally {
				t.dispose();
			}
		}

	}

	@Test
	public void test_org_eclipse_swt_skia_redraw_rectangle2() {

		for (var zoom : DPIScaler.getSupportedZooms()) {

			this.r = null;

			this.zoom = zoom;
			CanvasCompareTool t = new CanvasCompareTool();
			try {
				t.init(null);

				DPIScaler.setNativeZoom(t.classicalCanvas, zoom);
				DPIScaler.setNativeZoom(t.skiaCanvas, zoom);

				Display d = t.display;

				Color col0 = d.getSystemColor(SWT.COLOR_WHITE);
				Color col1 = d.getSystemColor(SWT.COLOR_RED);
				Color col2 = d.getSystemColor(SWT.COLOR_GREEN);

				t.addPaintListener(p -> fillRectangles(p, col0));
				t.waitForExecution();

				t.removePaintListeners();

				t.addPaintListener(p -> fillRectangles2(p, col1, col2));
				this.r = new Rectangle(30, 30, 50, 50);
				t.waitForExecution(r, true);

				// redraw area outside of visible area, no redraw expected.
				this.r = new Rectangle(2000, 2000, 1, 1);
				t.waitForExecution(r, false);

				Image i1 = t.extractImageFromClassic(); // extractImage(classicalCanvas);
				Image i2 = t.extractImageFromSkia(); // extractImage(skiaCanvas);

				t.assertImagesEqual(zoom, i1, i2);

				i1.dispose();
				i2.dispose();
			} finally {
				t.dispose();
			}

		}

	}

}
