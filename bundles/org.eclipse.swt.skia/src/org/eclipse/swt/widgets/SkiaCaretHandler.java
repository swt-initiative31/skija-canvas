package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.SkiaGC;
import org.eclipse.swt.internal.canvasext.DPIScaler;

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
		return new Rect(DPIScaler.autoScaleUp(x), DPIScaler.autoScaleUp(y), DPIScaler.autoScaleUp(x + width),
				DPIScaler.autoScaleUp(y + height));
	}

	private static float getScaledOffsetValue(float width) {
		final boolean isDefaultLineWidth = width == 0;
		if (isDefaultLineWidth) {
			return 0.5f;
		}

		final float effectiveLineWidth = DPIScaler.autoScaleUp(width);
		if (effectiveLineWidth % 2 == 1) {
			return DPIScaler.autoScaleUp(0.5f);
		}
		return 0f;
	}

	private static Rect offsetRectangle(Rect rect) {
		final float scaledOffsetValue = getScaledOffsetValue(rect.getLeft() - rect.getRight());
		final float widthHightAutoScaleOffset = DPIScaler.autoScaleUp(1) - 1.0f;
		if (scaledOffsetValue != 0f) {
			return new Rect(rect.getLeft() + scaledOffsetValue, rect.getTop() + scaledOffsetValue,
					rect.getRight() + scaledOffsetValue + widthHightAutoScaleOffset,
					rect.getBottom() + scaledOffsetValue + widthHightAutoScaleOffset);
		}
		return rect;
	}

}
