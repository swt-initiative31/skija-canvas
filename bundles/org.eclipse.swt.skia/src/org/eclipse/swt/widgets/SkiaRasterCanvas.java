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
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;

public class SkiaRasterCanvas extends RasterCanvas implements ISkiaCanvas {

	private Surface surface;

	public SkiaRasterCanvas(Composite parent, int style) {
		super(parent, style);
	}

	@Override
	public void doPaint(PaintEventSender s) {

		if (surface == null) {
			return;
		}

		// TODO get bg color from canvas...
		surface.getCanvas().clear(0xFFFFFFFF);

		final Event event = new Event();
		event.count = 1;

		final var size = getSize();

		final Rectangle eventBounds = new Rectangle(0, 0, size.x, size.y);
		event.setBounds(eventBounds);
		final GCData data = new GCData();
		data.device = this.getDisplay();
		// critical for drawing without clearing
		final SkiaGC gc = new SkiaGC(this, SWT.None);


		// here we need the skiaGC init not the gc init...
		// Note: use GC#setClipping(x,y,width,height) because GC#setClipping(Rectangle)
		// got broken by bug 446075
		//		gc.setClipping (eventBounds.x, eventBounds.y, eventBounds.width, eventBounds.height);
		event.gc = new GCExtension(gc);
		event.type = SWT.Paint;
		s.sendPaintEvent(event);
		gc.dispose();
		event.gc = null;

		surface.flush();

	}

	@Override
	public void createSurface(long pointer, Point size, RasterImageInfo info) {

		final var s = getSize();
		final ColorAlphaType type = info.premule ? ColorAlphaType.PREMUL : ColorAlphaType.UNPREMUL;
		final ImageInfo imageInfo = ImageInfo.makeN32(size.x, size.y, type);

		this.surface = Surface.makeRasterDirect(imageInfo, pointer, 4 * (size.x));

		if (info.transform) {
			surface.getCanvas().scale(1, -1);
			surface.getCanvas().translate(0, -s.y);
		}

	}

	@Override
	void preResize(Event e) {

		if (surface != null && !surface.isClosed()) {
			surface.close();
		}

	}

	@Override
	public long internal_new_GC(GCData data) {
		return 0;
	}

	@Override
	public void internal_dispose_GC(long handle, GCData data) {

	}

	@Override
	public Surface getSurface() {
		return surface;
	}

}
