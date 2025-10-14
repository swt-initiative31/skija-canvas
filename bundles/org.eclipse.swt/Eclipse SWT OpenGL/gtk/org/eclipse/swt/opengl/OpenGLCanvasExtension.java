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

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

public abstract class OpenGLCanvasExtension extends GLCanvasExtension implements IExternalCanvasHandler {

	private boolean redrawTriggered;

	public OpenGLCanvasExtension(Canvas canvas, GLData data) {
		super(canvas, data);
	}

	@Override
	public Object paint(PaintEventSender e,long arg1, long arg2) {

		if (canvas.isDisposed())
			return null;

		if(!isCurrent())
		{
			setCurrent();
		}
		doPaint(e);
		swapBuffers();

		return null;
	}

	abstract public void preResize(Event e);

	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender e);

	@Override
	public void redrawTriggered() {
		this.redrawTriggered = true;
	}

}
