package org.eclipse.swt.internal.skia;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.canvasext.DpiScaler;
import org.eclipse.swt.internal.canvasext.FontProperties;
import org.eclipse.swt.internal.skia.cache.ImageKey;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontSlant;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;

public class SkiaResources {
	private final static boolean USE_IMAGE_CACHE = true;

	private Color background;
	private Color foreground;
	private final Canvas canvas;
	private Font swtFont;
	private io.github.humbleui.skija.Font skiaFont;

	private final Map<FontProperties, io.github.humbleui.skija.Font> fontCache = new ConcurrentHashMap<>();
	private final Map<ImageKey, io.github.humbleui.skija.Image> imageCache = new HashMap<>();
	private final ISkiaCanvasExtension skiaExtension;

	public SkiaResources(Canvas canvas, ISkiaCanvasExtension skiaExtension) {
		this.canvas = canvas;
		this.skiaExtension = skiaExtension;
		this.canvas.addListener(SWT.Dispose, e -> resetResources());
	}

	public void setBackground(Color color) {

		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.background = color;
	}

	public void setForeground(Color color) {
		if (color == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.foreground = color;
	}

	public Color getForeground() {
		if (foreground != null && !foreground.isDisposed()) {
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

	}

	private io.github.humbleui.skija.Font getSkijaFont(org.eclipse.swt.graphics.Font font) {
		final FontProperties props = FontProperties.getFontProperties(font);
		var cachedFont = fontCache.get(props);
		if (cachedFont != null && cachedFont.isClosed()) {
			fontCache.remove(props);
			cachedFont = null;
		}

		io.github.humbleui.skija.Font f = null;

		if (cachedFont == null) {
			f = createSkijaFont(props);
			fontCache.put(props, f);
			return f;
		}

		return fontCache.get(props);
	}

	private io.github.humbleui.skija.Font createSkijaFont(FontProperties props) {

		FontSlant slant = FontSlant.UPRIGHT;
		if (props.lfItalic != 0) {
			slant = FontSlant.ITALIC;
		}

		final FontStyle style = new FontStyle(props.lfWeight, 5, slant);
		final io.github.humbleui.skija.Font skijaFont = new io.github.humbleui.skija.Font(
				Typeface.makeFromName(props.name, style));

		if (props.lfWidth != 0) {
			final float stretch = (float) ((props.lfWidth / 10.0) + 0.5);
			skijaFont.setScaleX(stretch);
		}

		int fontSize = (props.lfHeight);
		if (SWT.getPlatform().equals("win32")) { //$NON-NLS-1$
			fontSize = skiaExtension.getScaler().getZoomedFontSize(fontSize);
		}
		if (SWT.getPlatform().equals("gtk") || true) { //$NON-NLS-1$
			// TODO move this to the local font size calculation in gtk
			// this should be done in the scaler.
			fontSize = (fontSize * Display.getDefault().getDPI().y) / 72;
		}
		skijaFont.setSize(fontSize);
		// --------------------------------------------------
		// This might be the same option like the windows gdi text antialias option.
		skijaFont.setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
		skijaFont.setSubpixel(true);
		skijaFont.setHinting(FontHinting.NORMAL);
		skijaFont.setAutoHintingForced(true);

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

	public DpiScaler getScaler() {
		return skiaExtension.getScaler();
	}

	private void resetResources() {

		if (this.skiaFont != null && !this.skiaFont.isClosed()) {
			this.skiaFont.close();
		}
		skiaFont = null;

		fontCache.values().forEach(f -> {
			if (!f.isClosed()) {
				f.close();
			}
		});
		fontCache.clear();

		imageCache.values().forEach(i -> {
			if (!i.isClosed()) {
				i.close();
			}
		});

		imageCache.clear();

	}

	public Font getFont() {

		if (swtFont != null && !swtFont.isDisposed()) {
			return swtFont;
		}
		swtFont = getDefaultFont();

		return swtFont;
	}

	public void resetBaseColors() {
		foreground = null;
		background = null;

	}

	public void cacheImage(Image swtImage, int zoom, io.github.humbleui.skija.Image skijaImage) {
		if (USE_IMAGE_CACHE ) {
			final var key = new ImageKey(swtImage, swtImage.getImageVersion(), zoom);
			final var old = imageCache.get(key);
			if (old != null && !old.isClosed()) {
				old.close();
			}
			this.imageCache.put(key, skijaImage);
		}
	}

	public io.github.humbleui.skija.Image getCachedImage(Image swtImage, int zoom) {
		return this.imageCache.get(new ImageKey(swtImage, swtImage.getImageVersion(), zoom));
	}

}