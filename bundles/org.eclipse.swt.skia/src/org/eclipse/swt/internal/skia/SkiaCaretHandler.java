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
import org.eclipse.swt.internal.canvasext.DpiScaler;
import org.eclipse.swt.internal.graphics.SkiaGC;
import org.eclipse.swt.widgets.Canvas;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;

public class SkiaCaretHandler {

	public static void handleCaret( Surface s, Canvas c  ) {

		if(s == null || s.isClosed()) {
			return;
		}

		if(c == null || c.isDisposed()) {
			return;
		}
		final var car = c.getCaret();

		if(car == null || car.isDisposed()) {
			return;
		}

		final var b = car.getBounds();

		try ( final Paint p = new Paint() )   {

			p.setColor(SkiaGC.convertSWTColorToSkijaColor(c.getForeground()));
			p.setBlendMode(BlendMode.DIFFERENCE);
			p.setAlpha(255);
			p.setMode(PaintMode.FILL);
			p.setAntiAlias(false);
			// for some reason, the color must be inverted.
			p.setColor(SkiaGC.invertSWTColorToInt(c.getDisplay().getSystemColor(SWT.COLOR_BLACK)));
			s.getCanvas().drawRect(offsetRectangle(createScaledRectangle(b.x, b.y, b.width, b.height)), p);

		}

	}

	private static Rect createScaledRectangle(int x, int y, int width, int height) {
		return new Rect(DpiScaler.autoScaleUp(x), DpiScaler.autoScaleUp(y), DpiScaler.autoScaleUp(x + width),
				DpiScaler.autoScaleUp(y + height));
	}

	private static float getScaledOffsetValue(float width) {
		final boolean isDefaultLineWidth = width == 0;
		if (isDefaultLineWidth) {
			return 0.5f;
		}

		final float effectiveLineWidth = DpiScaler.autoScaleUp(width);
		if (effectiveLineWidth % 2 == 1) {
			return DpiScaler.autoScaleUp(0.5f);
		}
		return 0f;
	}

	private static Rect offsetRectangle(Rect rect) {
		final float scaledOffsetValue = getScaledOffsetValue(rect.getLeft() - rect.getRight());
		final float widthHightAutoScaleOffset = DpiScaler.autoScaleUp(1) - 1.0f;
		if (scaledOffsetValue != 0f) {
			return new Rect(rect.getLeft() + scaledOffsetValue, rect.getTop() + scaledOffsetValue,
					rect.getRight() + scaledOffsetValue + widthHightAutoScaleOffset,
					rect.getBottom() + scaledOffsetValue + widthHightAutoScaleOffset);
		}
		return rect;
	}

}
