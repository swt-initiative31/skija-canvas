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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DPIScaler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

public class Test_org_eclipse_swt_widgets_SkiaCanvas_Two {

	static void fillRectangles(PaintEvent e, Color col1, Color col2) {

		Canvas c = (Canvas) e.widget;
		boolean classic = true;
		if (c.externalCanvasHandler != null) {
			classic = false;
		}

		var s = c.getSize();
		e.gc.setBackground(col1);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x, s.y));

		e.gc.setBackground(col2);
		e.gc.fillRectangle(new Rectangle(5, 5, 10, 10));

	}

	@Test
	public void test_org_eclipse_swt_skia_fillRectangle() {

		int zoom = 100;
		CanvasCompareTool t = new CanvasCompareTool();
		t.twoSkiaCanvas = true;
		t.init(null);

		DPIScaler.setNativeZoom(t.classicalCanvas, zoom);
		DPIScaler.setNativeZoom(t.skiaCanvas, zoom);

		Display d = t.display;

		Color col1 = d.getSystemColor(SWT.COLOR_RED);
		Color col2 = d.getSystemColor(SWT.COLOR_GREEN);

		t.addPaintListener(p -> fillRectangles(p, col1, col2));
		t.waitForExecution();

		Image i1 = t.extractImageFromClassic(); // extractImage(classicalCanvas);
		Image i2 = t.extractImageFromSkia(); // extractImage(skiaCanvas);

		t.assertImagesEqual(zoom, i1, i2);

		i1.dispose();
		i2.dispose();

		t.dispose();

	}

}
