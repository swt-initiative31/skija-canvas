package org.eclipse.swt.widgets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.DPIScaler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.SkiaGC;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Typeface;

public class SkiaResources {

	private Paint foregroundPaint;
	private Paint backgroundPaint;
	private final Map<TextProperties, io.github.humbleui.skija.Image> textBlobs = new HashMap<>();
	private final Map<PaintProperties, io.github.humbleui.skija.Paint> paintCache = new HashMap<>();
	private Color background;
	private Color foreground;
	private final Canvas canvas;
	private Font swtFont;
	private io.github.humbleui.skija.Font skiaFont;
	private float baseSymbolHeight;

	private final Map<FontData, io.github.humbleui.skija.Font> fontCache = new ConcurrentHashMap<>();

	private final DPIScaler scaler;

	public SkiaResources(Canvas canvas) {
		this.canvas = canvas;
		scaler = new DPIScaler(canvas);
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
			this.foregroundPaint = null;
		}

		this.foreground = color;

	}

	public Paint getForegroundPaint() {
		if (this.foregroundPaint == null) {

			final var prop = new PaintProperties(SkiaGC.convertSWTColorToSkijaColor(getForeground()), true, 255);

			paintCache.get(prop);

			this.foregroundPaint = paintCache.computeIfAbsent(prop, p -> {

				final var pa = new Paint();
				pa.setColor(p.color());
				pa.setAntiAlias(p.antialias());
				pa.setAlpha(p.alpha());
				pa.setBlendMode(BlendMode.SRC_OVER);
				pa.setMode(PaintMode.STROKE);
				return pa;

			});

		}
		return this.foregroundPaint;
	}

	public Paint getBackgroundPaint() {
		if (this.backgroundPaint == null) {

			this.backgroundPaint = new Paint();
			this.backgroundPaint.setColor(SkiaGC.convertSWTColorToSkijaColor(getBackground()));
			this.backgroundPaint.setAntiAlias(false);
			this.backgroundPaint.setMode(PaintMode.FILL);

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
		this.skiaFont = getSkijaFont(font);
		this.baseSymbolHeight = this.skiaFont.measureText("T").getHeight(); //$NON-NLS-1$

	}

	private io.github.humbleui.skija.Font getSkijaFont(org.eclipse.swt.graphics.Font font) {
		final FontData fontData = font.getFontData()[0];
		var cachedFont = fontCache.get(fontData);
		if (cachedFont != null && cachedFont.isClosed()) {
			fontCache.remove(fontData);
			cachedFont = null;
		}

		io.github.humbleui.skija.Font f = null;

		if (cachedFont == null) {
			f = createSkijaFont(fontData);
			fontCache.put(fontData, f);
			return f;
		}

		return fontCache.get(fontData);
	}

	private io.github.humbleui.skija.Font createSkijaFont(FontData fontData) {
		FontStyle style = FontStyle.NORMAL;
		final boolean isBold = (fontData.getStyle() & SWT.BOLD) != 0;
		final boolean isItalic = (fontData.getStyle() & SWT.ITALIC) != 0;
		if (isBold && isItalic) {
			style = FontStyle.BOLD_ITALIC;
		} else if (isBold) {
			style = FontStyle.BOLD;
		} else if (isItalic) {
			style = FontStyle.ITALIC;
		}
		final io.github.humbleui.skija.Font skijaFont = new io.github.humbleui.skija.Font(
				Typeface.makeFromName(fontData.getName(), style));

		// System.out.println("Canvas native zoom: " + canvas.nativeZoom);
		// System.out.println("Height: " + fontData.getHeight());
		// System.out.println("DeviceZoom: " + scaler.getDeviceZoom());
		// System.out.println("NativeDeviceZoom: " + scaler.getNativeDeviceZoom());
		//
		// System.out.println("Util: DeviceZoom: " + DPIUtil.getDeviceZoom());
		// System.out.println("Util: NativeDeviceZoom: " +
		// DPIUtil.getNativeDeviceZoom());

		int fontSize = (fontData.getHeight());
		if (SWT.getPlatform().equals("win32")) { //$NON-NLS-1$
			fontSize = scaler.getZoomedFontSize(fontSize);
		}
		if (SWT.getPlatform().equals("gtk") || true) { //$NON-NLS-1$
			// SWT's font size is in points, 1pt = 1/72 inch, adjust skija font size to this
			fontSize = (fontSize * Display.getDefault().getDPI().y) / 72;
		}
		skijaFont.setSize(fontSize);
		// --------------------------------------------------
		// This might be the same option like the windows gdi text antialias option.
		skijaFont.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
		skijaFont.setSubpixel(true);
		skijaFont.setHinting(FontHinting.NORMAL);
		skijaFont.setAutoHintingForced(true);

		final var m = skijaFont.getMetrics();

		final var asc = m.getAscent();
		final var des = m.getDescent();

		final var ascI = (int) Math.ceil(Math.abs(asc));
		final var desI = (int) Math.ceil(Math.abs(des));
		final var heightI = ascI + desI;

		//		System.out.println("Fontsize: AscF:" + m.getAscent() + "  DescF:" + m.getDescent() + " HeightF:" + m.getHeight()
		//		+ "AscI:" + ascI + "  DescI:" + desI + " HeightI:" + heightI);

		// --------------------------------------------------
		return skijaFont;
	}

	private org.eclipse.swt.graphics.Font getDefaultFont() {
		org.eclipse.swt.graphics.Font originalFont = canvas.getFont();

		if (originalFont == null || originalFont.isDisposed()) {
			originalFont = canvas.getDisplay().getSystemFont();
		}
		return originalFont;
	}

	public io.github.humbleui.skija.Font getSkiaFont() {
		if (skiaFont == null) {
			setFont(null);
		}
		return skiaFont;
	}

	public io.github.humbleui.skija.Image getTextImage(String text, int flags) {

		final TextProperties tp = new TextProperties(text, getSkiaFont().getSize(),
				getSkiaFont().getTypeface().getFamilyName(), (flags & SWT.TRANSPARENT) != 0,
				getBackgroundPaint().getColor(), getForegroundPaint().getColor());

		return this.textBlobs.get(tp);

	}

	public void setTextImage(String text, int flags, Image img) {

		this.textBlobs.put(new TextProperties(text, getSkiaFont().getSize(),
				getSkiaFont().getTypeface().getFamilyName(), (flags & SWT.TRANSPARENT) != 0,
				getBackgroundPaint().getColor(), getForegroundPaint().getColor()), img);

	}

	public DPIScaler getScaler() {
		return scaler;
	}

	private void resetResources() {

		skiaFont.close();
		foregroundPaint.close();
		backgroundPaint.close();
		textBlobs.forEach((t, u) -> {
			if (u != null && !u.isClosed()) {
				u.close();
			}
		});

		textBlobs.forEach((t, u) -> {
			if (u != null && !u.isClosed()) {
				u.close();
			}
		});

		fontCache.clear();
		textBlobs.clear();

		skiaFont = null;
		foregroundPaint = null;
		backgroundPaint = null;

	}

	public Font getFont() {

		if (swtFont != null) {
			if (swtFont.isDisposed()) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
		} else {
			swtFont = getDefaultFont();
		}

		return swtFont;
	}

	public void resetBaseColors() {
		foreground = null;
		background = null;
		foregroundPaint = null;
		backgroundPaint = null;

	}

}
