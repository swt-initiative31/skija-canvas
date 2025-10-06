package org.eclipse.swt.widgets;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.SkiaGC;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;

public class SkiaResources {

	private Paint foregroundPaint;
	private Paint backgroundPaint;
	private final Map<TextProperties, io.github.humbleui.skija.Image> textBlobs = new HashMap<>();
	private Color background;
	private Color foreground;
	private final Canvas canvas;
	private Font swtFont;
	private io.github.humbleui.skija.Font skiaFont;
	private float baseSymbolHeight;

	public SkiaResources(Canvas canvas) {
		this.canvas = canvas;
	}

	public void setBackground(Color color) {

		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		if (this.backgroundPaint != null) {
			this.backgroundPaint.close();
			this.backgroundPaint = null;
		}

		this.background = color;

	}

	public void setForeground(Color color) {
		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (this.foregroundPaint != null) {
			this.foregroundPaint.close();
			this.foregroundPaint = null;
		}

		this.foreground = color;

	}

	public Paint getForegroundPaint() {
		if (this.foregroundPaint == null) {

			this.foregroundPaint = new Paint();
			this.foregroundPaint.setColor(SkiaGC.convertSWTColorToSkijaColor(getForeground()));
			this.foregroundPaint.setAntiAlias(false);
			this.foregroundPaint.setAlpha(255);
			this.foregroundPaint.setBlendMode(BlendMode.SRC_OVER);

		}
		return this.foregroundPaint;
	}

	public Paint getBackgroundPaint() {
		if (this.backgroundPaint == null) {

			this.backgroundPaint = new Paint();
			this.backgroundPaint.setColor(SkiaGC.convertSWTColorToSkijaColor(getBackground()));
			this.backgroundPaint.setAntiAlias(false);

		}
		return this.backgroundPaint;
	}

	public Color getForeground() {
		if (foreground != null) {
			return foreground;
		}
		return canvas.getForeground();

	}

	public Color getBackground() {

		if (background != null) {
			return background;
		}

		return canvas.getBackground();
	}

	public void setFont(Font font) {
		if (font != null) {
			if (font.isDisposed()) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
		} else {
			font = getDefaultFont();
		}
		this.swtFont = font;
		this.skiaFont = SkiaGC.getSkijaFont(font);
		this.baseSymbolHeight = this.skiaFont.measureText("T").getHeight(); //$NON-NLS-1$

	}

	private org.eclipse.swt.graphics.Font getDefaultFont() {
		org.eclipse.swt.graphics.Font originalFont = canvas.getFont();

		if (originalFont == null || originalFont.isDisposed()) {
			originalFont = canvas.getDisplay().getSystemFont();
		}
		return originalFont;
	}

	public io.github.humbleui.skija.Font getSkiaFont() {
		if(skiaFont == null) {
			setFont(null);
		}
		return skiaFont;
	}

	public io.github.humbleui.skija.Image getTextImage(String text, int flags) {

		final TextProperties tp = new TextProperties(text, getSkiaFont().getSize(), getSkiaFont().getTypeface().getFamilyName(),
				(flags & SWT.TRANSPARENT) != 0, getBackgroundPaint().getColor(), getForegroundPaint().getColor());

		return this.textBlobs.get(tp);

	}

	public void setTextImage(String text, int flags, Image img) {

		this.textBlobs.put(new TextProperties(text, getSkiaFont().getSize(), getSkiaFont().getTypeface().getFamilyName(),
				(flags & SWT.TRANSPARENT) != 0, getBackgroundPaint().getColor(), getForegroundPaint().getColor()), img);

	}

}
