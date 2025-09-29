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
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.opengl.OpenGLCanvasExtension;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;

public class SkiaGlCanvasExtension extends OpenGLCanvasExtension implements ISkiaCanvas {

	private final DirectContext skijaContext;
	private BackendRenderTarget renderTarget;
	private Surface surface;
	private final Canvas canvas;

	private static final int SAMPLES = 0;

	public SkiaGlCanvasExtension(Canvas canvas) {
		this(canvas,createGLData());
	}

	private static GLData createGLData() {
		final GLData data = new GLData();
		data.doubleBuffer = true;
		data.samples = SAMPLES;
		return data;
	}

	public SkiaGlCanvasExtension(Canvas canvas, GLData data) {
		super(canvas,data);

		setCurrent();
		skijaContext = DirectContext.makeGL();
		this.canvas = canvas;


		this.canvas.addListener(SWT.Resize, this::onResize);

	}

	private void onResize(Event e) {
		final Rectangle rect = this.canvas.getClientArea();

		renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */SAMPLES, /* stencil */0,
				/* fbid */0, FramebufferFormat.GR_GL_RGBA8);

		System.out.println("CreateOpenGLRenderTarget");

		if (surface != null && !surface.isClosed()) {
			surface.close();
		}

		surface = Surface.makeFromBackendRenderTarget(skijaContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
				SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(), new SurfaceProps(PixelGeometry.RGB_H));
		surface.getCanvas().clear(getBackroundForSkia());

	}


	@Override
	public Surface getSurface() {
		return surface;
	}


	@Override
	public void preResize(Event e) {
		if (surface != null && !surface.isClosed()) {
			surface.close();
		}
		this.canvas.redraw();
	}

	@Override
	public void createSurface(long pointer, Point size, RasterImageInfo info) {
		final Rectangle rect = this.canvas.getClientArea();
		renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */32, /* stencil */0,
				/* fbid */0, FramebufferFormat.GR_GL_RGBA8);
		surface = Surface.makeFromBackendRenderTarget(skijaContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
				SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(), new SurfaceProps(PixelGeometry.RGB_H));
		surface.getCanvas().clear(getBackroundForSkia());

	}

	@Override
	public void doPaint(PaintEventSender e) {

		if (surface == null) {
			return;
		}

		surface.getCanvas().clear(getBackroundForSkia());


		final Event event = new Event ();
		event.count = 1;

		final var size = canvas.getSize();

		final Rectangle eventBounds = new Rectangle (0, 0, size.x, size.y);
		event.setBounds (eventBounds);
		final GCData data = new GCData ();
		data.device = this.canvas.getDisplay();
		// critical for drawing without clearing
		final SkiaGC gc = new SkiaGC(canvas, this,SWT.None);
		event.gc = new GCExtension(gc);
		e.sendPaintEvent(event);
		gc.dispose ();
		event.gc = null;

		skijaContext.flush();
	}

	@Override
	public void handleEvent(Event event) {

		if(event.type == SWT.Resize) {
			final Rectangle rect = canvas.getClientArea();

			renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples */SAMPLES, /* stencil */0,
					/* fbid */0, FramebufferFormat.GR_GL_RGBA8);

			System.out.println("CreateOpenGLRenderTarget");

			if (surface != null && !surface.isClosed()) {
				surface.close();
			}

			surface = Surface.makeFromBackendRenderTarget(skijaContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
					SurfaceColorFormat.RGBA_8888, ColorSpace.getDisplayP3(), new SurfaceProps(PixelGeometry.RGB_H));
			surface.getCanvas().clear(0xFFFFFFFF);
		}

	}
	private int getBackroundForSkia() {
		return SkiaGC.convertSWTColorToSkijaColor(canvas.getBackground());
	}

}
