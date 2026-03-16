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

import java.util.function.*;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
/**
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class OpenGLCanvasExtension extends GLCanvasExtension {
	/**
	 * @noreference This class is not intended to be referenced by clients.
	 */
	public OpenGLCanvasExtension(Canvas canvas, GLData data) {
		super(canvas, data);
	}

	public Object paint(Consumer<Event> paintEventSender,long arg1, long arg2) {

		if (canvas.isDisposed())
			return null;

		if(!isCurrent())
		{
			setCurrent();
		}
		doPaint(paintEventSender);
		swapBuffers();

		return null;
	}

	abstract public void preResize(Event e);

	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(Consumer<Event> paintEventSender);

	public void redrawTriggered() {
	}

}
