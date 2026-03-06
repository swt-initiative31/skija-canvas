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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

public class CanvasCompareTool {

	final static int MAX_DIFF = 0;

	public static boolean SHOW_COMPARE_VIEW = false;

	private static int WAIT_IN_SECONDS = 0; // present the comparisons for xxx seconds

	boolean twoSkiaCanvas = false;

	Display display;
	Shell shell;
	Canvas classicalCanvas;
	Canvas skiaCanvas;

	List<PaintListener> listeners = new ArrayList<>();
	AtomicBoolean classicExecuted = new AtomicBoolean();
	AtomicBoolean skiaExecuted = new AtomicBoolean();

	public synchronized void init(Layout layout) {

		display = new Display();
		shell = new Shell(display);
		shell.setLayout(layout);
		shell.setSize(1000, 1000);

		if (twoSkiaCanvas) {
			SkiaConfiguration.activateSkiaGl();
			classicalCanvas = new Canvas(shell, SkiaConfiguration.SKIA);
		} else {
			classicalCanvas = new Canvas(shell, SWT.NONE);
		}
		SkiaConfiguration.activateSkiaGl();
		skiaCanvas = new Canvas(shell, SkiaConfiguration.SKIA);

		shell.addListener(SWT.Resize, (e) -> {

			if (!shell.isDisposed()) {

				var ca = shell.getClientArea();

				var h = ca.height;
				var w = ca.width;

				var canvasHeight = h - 10;
				var canvasWidth = h / 2 - 10;

				classicalCanvas.setBounds(3, 3, canvasWidth, canvasHeight);
				skiaCanvas.setBounds(w / 2 + 3, 3, canvasWidth, canvasHeight);

			}

		});

		shell.open();

	}

	public void cleanup() {

		this.listeners.forEach(e -> {
			classicalCanvas.removePaintListener(e);
			skiaCanvas.removePaintListener(e);
		});

		this.listeners.clear();
		classicExecuted.set(false);
		skiaExecuted.set(false);

	}

	public void dispose() {

		if (classicalCanvas != null)
			classicalCanvas.dispose();
		if (skiaCanvas != null)
			skiaCanvas.dispose();
		if (shell != null)
			shell.dispose();
		if (display != null)
			display.dispose();

	}

	void addPaintListener(PaintListener pe) {

		PaintListener classicPaintListener = e -> {
			pe.paintControl(e);
			classicExecuted.set(true);
		};
		PaintListener skiaPaintListener = e -> {
			pe.paintControl(e);
			skiaExecuted.set(true);
		};

		classicalCanvas.addPaintListener(classicPaintListener);
		skiaCanvas.addPaintListener(skiaPaintListener);

		this.listeners.add(classicPaintListener);
		this.listeners.add(skiaPaintListener);

	}

	void removePaintListeners() {

		for (var l : this.listeners) {
			classicalCanvas.removePaintListener(l);
			skiaCanvas.removePaintListener(l);
		}

		this.listeners.clear();

	}

	public void waitForExecution() {
		waitForExecution(null, true);
	}

	public void waitForExecution(Rectangle r, boolean expectingRedraw) {

		classicExecuted.set(false);
		skiaExecuted.set(false);

		long start = System.currentTimeMillis();
		if (r == null) {
			classicalCanvas.redraw();
			skiaCanvas.redraw();
		} else {
			classicalCanvas.redraw(r.x, r.x, r.width, r.height, true);
			skiaCanvas.redraw(r.x, r.x, r.width, r.height, true);
		}

		while (!shell.isDisposed() // is the shell is disposed, the test is definetly over
				&& (Display.getDefault().readAndDispatch() // if there are no more redraw events, we can continue
						|| (expectingRedraw && (!classicExecuted.get() && !skiaExecuted.get()))) // if we expect the redraw and the canvas was
																									// drawn, we can
																									// check the image
				|| System.currentTimeMillis() - start < WAIT_IN_SECONDS * 1000 // if the extended waiting time is set,
																				// we have to wait longer
		) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (expectingRedraw)
			if (!classicExecuted.get() || !skiaExecuted.get()) {
				throw new IllegalStateException("No redraw on both canvases..");
			}

	}

	public Image extractImageFromClassic() {
		return extractImage(classicalCanvas);
	}

	private Image extractImage(Canvas canvas) {

		Rectangle bounds = canvas.getBounds();
		Image image = new Image(Display.getDefault(), bounds.width, bounds.height);
		GC gc = new GC(canvas);
		gc.copyArea(image, 0, 0);
		gc.dispose();

		return image;
	}

	public Image extractImageFromSkia() {
		return extractImage(skiaCanvas);
	}

	private void assertTextAreaEquals(TextAreaPosition ta1, TextAreaPosition ta2) {

		assertEquals(ta1.canvasBackground, ta2.canvasBackground);
		assertEquals(ta1.bgColor, ta2.bgColor);
		approximatelyEquals(ta1.backgroundArea, ta2.backgroundArea);
		approximatelyEquals(ta1.foregroundArea, ta2.foregroundArea);

	}

	private void approximatelyEquals(Rectangle fa1, Rectangle fa2) {

		int diffX = fa1.x - fa2.x;
		int diffY = fa1.y - fa2.y;

		int diffWidth = fa1.width - fa2.width;
		int diffHeight = fa1.height - fa2.height;

		if (Math.abs(diffWidth) > MAX_DIFF || Math.abs(diffHeight) > MAX_DIFF || Math.abs(diffX) > MAX_DIFF
				|| Math.abs(diffY) > MAX_DIFF) {
			assertEquals(fa1, fa2);
		}

	}

	public void assertImagesEqual(int zoom, Image i1, Image i2) {

		var data1 = i1.getImageData(100);
		var data2 = i2.getImageData(100);

		assertEquals(data1.width, data2.width, "Widths not equal for zoom: " + zoom);
		assertEquals(data1.height, data2.height, "Heights not equal for zoom: " + zoom);

		TextAreaPosition ta1 = new TextAreaPosition(data1);
		TextAreaPosition ta2 = new TextAreaPosition(data1);

		assertTextAreaEquals(ta1, ta2);

	}

}
