/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.GCExtension;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.SkiaGC;

import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

public class SkiaRasterCanvasExtension extends RasterCanvasExtension implements ISkiaCanvas {

    final static int RECTANGLES_PER_LINE = 200;
    public static boolean SKIA_TEST_PERFORMANCE = false;

    private Matrix33 transformationMatrix = null;

    public static int convertSWTColorToSkijaColor(org.eclipse.swt.graphics.Color swtColor) {
	// extract RGB-components
	final int red = swtColor.getRed();
	final int green = swtColor.getGreen();
	final int blue = swtColor.getBlue();
	final int alpha = swtColor.getAlpha();

	// create ARGB 32-Bit-color
	final int skijaColor = (alpha << 24) | (red << 16) | (green << 8) | blue;

	return skijaColor;
    }

    private Surface surface;
    private final SkiaResources resources;

    public SkiaRasterCanvasExtension(Canvas canvas) {
	super(canvas);
	resources = new SkiaResources(canvas);
    }

    long lastStart = 0;
    int draws = 0;

    @Override
    public void doPaint(PaintEventSender s) {

	if (surface == null) {
	    return;
	}
	if (SKIA_TEST_PERFORMANCE) {
	    drawRectangles();
	    return;
	}

	surface.getCanvas().clear(SkiaGC.convertSWTColorToSkijaColor(canvas.getBackground()));

	final Event event = new Event();
	event.count = 1;

	final var size = canvas.getSize();

	final Rectangle eventBounds = new Rectangle(0, 0, size.x, size.y);
	event.setBounds(eventBounds);
	final GCData data = new GCData();
	data.device = canvas.getDisplay();
	// critical for drawing without clearing
	final SkiaGC gc = new SkiaGC(canvas, this ,  SWT.None);


	// here we need the skiaGC init not the gc init...
	// Note: use GC#setClipping(x,y,width,height) because GC#setClipping(Rectangle)
	// got broken by bug 446075
	//		gc.setClipping (eventBounds.x, eventBounds.y, eventBounds.width, eventBounds.height);
	event.gc = new GCExtension(gc);
	event.data = data;
	event.type = SWT.Paint;
	s.sendPaintEvent(event);
	gc.dispose();
	event.gc = null;

	SkiaCaretHandler.handleCaret(surface, canvas);
	surface.flush();



    }

    private void drawRectangles() {

	if (System.currentTimeMillis() - lastStart > 1000) {
	    System.out.println("Frames: " + draws); //$NON-NLS-1$
	    lastStart = System.currentTimeMillis();
	    draws = 0;
	}
	draws++;

	surface.getCanvas().clear(0xFFFFFFFF);

	final var size = canvas.getSize();

	long currentPosTime = System.currentTimeMillis();

	currentPosTime = currentPosTime % 10000;

	final double position = (double) currentPosTime / (double) 10000;

	final int shift = (int) (position * size.x);
	final int shiftDown = 20;

	final int[] col = new int[16];

	for (int i = 0; i < 16; i++) {
	    col[i] = convertSWTColorToSkijaColor(Display.getDefault().getSystemColor((i) % 16));
	}

	try (var paint = new Paint()) {

	    for (int x = 0; x < RECTANGLES_PER_LINE; x++) {
		for (int y = 0; y < RECTANGLES_PER_LINE; y++) {

		    final int left = shift + 2 * x;
		    final int top = shiftDown + 2 * y;
		    final int right = left + 2;
		    final int bottom = top + 2;

		    paint.setColor(col[(x + y) % 16]);
		    surface.getCanvas().drawRect(new Rect(left, top, right, bottom), paint);

		}
	    }
	}

    }

    @Override
    public void createSurface(long pointer, Point size, RasterImageInfo info) {

	final var s = canvas.getSize();
	final ColorAlphaType type = info.premule ? ColorAlphaType.PREMUL : ColorAlphaType.UNPREMUL;
	final ColorType t = getType(info);
	final ColorInfo ci = new ColorInfo(t, type, null);

	final ImageInfo imageInfo = new ImageInfo(ci, size.x, size.y);

	this.surface = Surface.makeRasterDirect(imageInfo, pointer, 4 * (size.x));

	if (info.transform) {

	    final var m1 = Matrix33.makeScale(1, -1);
	    final var m2 = Matrix33.makeTranslate(0, -s.y);

	    final var res =   m1.makeConcat(m2);

	    this.transformationMatrix = res;

	    surface.getCanvas().setMatrix(res);

	    //	    surface.getCanvas().scale(1, -1);
	    //	    surface.getCanvas().translate(0, -s.y);
	}

    }

    private ColorType getType(RasterImageInfo info) {

	return switch (info.colorType) {
	case RGBA_8888 -> ColorType.RGBA_8888;
	case BGRA_8888 -> ColorType.BGRA_8888;
	default -> ColorType.N32;
	};

    }

    @Override
    void preResize(Event e) {

	if (surface != null && !surface.isClosed()) {
	    surface.close();
	}

    }

    @Override
    public Matrix33 getTransformation() {

	return transformationMatrix;

    }

    @Override
    public Surface getSurface() {
	return surface;
    }

    @Override
    public SkiaResources getResources() {
	return this.resources;
    }

    @Override
    public Surface createSupportSurface(int width, int height) {
	return surface.makeSurface(width, height);
    }

}
