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
package org.eclipse.swt.opengl;

import org.eclipse.swt.widgets.*;

public abstract class OpenGLCanvasExtension extends GLCanvasExtension  {

	private boolean redrawTriggered;
	private Canvas canvas;

	public OpenGLCanvasExtension(Canvas canvas, GLData data) {
		super(canvas, data);
		this.canvas = canvas;
	}

	@Override
	public Object paint(PaintEventSender e, long wParam, long lParam) {

		if (canvas.isDisposed())
			return null;
		this.redrawTriggered = false;
		doPaint(e);
		swapBuffers();

		if(redrawTriggered) {
			canvas.redraw();
			redrawTriggered = false;
			return 0;
		}

		return null;

	}

	@Override
	public void redrawTriggered() {
		this.redrawTriggered = true;
	}

	abstract public void preResize(Event e);

//	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender e);

}
