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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test_org_eclipse_swt_widgets_SkiaCanvasCompare {

	static Shell shell;
	static Display display;
	static Canvas classicalCanvas;
	static Canvas skiaCanvas;

	List<PaintListener> listeners = new ArrayList<>();
	AtomicBoolean classicExecuted = new AtomicBoolean();
	AtomicBoolean skiaExecuted = new AtomicBoolean();

	@BeforeAll
	public static void classSetUp() {

		display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());

		classicalCanvas = new Canvas(shell, 0);
		SkiaConfiguration.activateSkiaGl();
		skiaCanvas = new Canvas(shell, SkiaConfiguration.SKIA);

		shell.setSize(400, 400);

		shell.open();

	}

	@BeforeEach
	public void setUp() {

		this.listeners.forEach(e -> {
			classicalCanvas.removePaintListener(e);
			skiaCanvas.removePaintListener(e);
		});

		this.listeners.clear();

		classicExecuted.set(false);
		skiaExecuted.set(false);

	}

	@AfterAll
	public static void classTearDown() {
		classicalCanvas.dispose();
		skiaCanvas.dispose();
		shell.dispose();
		display.dispose();
	}

	static void fillRectangles(PaintEvent e, Color col1, Color col2) {

		Canvas c = (Canvas) e.widget;
		var s = c.getSize();
		e.gc.setBackground(col1);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x, s.y));

		e.gc.setBackground(col2);
		e.gc.fillRectangle(new Rectangle(0, 0, s.x / 2, s.y / 2));

	}

	void addPaintListener( PaintListener pe ) {

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

	@Test
	public void test_org_eclipse_swt_skia_fillRectangle() {



		Color col1 = display.getSystemColor(SWT.COLOR_RED);
		Color col2 = display.getSystemColor(SWT.COLOR_GREEN);


		addPaintListener( p -> fillRectangles(p, col1, col2) );

		classicalCanvas.redraw();
		skiaCanvas.redraw();

		while (!shell.isDisposed() && !(classicExecuted.get() && skiaExecuted.get())) {
			display.readAndDispatch();
		}

		if (classicExecuted.get() && skiaExecuted.get()) {
			Image i1 = extractImage(classicalCanvas);
			Image i2 = extractImage(skiaCanvas);

			assertImageColorEquals(i1, i2);

			i1.dispose();
			i2.dispose();

		} else {
			throw new IllegalStateException("No redraw on both canvases..");
		}

	}


	private static void drawText(String text, PaintEvent e, Color col1, Color col2) {

		e.gc.setBackground(col1);
		e.gc.setForeground(col2);

		e.gc.drawText(text, 10, 10);

	}

	@Test
	public void test_org_eclipse_swt_skia_drawText() {

		Color col1 = display.getSystemColor(SWT.COLOR_RED);
		Color col2 = display.getSystemColor(SWT.COLOR_GREEN);

		addPaintListener(p ->  drawText("Compare Text", p, col1, col2));


		classicalCanvas.redraw();
		skiaCanvas.redraw();

		while (!shell.isDisposed() && !(classicExecuted.get() && skiaExecuted.get())) {
			display.readAndDispatch();
		}

		if (classicExecuted.get() && skiaExecuted.get()) {
			Image i1 = extractImage(classicalCanvas);
			Image i2 = extractImage(skiaCanvas);

			assertTextsAreEquals(i1, i2);

			i1.dispose();
			i2.dispose();

		} else {
			throw new IllegalStateException("No redraw on both canvases..");
		}

	}

	private void assertTextsAreEquals(Image i1, Image i2) {
		// TODO use an algorithm to check whether the size,position and colors are
		// similar
	}

	private void assertImageColorEquals(Image i1, Image i2) {

		var data1 = i1.getImageData(100);
		var data2 = i2.getImageData(100);

		assertEquals(data1.width, data2.width);
		assertEquals(data1.height, data2.height);

		for (int i = 0; i < data1.width; i++) {
			for (int j = 0; j < data1.height; j++) {
				var pix1 = data1.getPixel(i, j);
				var pix2 = data2.getPixel(i, j);
				assertEquals(pix1, pix2);
			}
		}
	}

	private Image extractImage(Canvas canvas) {

		Rectangle bounds = canvas.getBounds();
		Image image = new Image(display, bounds.width, bounds.height);
		GC gc = new GC(canvas);
		gc.copyArea(image, 0, 0);
		gc.dispose();

		return image;
	}

}
