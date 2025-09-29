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

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.cocoa.*;
import org.eclipse.swt.widgets.RasterImageInfo.*;

abstract class RasterCanvasExtension implements Listener, IExternalCanvasHandler {

    private long memoryPointer;
    private boolean triggerRedraw;

    NSBitmapImageRep imageRep;
    NSImage i;
    protected Canvas canvas;

    public RasterCanvasExtension(Canvas canvas) {
	this.canvas = canvas;
	canvas.addListener(SWT.Resize, this);
    }

    @Override
    public void handleEvent(Event e) {
	if (e.type == SWT.Resize) {

	    onResize(e);

	}
    }

    @Override
    public Object paint(PaintEventSender e, long wParam, long lParam) {
	// This is a complete override of the paint logic

	var s = canvas.getSize();
	int width = s.x;
	int height = s.y;

	canvas.getDisplay().isPainting.addObject(canvas.view);
	doPaint(e);

	// bitmapData() is the flush operation on mac for imageRepresentation.
	imageRep.bitmapData();

	pushImage(i, width, height);

	if (triggerRedraw) {
	    canvas.redraw();
	    triggerRedraw = false;
	}

	canvas.getDisplay().isPainting.removeObject(canvas.view);
	return null;

    }

    abstract void preResize(Event e);

    abstract void createSurface(long pointer, Point size, RasterImageInfo info);

    public abstract void doPaint(PaintEventSender e);

    private void createRepresentation(int width, int height) {

	imageRep = (NSBitmapImageRep) new NSBitmapImageRep().alloc();

	boolean hasAlpha = true;

	imageRep = imageRep.initWithBitmapDataPlanes(0, width, height, 8, hasAlpha ? 4 : 3, hasAlpha, false,
		OS.NSDeviceRGBColorSpace, OS.NSAlphaNonpremultipliedBitmapFormat, width * 4, 32);

	memoryPointer = imageRep.bitmapData();

	createSurface(memoryPointer, new Point(width, height),
		new RasterImageInfo(false, RasterColorType.RGBA8888, true));

    }

    private void pushImage(NSImage i, int width, int height) {

	NSGraphicsContext handle = NSGraphicsContext.currentContext();
	handle.saveGraphicsState();

	NSRect srcRect = new NSRect();
	srcRect.x = 0;
	srcRect.y = 0;
	srcRect.width = width;
	srcRect.height = height;
	NSRect destRect = new NSRect();
	destRect.x = 0;
	destRect.y = 0;
	destRect.width = srcRect.width;
	destRect.height = srcRect.height;

	i.drawInRect(destRect, srcRect, OS.NSCompositeSourceOver, 1);

	handle.restoreGraphicsState();

    }

    void onResize(Event e) {

	var s = canvas.getSize();
	int width = s.x;
	int height = s.y;

	preResize(null);

	if (imageRep != null) {
	    imageRep.release();
	}
	if (i != null) {
	    i.release();
	}

	createRepresentation(width, height);

	i = new NSImage();
	i.alloc();

	NSSize size = new NSSize();
	size.width = width;
	size.height = height;

	i.initWithSize(size);
	i.setCacheMode(OS.NSImageCacheNever);

	i.addRepresentation(imageRep);

    }

    @Override
    public void redrawTriggered() {
	triggerRedraw = true;
    }

}
