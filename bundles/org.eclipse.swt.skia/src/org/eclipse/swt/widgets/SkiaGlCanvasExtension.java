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
import org.eclipse.swt.graphics.DPIScaler;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.GCExtension;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.SkiaGC;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.opengl.OpenGLCanvasExtension;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorInfo;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.PixelGeometry;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceColorFormat;
import io.github.humbleui.skija.SurfaceOrigin;
import io.github.humbleui.skija.SurfaceProps;
import io.github.humbleui.types.Rect;

public class SkiaGlCanvasExtension extends OpenGLCanvasExtension implements ISkiaCanvasExtension, Listener {

	private final DirectContext skijaContext;
	private BackendRenderTarget renderTarget;
	private Surface surface;
	private final Canvas canvas;
	private SkiaResources resources;
	private Rectangle redrawRectangle;
	private DPIScaler scaler;
	private Image lastImage;

	private static final int SAMPLES = 0;

	public SkiaGlCanvasExtension(Canvas canvas) {
		this(canvas, createGLData());
		this.resources = new SkiaResources(canvas, this);
		this.scaler = new DPIScaler(canvas);
	}

	private static GLData createGLData() {
		final GLData data = new GLData();
		data.doubleBuffer = true;
		data.samples = SAMPLES;
		return data;
	}

	public SkiaGlCanvasExtension(Canvas canvas, GLData data) {
		super(canvas, data);

		setCurrent();
		skijaContext = DirectContext.makeGL();
		this.canvas = canvas;

		this.canvas.addListener(SWT.Resize, this::onResize);

	}

	private void onResize(Event e) {
		final Rectangle rect = this.canvas.getClientArea();

		final var scaled = resources.getScaler().scaleSize(rect.width, rect.height);

		if (renderTarget != null && !renderTarget.isClosed()) {
			renderTarget.close();
		}

		// System.out.println("CreateOpenGLRenderTarget"); //$NON-NLS-1$

		if (surface != null && !surface.isClosed()) {
			surface.close();
		}

		renderTarget = BackendRenderTarget.makeGL(scaled.x, scaled.y, /* samples */SAMPLES, /* stencil */0, /* fbid */0,
				FramebufferFormat.GR_GL_RGBA8);
		surface = Surface.makeFromBackendRenderTarget(skijaContext, renderTarget, SurfaceOrigin.BOTTOM_LEFT,
				SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB(),
				new SurfaceProps(PixelGeometry.RGB_H).withDeviceIndependentFonts(false));
		surface.getCanvas().clear(getBackroundForSkia());
		if (this.lastImage != null && !this.lastImage.isClosed()) {
			this.lastImage.close();
		}

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

	// @Override
	// public void createSurface(long pointer, Point size, RasterImageInfo info) {
	// final Rectangle rect = this.canvas.getClientArea();
	// renderTarget = BackendRenderTarget.makeGL(rect.width, rect.height, /* samples
	// */32, /* stencil */0, /* fbid */0,
	// FramebufferFormat.GR_GL_RGBA8);
	//
	// surface = Surface.makeFromBackendRenderTarget(skijaContext, renderTarget,
	// SurfaceOrigin.BOTTOM_LEFT,
	// SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB(), new
	// SurfaceProps(PixelGeometry.RGB_H));
	// // surface.getCanvas().clear(getBackroundForSkia());
	//
	// }

	@Override
	public void redrawTriggered(int x, int y, int width, int height, boolean all) {
		this.redrawRectangle = new Rectangle(x, y, width, height);
	}

	@Override
	public void redrawTriggered() {
		this.redrawRectangle = null;
		super.redrawTriggered();
	}

	@Override
	public void doPaint(PaintEventSender e) {

		if (surface == null) {
			return;
		}

		setCurrent();

		boolean redrawWithClipping = false;
		final var size = getSize();
		Rectangle bounds = null;
		this.redrawRectangle = null;
		if (this.redrawRectangle != null) {
			if (this.lastImage != null) {
				surface.getCanvas().drawImage(lastImage, 0, 0);
				this.lastImage.close();
				this.lastImage = null;
			}
			final var ca = canvas.getClientArea();
			ca.intersect(this.redrawRectangle);
			bounds = ca;

			if (bounds.width == 0 || bounds.height == 0) {
				skijaContext.flush();
				return;
			}

			redrawWithClipping = true;
			final var r = resources.getScaler().scaleBounds(redrawRectangle, DPIUtil.getDeviceZoom());
			surface.getCanvas().clipRect(new Rect(r.x, r.y, r.x + r.width, r.y + r.height));

		} else {
			surface.getCanvas().clear(getBackroundForSkia());
			bounds = new Rectangle(0, 0, size.x, size.y);
		}

		final Event event = new Event();
		event.count = 1;

		event.setBounds(bounds);
		final GCData data = new GCData();
		data.device = this.canvas.getDisplay();
		// critical for drawing without clearing
		final SkiaGC gc = new SkiaGC(canvas, this, SWT.None);
		event.gc = new GCExtension(gc);
		event.display = this.canvas.getDisplay();
		e.sendPaintEvent(event);
		gc.dispose();
		event.gc = null;
		SkiaCaretHandler.handleCaret(surface, canvas);

		if (this.lastImage != null && !this.lastImage.isClosed()) {
			this.lastImage.close();
		}
		this.redrawRectangle = null;
		if(surface != null && surface.getCanvas() != null && !surface.getCanvas().isClosed()) {
			this.lastImage = surface.makeImageSnapshot();
		}else {
			this.lastImage = null;
		}
		if (redrawWithClipping) {
			surface.getCanvas().restore();
		}
		skijaContext.flush();

	}

	private int getBackroundForSkia() {
		return SkiaGC.convertSWTColorToSkijaColor(canvas.getBackground());
	}

	@Override
	public SkiaResources getResources() {
		return this.resources;
	}

	private Point getSize() {

		final var c = canvas.getSize();
		final var p = resources.getScaler().scaleSize(c.x, c.y);
		final var s = new Point(p.x, p.y);

		return s;
	}

	@Override
	public Surface createSupportSurface(int width, int height) {
		final ImageInfo i = new ImageInfo(new ColorInfo(ColorType.RGBA_8888, ColorAlphaType.PREMUL, null), width,
				height);
		final var subSurface = Surface.makeRenderTarget(skijaContext, true, i);
		return subSurface;
	}

	@Override
	public DPIScaler getScaler() {
		return scaler;
	}

	@Override
	public void handleEvent(Event event) {
	}

	@Override
	public void createSurface(long pointer, Point size, RasterImageInfo info) {
		// TODO Auto-generated method stub

	}

}
