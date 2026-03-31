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
package org.eclipse.swt.internal.skia;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.graphics.SkiaGC;
import org.eclipse.swt.widgets.Canvas;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

public class SkiaCaretHandler {

	public static void handleCaret(Surface s, Canvas c) {

		if (s == null || s.isClosed()) {
			return;
		}

		if (c == null || c.isDisposed()) {
			return;
		}
		final var car = c.getCaret();

		if (car == null || car.isDisposed()) {
			return;
		}

		final var b = car.getBounds();

		try (final Paint p = new Paint()) {
			p.setBlendMode(BlendMode.DIFFERENCE);
			p.setAlpha(255);
			p.setMode(PaintMode.FILL);
			p.setAntiAlias(false);
			// for some reason, the color must be inverted.
			p.setColor(SkiaGC.invertSWTColorToInt(c.getDisplay().getSystemColor(SWT.COLOR_BLACK)));
			final Rect scaled = SkiaGC.createScaledRectangleStatic(c, b.x, b.y, b.width, b.height);
			s.getCanvas().drawRect(SkiaGC.offsetRectangleStatic(c,scaled, 0), p);
		}

	}

}