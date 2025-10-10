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
import io.github.humbleui.skija.Surface;

public class SkiaRasterCanvasExtension extends RasterCanvasExtension implements ISkiaCanvas {

	private Surface surface;
	private final SkiaResources resources;

	public SkiaRasterCanvasExtension(Canvas canvas) {
		super(canvas);
		resources = new SkiaResources(canvas);
	}

	@Override
	public void doPaint(PaintEventSender s) {

		if (surface == null) {
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

		surface.flush();

	}

	@Override
	public void createSurface(long pointer, Point size, RasterImageInfo info) {

		final var s = canvas.getSize();
		final ColorAlphaType type = info.premule ? ColorAlphaType.PREMUL : ColorAlphaType.UNPREMUL;
		final ColorType t = getType(  info ) ;
		final ColorInfo ci = new ColorInfo(t, type, null);

		final ImageInfo imageInfo = new ImageInfo(ci, size.x, size.y);

		this.surface = Surface.makeRasterDirect(imageInfo, pointer, 4 * (size.x));

		if (info.transform) {
			surface.getCanvas().scale(1, -1);
			surface.getCanvas().translate(0, -s.y);
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
	public Surface getSurface() {
		return surface;
	}

	@Override
	public SkiaResources getResources() {
		return this.resources;
	}

	@Override
	public Surface createSupportSurface(int width, int height) {
		return  surface.makeSurface(width,height);
	}


}
