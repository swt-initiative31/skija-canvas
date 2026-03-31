/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *     SAP SE and others - initial API and implementation
 *******************************************************************************/

package org.eclipse.swt.internal.graphics;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.FontMetricsExtension;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.PatternProperties;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.internal.canvasext.DpiScaler;
import org.eclipse.swt.internal.canvasext.FontProperties;
import org.eclipse.swt.internal.canvasext.IExternalGC;
import org.eclipse.swt.internal.canvasext.Logger;
import org.eclipse.swt.internal.skia.ISkiaCanvasExtension;
import org.eclipse.swt.internal.skia.SkiaResources;
import org.eclipse.swt.widgets.Display;

import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.EncoderPNG;
import io.github.humbleui.skija.FilterMipmap;
import io.github.humbleui.skija.FilterMode;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.GradientStyle;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.MipmapMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.PathFillMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

public class SkiaGC implements IExternalGC {

	public final static boolean USE_TEXT_CACHE = true;

	public static boolean logImageNullError = true;
	private final Surface surface;
	private int lineWidth = 1;
	private int lineStyle;
	private int lineCap = SWT.CAP_FLAT;
	private int lineJoin = SWT.JOIN_MITER;
	private float[] lineDashes = null;
	private float dashOffset = 0;
	private float miterLimit = 10;
	private int fillRule = SWT.FILL_EVEN_ODD;
	private int antialias;
	private int alpha = 255;
	private Pattern foregroundPattern;
	private Pattern backgroundPattern;

	private final org.eclipse.swt.widgets.Canvas canvas;
	private final Display device;

	private static Map<ColorType, int[]> colorTypeMap = null;
	private Matrix33 currentTransform = Matrix33.IDENTITY;

	private SamplingMode interpolationMode = SamplingMode.DEFAULT;

	private boolean isClipSet;
	private Rectangle currentClipBounds;
	private Region currentClipRegion;

	private final ISkiaCanvasExtension skiaExtension;
	private final SkiaResources resources;
	private boolean xorModeActive;
	private final int style;
	private int textAntiAlias = SWT.ON;

	private final int initialSaveCount;

	public SkiaGC(org.eclipse.swt.widgets.Canvas canvas, ISkiaCanvasExtension exst, int style) {
		this.canvas = canvas;
		device = canvas.getDisplay();
		this.surface = exst.getSurface();
		this.skiaExtension = exst;
		this.resources = skiaExtension.getResources();
		this.style = style;
		// Save the initial canvas state so it can be fully restored on dispose()
		this.initialSaveCount = this.surface.getCanvas().save();
	}

	@Override
	public void dispose() {

		resources.resetBaseColors();
		// Restore all canvas state changes made during the lifetime of this GC,
		// including any clipping or transform layers pushed after construction
		surface.getCanvas().restoreToCount(initialSaveCount);

	}

	private void performDraw(Consumer<Paint> operations) {
		try (final Paint paint = new Paint()) {
			// Set up all paint properties first
			paint.setAlpha(alpha);
			paint.setColor(convertSWTColorToSkijaColor(getForeground(), this.alpha));
			paint.setAntiAlias(this.antialias != SWT.OFF);
			if (this.xorModeActive) {
				paint.setBlendMode(BlendMode.DIFFERENCE);
			} else {
				paint.setBlendMode(BlendMode.SRC_OVER);
			}

			final var scaledLineWidth = getScaler().autoScaleUp(lineWidth * 1F);

			paint.setMode(PaintMode.STROKE);
			paint.setStrokeWidth(scaledLineWidth);
			paint.setStrokeCap(getSkijaLineCap());
			switch (this.lineStyle) {
			case SWT.LINE_DOT -> paint.setPathEffect(
					PathEffect.makeDash(new float[] { 1f * scaledLineWidth, 1f * scaledLineWidth }, 0.0f));
			case SWT.LINE_DASH -> paint.setPathEffect(
					PathEffect.makeDash(new float[] { 3f * scaledLineWidth, 1f * scaledLineWidth }, 0.0f));
			case SWT.LINE_DASHDOT -> paint.setPathEffect(PathEffect.makeDash(new float[] { 3f * scaledLineWidth,
					1f * scaledLineWidth, 1f * scaledLineWidth, 1f * scaledLineWidth }, 0.0f));
			case SWT.LINE_DASHDOTDOT ->
			paint.setPathEffect(
					PathEffect
					.makeDash(
							new float[] { 3f * scaledLineWidth, 1f * scaledLineWidth, 1f * scaledLineWidth,
									1f * scaledLineWidth, 1f * scaledLineWidth, 1f * scaledLineWidth },
							0.0f));
			default -> paint.setPathEffect(null);
			}
			// Set shader last and use try-with-resources to prevent resource leaks
			if (this.foregroundPattern != null && !this.foregroundPattern.isDisposed()) {
				try (Shader shader = convertSWTPatternToSkijaShader(this.foregroundPattern)) {
					if (shader != null) {
						paint.setShader(shader);
					}
					operations.accept(paint);
				}
			} else {
				operations.accept(paint);
			}
		}
	}

	private PaintStrokeCap getSkijaLineCap() {
		if ((this.lineCap == SWT.CAP_SQUARE)) {
			return PaintStrokeCap.SQUARE;
		}
		if (this.lineCap == SWT.CAP_ROUND) {
			return PaintStrokeCap.ROUND;
		}
		return PaintStrokeCap.BUTT;
	}

	private void performDrawFilled(Consumer<Paint> operations) {
		try (final Paint paint = new Paint()) {
			paint.setMode(PaintMode.FILL);
			// Set background color by default
			if (this.alpha < 255) {
				paint.setColor(convertSWTColorToSkijaColor(getBackground(), this.alpha));
			} else {
				paint.setColor(convertSWTColorToSkijaColor(getBackground()));
			}
			// If a background pattern is set, override color with shader using
			// try-with-resources to prevent resource leaks
			if (backgroundPattern != null && !backgroundPattern.isDisposed()) {
				try (Shader shader = convertSWTPatternToSkijaShader(backgroundPattern)) {
					if (shader != null) {
						paint.setShader(shader);
					}
					operations.accept(paint);
				}
			} else {
				operations.accept(paint);
			}
		}
	}

	@Override
	public Point textExtent(String string) {
		return textExtent(string, SWT.NONE);
	}

	@Override
	public void setBackground(Color color) {
		this.resources.setBackground(color);
	}

	@Override
	public void setForeground(Color color) {
		this.resources.setForeground(color);
	}

	@Override
	public void fillRectangle(Rectangle rect) {
		fillRectangle(rect.x, rect.y, rect.width, rect.height);
	}

	private static void logImageNull(int[] positionData) {

		if (logImageNullError) {

			Logger.logException(new IllegalArgumentException(
					"Image argument is null. Position and size data: " + java.util.Arrays.toString(positionData))); //$NON-NLS-1$
			logImageNullError = false;
		}
	}

	@Override
	public void drawImage(Image image, int x, int y) {

		if (image == null) {
			logImageNull(new int[] { x, y });
			return;
		}

		final var imgBounds = image.getBounds();
		drawImage(image, 0, 0, imgBounds.width, imgBounds.height, x, y, imgBounds.width, imgBounds.height);
	}

	@Override
	public void drawImage(Image image, int destX, int destY, int destWidth, int destHeight) {

		if (image == null) {
			logImageNull(new int[] { destX, destY, destWidth, destHeight });
			return;
		}

		drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, destX, destY, destWidth, destHeight);

	}

	@Override
	public void drawImage(Image image, int srcX, int srcY, int srcWidth, int srcHeight, int destX, int destY,
			int destWidth, int destHeight) {

		if (image == null) {
			logImageNull(new int[] { srcX, srcY, srcWidth, srcHeight, destX, destY, destWidth, destHeight });
			return;
		}

		int factor = Math.round(Math.max((float) destWidth / srcWidth, (float) destHeight / srcHeight));

		if (factor == 0) {
			factor = 1;
		}
		final Canvas canvas = surface.getCanvas();

		final int fac = (int) Math.ceil(factor);

		performDraw(paint -> {
			paint.setAlpha(alpha);
			paint.setAntiAlias(true);
			// TODO create an image cache, instead of recreating the skija image every time
			canvas.drawImageRect(convertSWTImageToSkijaImage(image, getScaler().autoScaleUp(100 * fac)),
					createScaledRectangle(srcX * fac, srcY * fac, srcWidth * fac, srcHeight * fac),
					createScaledRectangle(destX, destY, destWidth, destHeight), this.interpolationMode, paint, false);

		});

	}

	private static ColorType getColorType(ImageData imageData) {
		final PaletteData palette = imageData.palette;

		if (imageData.getTransparencyType() == SWT.TRANSPARENCY_MASK) {
			return ColorType.UNKNOWN;
		}

		if (palette.isDirect) {
			final int redMask = palette.redMask;
			final int greenMask = palette.greenMask;
			final int blueMask = palette.blueMask;

			if (redMask == 0xFF0000 && greenMask == 0x00FF00 && blueMask == 0x0000FF) {
				return ColorType.UNKNOWN;
			}

			if (redMask == 0xFF000000 && greenMask == 0x00FF0000 && blueMask == 0x0000FF00) {
				return ColorType.RGBA_8888;
			}

			if (redMask == 0xF800 && greenMask == 0x07E0 && blueMask == 0x001F) {
				return ColorType.RGB_565;
			}

			if (redMask == 0xF000 && greenMask == 0x0F00 && blueMask == 0x00F0) {
				return ColorType.ARGB_4444;
			}

			if (redMask == 0x0000FF00 && greenMask == 0x00FF0000 && blueMask == 0xFF000000) {
				return ColorType.BGRA_8888;
			}

			if (redMask == 0x3FF00000 && greenMask == 0x000FFC00 && blueMask == 0x000003FF) {
				return ColorType.RGBA_1010102;
			}

			if (redMask == 0x000003FF && greenMask == 0x000FFC00 && blueMask == 0x3FF00000) {
				return ColorType.BGRA_1010102;
			}

			if (redMask == 0xFF && greenMask == 0xFF00 && blueMask == 0xFF0000) {
				return ColorType.UNKNOWN;
			}
		} else {
			if (imageData.depth == 8 && palette.colors != null && palette.colors.length <= 256) {
				return ColorType.ALPHA_8;
			}

			switch (imageData.depth) {
			case 16:
				// 16-bit indexed images are not directly mappable to common
				// ColorTypes
				return ColorType.ARGB_4444; // Assume for indexed color images
			case 24:
				// Assuming RGB with no alpha channel
				return ColorType.RGB_888X;
			case 32:
				// Assuming 32-bit color with alpha channel
				return ColorType.RGBA_8888;
			default:
				break;
			}
		}

		return ColorType.UNKNOWN;
	}

	private io.github.humbleui.skija.Image convertSWTImageToSkijaImage(Image swtImage, int zoom) {

		var img = resources.getCachedImage(swtImage, zoom);

		if (img != null && !img.isClosed()) {
			return img;
		}

		final ImageData imageData = swtImage.getImageData(zoom);
		img = convertSWTImageToSkijaImage(imageData);

		resources.cacheImage(swtImage, zoom, img);

		return img;

	}

	static io.github.humbleui.skija.Image convertSWTImageToSkijaImage(ImageData imageData) {
		final int width = imageData.width;
		final int height = imageData.height;
		ColorType colType = getColorType(imageData);

		// always prefer the alphaData. If these are set, the bytes data are empty!!
		// it seems ColorType.ALPHA_8 does not work with the direct data conversion.
		if (colType.equals(ColorType.UNKNOWN) || imageData.alphaData != null || colType.equals(ColorType.ALPHA_8)) {
			byte[] bytes = null;
			bytes = convertToRGBA(imageData);
			colType = ColorType.RGBA_8888;
			final ImageInfo imageInfo = new ImageInfo(width, height, colType, ColorAlphaType.UNPREMUL);
			return io.github.humbleui.skija.Image.makeRasterFromBytes(imageInfo, bytes, imageData.width * 4);
		}
		final ImageInfo imageInfo = new ImageInfo(width, height, colType, ColorAlphaType.UNPREMUL);

		return io.github.humbleui.skija.Image.makeRasterFromBytes(imageInfo, imageData.data, imageData.width * 4);
	}

	public static byte[] convertToRGBA(ImageData imageData) {

		return RGBAEncoder.encode(imageData);
	}

	static ImageData convertSkijaImageToImageData(io.github.humbleui.skija.Image image) {
		final Bitmap bm = Bitmap.makeFromImage(image);
		final var colType = bm.getColorType();
		final byte[] alphas = new byte[bm.getHeight() * bm.getWidth()];
		final var source = bm.readPixels();
		final byte[] convertedData = new byte[bm.getHeight() * bm.getWidth() * 3];

		final var colorOrder = getPixelOrder(colType);

		// no alphaType handling support. UNPREMUL and OPAQUE should always work.
		// ColorAlphaType alphaType = bm.getAlphaType();

		for (int y = 0; y < bm.getHeight(); y++) {
			for (int x = 0; x < bm.getWidth(); x++) {
				byte alpha = convertAlphaTo255Range(bm.getAlphaf(x, y));

				final int index = (x + y * bm.getWidth()) * 4;

				final byte red = source[index + colorOrder[0]];
				final byte green = source[index + colorOrder[1]];
				final byte blue = source[index + colorOrder[2]];
				alpha = source[index + colorOrder[3]];

				alphas[x + y * bm.getWidth()] = alpha;

				final int target = (x + y * bm.getWidth()) * 3;

				convertedData[target + 0] = (red);
				convertedData[target + 1] = (green);
				convertedData[target + 2] = (blue);
			}
		}

		final ImageData d = new ImageData(bm.getWidth(), bm.getHeight(), 24,
				new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
		d.data = convertedData;
		d.alphaData = alphas;
		d.bytesPerLine = d.width * 3;

		return d;
	}

	public static void writeFile(String str, io.github.humbleui.skija.Image image) {
		final byte[] imageBytes = EncoderPNG.encode(image).getBytes();

		final File f = new File(str);
		if (f.exists()) {
			f.delete();
		}

		try (FileOutputStream fis = new FileOutputStream(f)) {
			fis.write(imageBytes);
		} catch (final Exception e) {
			Logger.logException(e);
		}
	}

	public static byte convertAlphaTo255Range(float alphaF) {
		if (alphaF < 0.0f) {
			alphaF = 0.0f;
		}
		if (alphaF > 1.0f) {
			alphaF = 1.0f;
		}

		return (byte) Math.round(alphaF * 255);
	}

	public static int convertSWTColorToSkijaColor(Color swtColor) {
		// extract RGB-components
		final int red = swtColor.getRed();
		final int green = swtColor.getGreen();
		final int blue = swtColor.getBlue();
		final int alpha = swtColor.getAlpha();

		// create ARGB 32-Bit-color
		final int skijaColor = (alpha << 24) | (red << 16) | (green << 8) | blue;

		return skijaColor;
	}

	public static int convertSWTColorToSkijaColor(Color swtColor, int alphaOverride) {
		// extract RGB-components
		final int red = swtColor.getRed();
		final int green = swtColor.getGreen();
		final int blue = swtColor.getBlue();
		final int alpha = alphaOverride;

		// create ARGB 32-Bit-color
		final int skijaColor = (alpha << 24) | (red << 16) | (green << 8) | blue;

		return skijaColor;
	}

	public static int invertSWTColorToInt(Color swtColor) {

		// extract RGB-components
		final int red = swtColor.getRed();
		final int green = swtColor.getGreen();
		final int blue = swtColor.getBlue();
		final int alpha = swtColor.getAlpha();

		// create ARGB 32-Bit-color
		final int skijaColor = (alpha << 24) | ((0xFF - red) << 16) | ((0xFF - green) << 8) | (0xFF - blue);

		return skijaColor;

	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		final float scaledOffsetValue = getScaledOffsetValue();

		final var scaler = resources.getScaler();

		performDraw(paint -> surface.getCanvas().drawLine(scaler.autoScaleUp(x1) + scaledOffsetValue,
				scaler.autoScaleUp(y1) + scaledOffsetValue, scaler.autoScaleUp(x2) + scaledOffsetValue,
				scaler.autoScaleUp(y2) + scaledOffsetValue, paint));
	}

	@Override
	public Color getForeground() {
		return this.resources.getForeground();
	}

	@Override
	public void drawText(String string, int x, int y) {
		drawText(string, x, y, SWT.DRAW_DELIMITER | SWT.DRAW_TAB);
	}

	@Override
	public void drawText(String string, int x, int y, boolean isTransparent) {
		int flags = SWT.DRAW_DELIMITER | SWT.DRAW_TAB;
		if (isTransparent) {
			flags |= SWT.DRAW_TRANSPARENT;
		}
		drawText(string, x, y, flags);
	}

	@Override
	public void drawText(String text, int x, int y, int flags) {
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		drawTextBlob(text, flags, x, y);
	}

	private io.github.humbleui.skija.Font getSkiaFont() {
		return this.resources.getSkiaFont();
	}

	private void drawTextBlob(String inputText, int flags, int x, int y) {

		if (this.surface.getWidth() < x || this.surface.getHeight() < y) {
			return;
		}

		final String splits[] = this.resources.getTextSplits(inputText, flags);

		if (USE_TEXT_CACHE) {
			drawTextBlobWithCache(splits, flags, x, y);
			return;
		}

		drawTextBlobNoCache(splits, flags, x, y);
	}

	private void drawTextBlobNoCache(String[] splits, int flags, int x, int y) {

		final boolean transparent = isTransparent(flags);
		final boolean antiAlias = this.textAntiAlias == SWT.ON || this.textAntiAlias == SWT.DEFAULT;
		final int[] yPos = new int[1];
		yPos[0] = y;
		final var f = getSkiaFont();

		performDraw(fgp -> {
			fgp.setAntiAlias(antiAlias);
			fgp.setMode(PaintMode.FILL);
			f.setSubpixel(antiAlias);
			f.setEdging(antiAlias ? FontEdging.SUBPIXEL_ANTI_ALIAS : FontEdging.ALIAS);

			fgp.setStrokeWidth(1);
			fgp.setStrokeCap(PaintStrokeCap.BUTT);
			fgp.setPathEffect(null);
			for (final var text : splits) {

				final var metric = f.getMetrics();
				final var asc = metric.getAscent();
				final var des = metric.getDescent();
				final var leading = metric.getLeading();

				final var ascI = (int) Math.ceil(Math.abs(asc));
				final var desI = (int) Math.ceil(Math.abs(des));
				final var heightI = ascI + desI;
				final var r = resources.getScaler().scaleSize(x, yPos[0]);

				if (!transparent) {

					// draw rectangle background for the text.

					// Skia draws a text with a slightly right shift. So textExtent is insufficient
					// in the width to make sure, the background area is big enough
					// So make the area a little bit wider.

					// The background rectangle can be a little bit smaller, and in the worst
					// case a small part of the text is not covered.
					// Similar in windows. Actually for some fonts and sizes this solution is
					// already better than windows.

					// heuristic number. After 0.12 of the ascent, the text is usually sufficiently
					// in the rectangle area.
					final double endOfRectangle = Math.abs(asc) * 0.12;
					final var rect = f.measureText(text, fgp);

					final Point size = new Point((int) Math.ceil(rect.getWidth() + endOfRectangle),
							(int) Math.ceil(heightI + leading));

					performDrawFilled(paint -> {
						surface.getCanvas().drawRect(new Rect(r.x, r.y, r.x + size.x, r.y + size.y), paint);
					});

				}
				surface.getCanvas().drawString(text, r.x, r.y + ascI, f, fgp);
				yPos[0] += heightI + leading;
			}
		});

	}

	private void drawTextBlobWithCache(String[] splits, int flags, int xIn, int yIn) {

		final var scaler = resources.getScaler();

		final int x = scaler.autoScaleUp(xIn);
		final int y = scaler.autoScaleUp(yIn);

		final var f = getSkiaFont();
		final FontProperties props = FontProperties.getFontProperties(getFont());
		final boolean transparent = isTransparent(flags);
		final int backgroundColor = this.alpha < 255 ? convertSWTColorToSkijaColor(getBackground(), this.alpha)
				: convertSWTColorToSkijaColor(getBackground());
		final int foregroundColor = convertSWTColorToSkijaColor(getForeground(), this.alpha);
		final boolean antiAlias = this.textAntiAlias == SWT.ON || this.textAntiAlias == SWT.DEFAULT;

		final int[] yPos = new int[1];
		yPos[0] = y;
		for (final var text : splits) {

			final var cachedImage = this.resources.getTextImage(text, props, transparent, backgroundColor,
					foregroundColor, antiAlias);
			if (cachedImage != null && !cachedImage.isClosed()) {
				surface.getCanvas().drawImage(cachedImage, x, yPos[0]);
				yPos[0] += Math.ceil(cachedImage.getHeight());
				continue;
			}
			performDraw(fgp -> {

				fgp.setAntiAlias(antiAlias);
				fgp.setMode(PaintMode.FILL);
				f.setSubpixel(antiAlias);
				f.setEdging(antiAlias ? FontEdging.SUBPIXEL_ANTI_ALIAS : FontEdging.ALIAS);

				fgp.setStrokeWidth(1);
				fgp.setStrokeCap(PaintStrokeCap.BUTT);
				fgp.setPathEffect(null);
				fgp.setBlendMode(BlendMode.SRC_IN);

				final var rect = f.measureText(text, fgp);
				final var metric = f.getMetrics();
				final var asc = metric.getAscent();
				final var des = metric.getDescent();
				final var leading = metric.getLeading();

				final var ascI = (int) Math.ceil(Math.abs(asc));
				final var desI = (int) Math.ceil(Math.abs(des));
				final var heightI = ascI + desI;

				// skija draws a text with a slightly right shift. So textExtent is insufficient
				// in
				// the width. So make the background area a little bit wider.

				// Idea:
				// 1. the support surface should always be wide enough to contain the complete
				// text.
				// 2. the background rectangle can be a little bit smaller, and in the worst
				// case a small part of the text is not covered.
				// Similar in windows. Actually for some fonts and sizes this solution is
				// already better than windows.

				final double additionalArea = Math.abs(asc) * 2;

				final Point size = new Point((int) Math.ceil(rect.getWidth() + additionalArea),
						(int) Math.ceil(heightI + leading));

				final int MAX_SURFACE_WIDTH = 8192; // Documented practical limit
				int surfaceWidth = size.x;
				if (surfaceWidth > MAX_SURFACE_WIDTH) {
					Logger.logException(new IllegalStateException("Surface width restricted: calculated=" + surfaceWidth
							+ ", max=" + MAX_SURFACE_WIDTH + ", font=" + props.name + ", size=" + size
							+ ", backgroundColor=" + backgroundColor + ", foregroundColor=" + foregroundColor
							+ ", antiAlias=" + antiAlias + ", transparent=" + transparent + ", text='" + text + "'"));
					surfaceWidth = MAX_SURFACE_WIDTH;
				}
				if (surfaceWidth <= 0 || size.y <= 0) {
					final StringBuilder sb = new StringBuilder();
					sb.append("Calculated text image size is invalid. font=").append(props.name).append(", size=")
					.append(size).append(", text='").append(text).append("'");
					Logger.logException(new IllegalStateException(sb.toString()));
					return;
				}
				try (Surface supportSurface = this.skiaExtension.createSupportSurface(surfaceWidth, size.y)) {

					supportSurface.getCanvas().clear(0);
					if (!transparent) {

						// heuristic number. After 0.12 of the ascent, the text is usually sufficiently
						// in the rectangle area.
						final double endOfRectangle = Math.abs(asc) * 0.12;
						// always clear the support surface, then fill a specific rectangle area with
						// the background color, the rest stays transparent.
						try (Paint p = new Paint()) {
							p.setColor(convertSWTColorToSkijaColor(getBackground(), this.alpha));
							p.setMode(PaintMode.FILL);
							supportSurface.getCanvas().drawRect(
									new Rect(0, 0, (int) Math.ceil(rect.getWidth() + endOfRectangle), size.y), p);
						}

					} else {
						// very important at text on transparent background. Blend over the source,
						// otherwise the text won't be visible.
						fgp.setBlendMode(BlendMode.SRC_OVER);
					}

					supportSurface.getCanvas().drawString(text, 0, ascI, f, fgp);
					final var image = supportSurface.makeImageSnapshot();
					this.resources.cacheTextImage(text, props, transparent, backgroundColor, foregroundColor, antiAlias,
							image);
					surface.getCanvas().drawImage(image, x, yPos[0]);
					yPos[0] += Math.ceil(image.getHeight());

				}

			});
		}

	}

	private static boolean isTransparent(int flags) {
		return (SWT.DRAW_TRANSPARENT & flags) != 0;
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		performDraw(paint -> surface.getCanvas().drawArc(getScaler().autoScaleUp(x), getScaler().autoScaleUp(y),
				getScaler().autoScaleUp(x + width), getScaler().autoScaleUp(y + height), -startAngle, -arcAngle, false,
				paint));
	}

	@Override
	public void drawFocus(int x, int y, int width, int height) {
		performDraw(paint -> {
			final var scaledLineWidth = getScaler().autoScaleUp(lineWidth * 1F);

			paint.setPathEffect(PathEffect.makeDash(new float[] { 1.5f * scaledLineWidth, 1.5f * scaledLineWidth }, 0.0f));
			surface.getCanvas().drawRect(offsetRectangle(createScaledRectangle(x, y, width, height)), paint);
		});
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		performDraw(paint -> surface.getCanvas().drawOval(offsetRectangle(createScaledRectangle(x, y, width, height)),
				paint));
	}

	@Override
	public void drawPath(Path path) {
		try (io.github.humbleui.skija.Path skijaPath = convertSWTPathToSkijaPath(path)) {
			if (skijaPath == null) {
				return;
			}
			performDraw(paint -> {

				paint.setStrokeJoin(PaintStrokeJoin.MITER);
				paint.setStrokeMiter(100000);
				paint.setAntiAlias(false);
				paint.setStrokeCap(PaintStrokeCap.BUTT);

				surface.getCanvas().drawPath(skijaPath, paint);
			});

		}
	}

	@Override
	public void drawPoint(int x, int y) {
		performDraw(paint -> surface.getCanvas().drawRect(createScaledRectangle(x, y, 1, 1), paint));
	}

	@Override
	public void drawPolygon(int[] pointArray) {
		if (pointArray == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (isDisposed()) {
			SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
		}
		if (pointArray.length < 6 || pointArray.length % 2 != 0) {
			return;
		}

		// Handle SWT.MIRRORED style (adjust x-coordinates if needed)
		final int style = getStyle();
		final boolean mirrored = (style & SWT.MIRRORED) != 0;
		final boolean adjustX = mirrored && lineWidth != 0 && lineWidth % 2 == 0;
		if (adjustX) {
			for (int i = 0; i < pointArray.length; i += 2) {
				pointArray[i]--;
			}
		}

		// Create Skija path for the polygon
		try (io.github.humbleui.skija.PathBuilder path = new io.github.humbleui.skija.PathBuilder()) {
			// Move to first point
			path.moveTo(getScaler().autoScaleUp(pointArray[0]), getScaler().autoScaleUp(pointArray[1]));
			// Add lines to subsequent points
			for (int i = 2; i < pointArray.length; i += 2) {
				path.lineTo(getScaler().autoScaleUp(pointArray[i]), getScaler().autoScaleUp(pointArray[i + 1]));
			}
			path.closePath();
			// Draw the polygon outline
			performDraw(paint -> surface.getCanvas().drawPath(path.build(), paint));
		}
		// Restore x-coordinates if mirrored
		if (adjustX) {
			for (int i = 0; i < pointArray.length; i += 2) {
				pointArray[i]++;
			}
		}
	}

	private io.github.humbleui.skija.Path convertSWTPathToSkijaPath(Path swtPath) {
		if (swtPath == null || swtPath.isDisposed()) {
			return null;
		}
		final PathData data = swtPath.getPathData();

		try (final io.github.humbleui.skija.PathBuilder skijaPath = new io.github.humbleui.skija.PathBuilder()) {

			final float[] pts = data.points;
			final byte[] types = data.types;
			int pi = 0;
			for (final byte type : types) {
				switch (type) {
				case SWT.PATH_MOVE_TO:
					skijaPath.moveTo(getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]));
					break;
				case SWT.PATH_LINE_TO:
					skijaPath.lineTo(getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]));
					break;
				case SWT.PATH_CUBIC_TO:
					skijaPath.cubicTo(getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]),
							getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]),
							getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]));
					break;
				case SWT.PATH_QUAD_TO:
					skijaPath.quadTo(getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]),
							getScaler().autoScaleUp(pts[pi++]), getScaler().autoScaleUp(pts[pi++]));
					break;
				case SWT.PATH_CLOSE:
					skijaPath.closePath();
					break;
				default:
				}
			}
			final var p = skijaPath.build();
			return p;
		}
	}

	@Override
	public void drawPolyline(int[] pointArray) {
		performDraw(paint -> surface.getCanvas().drawPolygon(convertToFloat(pointArray), paint));
	}

	private float[] convertToFloat(int[] array) {
		final float[] arrayAsFloat = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			arrayAsFloat[i] = getScaler().autoScaleUp(array[i]);
		}
		return arrayAsFloat;
	}

	@Override
	public void drawRectangle(int x, int y, int width, int height) {

		performDraw(paint -> {
			surface.getCanvas().drawRect(offsetRectangle(createScaledRectangle(x, y, width, height)), paint);
		});

	}

	@Override
	public void drawRectangle(Rectangle rect) {
		drawRectangle(rect.x, rect.y, rect.width, rect.height);
	}

	@Override
	public void drawRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		performDraw(paint -> surface.getCanvas().drawRRect(
				offsetRectangle(createScaledRoundRectangle(x, y, width, height, arcWidth / 2.0f, arcHeight / 2.0f)),
				paint));
	}

	@Override
	public void drawString(String string, int x, int y) {
		drawString(string, x, y, false);
	}

	@Override
	public void drawString(String string, int x, int y, boolean isTransparent) {
		if (string == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		drawText(string, x, y, isTransparent);
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		performDrawFilled(paint -> surface.getCanvas().drawArc(getScaler().autoScaleUp(x), getScaler().autoScaleUp(y),
				getScaler().autoScaleUp(x + width), getScaler().autoScaleUp(y + height), -startAngle, -arcAngle, true,
				paint));
	}

	@Override
	public void fillGradientRectangle(int x, int y, int width, int height, boolean vertical) {

		boolean swapColors = false;
		if (width < 0) {
			x = Math.max(0, x + width);
			width = -width;
			if (!vertical) {
				swapColors = true;
			}
		}
		if (height < 0) {
			y = Math.max(0, y + height);
			height = -height;
			if (vertical) {
				swapColors = true;
			}
		}
		final int x2 = vertical ? x : x + width;
		final int y2 = vertical ? y + height : y;

		final Rect rect = createScaledRectangle(x, y, width, height);
		int fromColor = convertSWTColorToSkijaColor(getForeground());
		int toColor = convertSWTColorToSkijaColor(getBackground());
		if (fromColor == toColor) {
			performDrawFilled(paint -> surface.getCanvas().drawRect(rect, paint));
			return;
		}
		if (swapColors) {
			final int tempColor = convertSWTColorToSkijaColor(getForeground());
			fromColor = convertSWTColorToSkijaColor(getBackground());
			toColor = tempColor;
		}

		final var s = getScaler();
		performDrawGradientFilled(paint -> surface.getCanvas().drawRect(rect, paint), s.autoScaleUp(x),
				s.autoScaleUp(y), s.autoScaleUp(x2), s.autoScaleUp(y2), fromColor, toColor);
	}

	private void performDrawGradientFilled(Consumer<Paint> operations, int x, int y, int x2, int y2, int fromColor,
			int toColor) {
		performDraw(paint -> {

			try (Shader gradient = convertGradientRectangleToSkijaShader(x, y, x2 - x, y2 - y, false)) {
				paint.setShader(gradient);
				paint.setAntiAlias(true);
				paint.setMode(PaintMode.FILL);
				operations.accept(paint);
			}
		});
	}

	private Shader convertGradientRectangleToSkijaShader(int x, int y, int width, int height, boolean vertical) {

		final int col1 = convertSWTColorToSkijaColor(getForeground());
		final int col2 = convertSWTColorToSkijaColor(getBackground());

		final var gs = new GradientStyle(FilterTileMode.REPEAT, true, null);
		final Shader s = Shader.makeLinearGradient(x, y, x + width, y + height, new int[] { col1, col2 }, null, gs);

		return s;
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		performDrawFilled(paint -> surface.getCanvas().drawOval(createScaledRectangle(x, y, width, height), paint));
	}

	@Override
	public void fillPath(Path path) {
		try (io.github.humbleui.skija.Path skijaPath = convertSWTPathToSkijaPath(path)) {
			if (skijaPath == null) {
				return;
			}
			skijaPath.setFillMode(fillRule == SWT.FILL_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
			performDrawFilled(paint -> {
				paint.setAntiAlias(this.antialias == SWT.ON);
				surface.getCanvas().drawPath(skijaPath, paint);
			});
		}
	}

	@Override
	public void fillPolygon(int[] pointArray) {
		if (pointArray == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (isDisposed()) {
			SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
		}
		if (pointArray.length < 6 || pointArray.length % 2 != 0) {
			return;
		}

		// Create Skija path for the polygon
		try (io.github.humbleui.skija.PathBuilder path = new io.github.humbleui.skija.PathBuilder()) { // Move to first
			// point
			path.moveTo(getScaler().autoScaleUp(pointArray[0]), getScaler().autoScaleUp(pointArray[1]));
			// Add lines to subsequent points
			for (int i = 2; i < pointArray.length; i += 2) {
				path.lineTo(getScaler().autoScaleUp(pointArray[i]), getScaler().autoScaleUp(pointArray[i + 1]));
			}
			// Close the path to form a polygon
			path.closePath();
			path.setFillMode(fillRule == SWT.FILL_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
			// Fill the polygon
			performDrawFilled(paint -> surface.getCanvas().drawPath(path.build(), paint));
		}
	}

	@Override
	public void fillRectangle(int x, int y, int width, int height) {
		performDrawFilled(paint -> surface.getCanvas().drawRect(createScaledRectangle(x, y, width, height), paint));
	}

	@Override
	public void fillRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		performDrawFilled(paint -> surface.getCanvas()
				.drawRRect(createScaledRoundRectangle(x, y, width, height, arcWidth / 2.0f, arcHeight / 2.0f), paint));
	}

	@Override
	public Point textExtent(String text, int flags) {
		return getScaler().scaleDown(resources.textExtent(text, flags));
	}

	private static String replaceMnemonics(String text) {
		return SkiaResources.replaceMnemonics(text);
	}

	@Override
	public void setFont(org.eclipse.swt.graphics.Font font) {
		this.resources.setFont(font);
	}

	@Override
	public void setClipping(int x, int y, int width, int height) {
		setClipping(new Rectangle(x, y, width, height));
	}

	@Override
	public void setTransform(Transform transform) {

		final var sc = getScaler();

		if (transform == null) {
			currentTransform = Matrix33.IDENTITY;
			surface.getCanvas().setMatrix(currentTransform);
		} else {
			if (transform.isDisposed()) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			final float[] elements = new float[6];
			transform.getElements(elements);
			// SWT Transform: [m11, m12, m21, m22, dx, dy]
			// Skija Matrix33: [scaleX, skewX, transX, skewY, scaleY, transY, persp0,
			// persp1, persp2]
			// Correct mapping: SWT [0,1,2,3,4,5] -> Skija [0,2,4,1,3,5,0,0,1]
			final float[] skijaMat = new float[] { elements[0], // m11 -> scaleX
					elements[2], // m21 -> skewX
					sc.autoScaleUp(elements[4]), // dx -> transX
					elements[1], // m12 -> skewY
					elements[3], // m22 -> scaleY
					sc.autoScaleUp(elements[5]), // dy -> transY
					0, 0, 1 // perspective elements
			};
			currentTransform = new Matrix33(skijaMat);
			surface.getCanvas().setMatrix(currentTransform);
		}

		// Save the canvas state after applying the new transform, so subsequent
		// operations (e.g. clipping) can be stacked and later restored independently
		surface.getCanvas().save();

	}

	@Override
	public void setAlpha(int alpha) {
		alpha = alpha & 0xFF;
		if (alpha < 0 || alpha > 255) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.alpha = alpha;
	}

	@Override
	public int getAlpha() {
		return this.alpha;
	}

	@Override
	public void setLineWidth(int i) {
		this.lineWidth = i;
	}

	@Override
	public int getAntialias() {
		return antialias;
	}

	@Override
	public void setAntialias(int antialias) {
		if (antialias != SWT.DEFAULT && antialias != SWT.ON && antialias != SWT.OFF) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.antialias = antialias;
	}

	@Override
	public void setAdvanced(boolean enable) {
		// Nothing to do...
	}

	private Rect offsetRectangle(Rect rect) {
		final float scaledOffsetValue = getScaledOffsetValue();
		final float widthHightAutoScaleOffset = getScaler().autoScaleUp(1) - 1.0f;
		if (scaledOffsetValue > 0f) {
			return new Rect(rect.getLeft() + 1, rect.getTop() + 1, rect.getRight() + 1 + widthHightAutoScaleOffset,
					rect.getBottom() + 1 + widthHightAutoScaleOffset);
		}
		return rect;
	}

	private RRect offsetRectangle(RRect rect) {
		final float scaledOffsetValue = getScaledOffsetValue();
		final float widthHightAutoScaleOffset = getScaler().autoScaleUp(1) - 1.0f;
		if (scaledOffsetValue != 0f) {
			return new RRect(rect.getLeft() + scaledOffsetValue, rect.getTop() + scaledOffsetValue,
					rect.getRight() + scaledOffsetValue + widthHightAutoScaleOffset,
					rect.getBottom() + scaledOffsetValue + widthHightAutoScaleOffset, rect._radii);
		}
		return rect;
	}

	private Rect createScaledRectangle(Rectangle r) {
		return createScaledRectangle(r.x, r.y, r.width, r.height);
	}

	private Rect createScaledRectangle(int x, int y, int width, int height) {
		final var r = getScaler().scaleUpRectangle(new Rectangle(x, y, width, height));
		return new Rect(r.x, r.y, r.width + r.x, r.height + r.y);
	}

	/**
	 * Scales a rectangle from logical to physical pixels using only the global DPI
	 * scale factor (no per-canvas zoom). Suitable for stateless callers such as
	 * {@link org.eclipse.swt.internal.skia.SkiaCaretHandler}.
	 *
	 * @param c
	 */
	public static Rect createScaledRectangleStatic(org.eclipse.swt.widgets.Canvas c, int x, int y, int width,
			int height) {
		final DpiScaler scaler = new DpiScaler(c);
		return new Rect(scaler.autoScaleUp(x), scaler.autoScaleUp(y), scaler.autoScaleUp(x + width),
				scaler.autoScaleUp(y + height));
	}

	/**
	 * Applies a sub-pixel offset to a rectangle based on a given stroke width.
	 * Shared with stateless callers such as
	 * {@link org.eclipse.swt.internal.skia.SkiaCaretHandler}.
	 *
	 * @param c
	 */
	public static Rect offsetRectangleStatic(org.eclipse.swt.widgets.Canvas c, Rect rect, float strokeWidth) {
		final DpiScaler scaler = new DpiScaler(c);

		final boolean isDefaultLineWidth = strokeWidth == 0;
		final float scaledOffsetValue;
		if (isDefaultLineWidth) {
			scaledOffsetValue = 0.5f;
		} else {
			final float effectiveLineWidth = scaler.autoScaleUp(strokeWidth);
			scaledOffsetValue = (effectiveLineWidth % 2 == 1) ? scaler.autoScaleUp(0.5f) : 0f;
		}
		if (scaledOffsetValue != 0f) {
			final float whaOffset = scaler.autoScaleUp(1) - 1.0f;
			return new Rect(rect.getLeft() + scaledOffsetValue, rect.getTop() + scaledOffsetValue,
					rect.getRight() + scaledOffsetValue + whaOffset, rect.getBottom() + scaledOffsetValue + whaOffset);
		}
		return rect;
	}

	private DpiScaler getScaler() {
		return resources.getScaler();
	}

	private float getScaledOffsetValue() {
		final boolean isDefaultLineWidth = lineWidth == 0;
		if (isDefaultLineWidth) {
			return 0.5f;
		}
		final int effectiveLineWidth = getScaler().autoScaleUp(lineWidth);
		if (effectiveLineWidth % 2 == 1) {
			return getScaler().autoScaleUp(0.5f);
		}
		return 0f;
	}

	private RRect createScaledRoundRectangle(int x, int y, int width, int height, float arcWidth, float arcHeight) {
		return new RRect(getScaler().autoScaleUp(x), getScaler().autoScaleUp(y), getScaler().autoScaleUp(x + width),
				getScaler().autoScaleUp(y + height),
				new float[] { getScaler().autoScaleUp(arcWidth), getScaler().autoScaleUp(arcHeight) });
	}

	@Override
	public void setLineStyle(int lineStyle) {
		if (lineStyle != SWT.LINE_SOLID && lineStyle != SWT.LINE_DASH && lineStyle != SWT.LINE_DOT
				&& lineStyle != SWT.LINE_DASHDOT && lineStyle != SWT.LINE_DASHDOTDOT && lineStyle != SWT.LINE_CUSTOM) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.lineStyle = lineStyle;
	}

	@Override
	public int getLineStyle() {
		return lineStyle;
	}

	@Override
	public int getLineWidth() {
		return lineWidth;
	}

	@Override
	public LineAttributes getLineAttributes() {
		final LineAttributes attributes = getLineAttributesInPixels();
		attributes.width = getScaler().autoScaleDown(attributes.width);
		if (attributes.dash != null) {
			attributes.dash = getScaler().autoScaleDown(attributes.dash);
		}
		return attributes;
	}

	LineAttributes getLineAttributesInPixels() {
		return new LineAttributes(lineWidth, lineCap, lineJoin, lineStyle, lineDashes, dashOffset, miterLimit);
	}

	@Override
	public Rectangle getClipping() {
		return currentClipBounds;
	}

	@Override
	public Point stringExtent(String string) {
		return textExtent(string);
	}

	@Override
	public int getLineCap() {
		return lineCap;
	}

	@Override
	public void copyArea(Image image, int x, int y) {

		if (image == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}

		final io.github.humbleui.skija.Image skijaImage = convertSWTImageToSkijaImage(image,
				getScaler().getNativeZoom());
		try (final io.github.humbleui.skija.Image copiedArea = surface.makeImageSnapshot(
				createScaledRectangle(x, y, skijaImage.getWidth(), skijaImage.getHeight()).toIRect())) {

			if (copiedArea == null) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}

			try (final Surface imageSurface = surface.makeSurface(skijaImage.getWidth(), skijaImage.getHeight())) {
				imageSurface.getCanvas().drawImage(copiedArea, 0, 0);
				try (final io.github.humbleui.skija.Image snapshot = imageSurface.makeImageSnapshot()) {
					final ImageData imgData = convertSkijaImageToImageData(snapshot);
					Image i = null;
					GC gc = null;
					try {
						i = new Image(device, imgData);
						gc = new GC(image);
						gc.drawImage(i, 0, 0);
					} finally {
						if (gc != null) {
							gc.dispose();
						}
						if (i != null) {
							i.dispose();
						}
					}
				}
			}
		}
	}

	@Override
	public void copyArea(int srcX, int srcY, int width, int height, int destX, int destY) {
		try (io.github.humbleui.skija.Image copiedArea = surface
				.makeImageSnapshot(createScaledRectangle(srcX, srcY, width, height).toIRect())) {
			surface.getCanvas().drawImage(copiedArea, getScaler().autoScaleUp(destX), getScaler().autoScaleUp(destY));
		}
	}

	@Override
	public void copyArea(int srcX, int srcY, int width, int height, int destX, int destY, boolean paint) {

		copyArea(srcX, srcY, width, height, destX, destY);
		if (paint) {
			// Save the canvas state before clipping
			surface.getCanvas().save();
			// Clip to the destination rectangle so only this area is affected
			surface.getCanvas().clipRect(createScaledRectangle(srcX, srcY, width, height));
			// Clear the clipped area with transparent background (simulates OS.SW_ERASE)
			surface.getCanvas().clear(convertSWTColorToSkijaColor(getBackground()));
			// Restore the canvas state
			surface.getCanvas().restore();
			// Trigger redraw for the source area if using SWT Canvas
			canvas.redraw(srcX, srcY, width, height, false);
		}
	}

	@Override
	public boolean isClipped() {
		return isClipSet;
	}

	@Override
	public int getFillRule() {
		return fillRule;
	}

	@Override
	public void getClipping(Region region) {
		if (region == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (region.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}

		if (this.currentClipBounds != null) {

			region.intersect(this.currentClipBounds);
			region.add(this.currentClipBounds);
			return;
		}

		if (this.currentClipRegion != null) {
			region.intersect(this.currentClipRegion);
			region.add(this.currentClipRegion);
			return;
		}
	}

	@Override
	public int getAdvanceWidth(char ch) {

		final var f = getSkiaFont();
		final AtomicInteger result = new AtomicInteger();
		result.set(-1);

		performDraw(paint -> {
			paint.setAntiAlias(false);
			paint.setMode(PaintMode.FILL);

			final var textWidth = f.measureTextWidth(String.valueOf(ch), paint);
			result.set((int) Math.ceil(textWidth));

		});

		return result.get();

	}

	@Override
	public boolean getAdvanced() {
		return true;
	}

	@Override
	public Pattern getBackgroundPattern() {
		return backgroundPattern;
	}

	@Override
	public int getCharWidth(char ch) {
		return getAdvanceWidth(ch);
	}

	@Override
	public Pattern getForegroundPattern() {
		return foregroundPattern;
	}

	@Override
	public GCData getGCData() {
		return null;
	}

	@Override
	public int getInterpolation() {
		if (interpolationMode == SamplingMode.DEFAULT) {
			return SWT.NONE;
		}
		if (interpolationMode == SamplingMode.LINEAR) {
			return SWT.HIGH;
		}
		if (interpolationMode instanceof final FilterMipmap fm) {
			if (fm.getFilterMode() == FilterMode.LINEAR && fm.getMipmapMode() == MipmapMode.LINEAR) {
				return SWT.LOW;
			}
		}
		return SWT.DEFAULT;
	}

	@Override
	public int[] getLineDash() {
		if (lineDashes == null) {
			return null;
		}
		final int[] lineDashesInt = new int[lineDashes.length];
		for (int i = 0; i < lineDashesInt.length; i++) {
			lineDashesInt[i] = getScaler().autoScaleDownToInt(lineDashes[i]);
		}
		return lineDashesInt;
	}

	@Override
	public int getLineJoin() {
		return lineJoin;
	}

	@Override
	public int getStyle() {
		return style;
	}

	@Override
	public int getTextAntialias() {
		return textAntiAlias;
	}

	@Override
	public void getTransform(Transform transform) {
		if (transform == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final float[] m = currentTransform.getMat();
		// Skija Matrix33: [scaleX, skewX, transX, skewY, scaleY, transY, persp0,
		// persp1, persp2]
		// SWT Transform: [m11, m12, m21, m22, dx, dy]
		// Correct inverse mapping: Skija [0,1,2,3,4,5] -> SWT [0,3,1,4,2,5]
		transform.setElements(m[0], m[3], m[1], m[4], m[2], m[5]);
	}

	@Override
	public boolean getXORMode() {
		return xorModeActive;
	}

	@Override
	public void setBackgroundPattern(Pattern pattern) {
		if (pattern != null && pattern.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.backgroundPattern = pattern;
	}

	@Override
	public void setClipping(Path path) {
		final Canvas canvas = surface.getCanvas();
		if (isClipSet) {
			canvas.restore(); // pop the previously saved clip layer
			isClipSet = false;
		}
		if (path == null) {
			return;
		}
		if (path.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		try (final io.github.humbleui.skija.Path skijaPath = convertSWTPathToSkijaPath(path)) {
			if (skijaPath == null) {
				return;
			}
			skijaPath.setFillMode(fillRule == SWT.FILL_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
			// Push a new canvas layer so the clip can be undone later with restore()
			canvas.save();
			isClipSet = true;
			canvas.clipPath(skijaPath, ClipMode.INTERSECT, true);
		}
	}

	@Override
	public void setClipping(Rectangle rect) {

		// Skija uses a canvas state stack; each save() pushes a new layer,
		// and restore() pops it, removing the clipping region set in that layer.
		// Since only one clip is tracked at a time, no explicit restore() is needed
		// here
		// because the clip will be cleared when the next setClipping call is made or on
		// dispose.
		final Canvas canvas = surface.getCanvas();
		if (isClipSet) {
			canvas.restore(); // pop the previously saved clip layer
			isClipSet = false;
		}
		if (rect == null) {
			currentClipBounds = null;
			return;
		}
		currentClipBounds = new Rectangle(rect.x, rect.y, rect.width, rect.height);
		// Push a new canvas layer so the clip can be undone later with restore()
		canvas.save();
		canvas.clipRect(createScaledRectangle(rect));
		isClipSet = true;

	}

	@Override
	public void setClipping(Region region) {

		// Skija uses a canvas state stack; each save() pushes a new layer,
		// and restore() pops it, removing the clipping region set in that layer.
		final Canvas canvas = surface.getCanvas();
		if (isClipSet) {
			canvas.restore(); // pop the previously saved clip layer
			isClipSet = false;
		}
		currentClipBounds = null;
		currentClipRegion = region;

		if (region == null) {
			return;
		}

		final SkiaRegionCalculator calc = new SkiaRegionCalculator(region, skiaExtension);
		// Push a new canvas layer so the clip can be undone later with restore()
		canvas.save();
		try (calc) {
			canvas.clipRegion(calc.getSkiaRegion());
		}
		isClipSet = true;

	}

	@Override
	public void setFillRule(int rule) {
		if (rule != SWT.FILL_EVEN_ODD && rule != SWT.FILL_WINDING) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.fillRule = rule;
	}

	@Override
	public void setForegroundPattern(Pattern pattern) {
		if (pattern != null && pattern.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.foregroundPattern = pattern;
	}

	@Override
	public void setInterpolation(int interpolation) {

		// GDI | Skia | description
		// NearestNeighbor SKFilterMode.Nearest hart pixels
		// Low / Bilinear SKFilterMode.Linear simple linear
		// High / Bicubic SKCubicResampler.Mitchell high quality
		// HighQualityBicubic SKCubicResampler.CatmullRom maximum sharp, cubic
		// interpolation

		switch (interpolation) {
		case SWT.NONE -> this.interpolationMode = SamplingMode.DEFAULT; // Nearest neighbor
		case SWT.LOW -> this.interpolationMode = SamplingMode.LINEAR;
		case SWT.DEFAULT -> this.interpolationMode = SamplingMode.MITCHELL;
		case SWT.HIGH -> this.interpolationMode = SamplingMode.CATMULL_ROM;
		default -> SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
	}

	@Override
	public void setLineAttributes(LineAttributes attributes) {
		if (attributes == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final float scaledWidth = getScaler().autoScaleUp(attributes.width);
		setLineAttributesInPixels(attributes, scaledWidth);
	}

	private void setLineAttributesInPixels(LineAttributes attributes, float scaledWidth) {
		if (isDisposed()) {
			SWT.error(SWT.ERROR_GRAPHIC_DISPOSED);
		}
		boolean changed = false;
		if (scaledWidth != this.lineWidth) {
			this.lineWidth = (int) scaledWidth;
			changed = true;
		}
		// Handle line style with validation
		int lineStyle = attributes.style;
		if (lineStyle != this.lineStyle) {
			switch (lineStyle) {
			case SWT.LINE_SOLID:
			case SWT.LINE_DASH:
			case SWT.LINE_DOT:
			case SWT.LINE_DASHDOT:
			case SWT.LINE_DASHDOTDOT:
				break;
			case SWT.LINE_CUSTOM:
				if (attributes.dash == null) {
					lineStyle = SWT.LINE_SOLID;
				}
				break;
			default:
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			this.lineStyle = lineStyle;
			changed = true;
		}
		// Handle line cap with validation
		final int cap = attributes.cap;
		if (cap != this.lineCap) {
			switch (cap) {
			case SWT.CAP_FLAT:
			case SWT.CAP_ROUND:
			case SWT.CAP_SQUARE:
				break;
			default:
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			this.lineCap = cap;
			changed = true;
		}
		// Handle line join with validation
		final int join = attributes.join;
		if (join != this.lineJoin) {
			switch (join) {
			case SWT.JOIN_MITER:
			case SWT.JOIN_ROUND:
			case SWT.JOIN_BEVEL:
				break;
			default:
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			this.lineJoin = join;
			changed = true;
		}
		// Handle dash pattern with validation and DPI scaling
		final float[] dashes = attributes.dash;
		final float[] currentDashes = this.lineDashes;

		if (dashes != null && dashes.length > 0) {
			boolean dashesChanged = currentDashes == null || currentDashes.length != dashes.length;
			final float[] newDashes = new float[dashes.length];

			for (int i = 0; i < dashes.length; i++) {
				final float dash = dashes[i];
				if (dash <= 0) {
					SWT.error(SWT.ERROR_INVALID_ARGUMENT);
				}

				// Scale dash values for DPI
				newDashes[i] = getScaler().autoScaleUp(dash);

				if (!dashesChanged && currentDashes != null && currentDashes[i] != newDashes[i]) {
					dashesChanged = true;
				}
			}
			if (dashesChanged) {
				this.lineDashes = newDashes;
				changed = true;
			}
		} else {
			// Clear dash pattern
			if (currentDashes != null && currentDashes.length > 0) {
				this.lineDashes = null;
				changed = true;
			}
		}
		// Handle dash offset - store for use in createPathEffectForLineStyle()
		final float dashOffset = attributes.dashOffset;
		if (this.dashOffset != dashOffset) {
			this.dashOffset = dashOffset;
			changed = true;
		}
		// Handle miter limit - store for use in performDraw()
		final float miterLimit = attributes.miterLimit;
		if (this.miterLimit != miterLimit) {
			this.miterLimit = miterLimit;
			changed = true;
		}
		if (!changed) {
			return;
		}
	}

	@Override
	public void setLineCap(int cap) {
		if (cap != SWT.CAP_FLAT && cap != SWT.CAP_ROUND && cap != SWT.CAP_SQUARE) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.lineCap = cap;
	}

	@Override
	public void setLineDash(int[] dashes) {
		if (dashes != null && dashes.length > 0) {
			boolean changed = this.lineStyle != SWT.LINE_CUSTOM || lineDashes == null
					|| lineDashes.length != dashes.length;
			final float[] newDashes = new float[dashes.length];
			for (int i = 0; i < dashes.length; i++) {
				if (dashes[i] <= 0) {
					SWT.error(SWT.ERROR_INVALID_ARGUMENT);
				}
				newDashes[i] = getScaler().autoScaleUp(dashes[i]);
				if (!changed && lineDashes != null && lineDashes[i] != newDashes[i]) {
					changed = true;
				}
			}
			if (!changed) {
				return;
			}
			this.lineDashes = newDashes;
			this.lineStyle = SWT.LINE_CUSTOM;
		} else {
			if (this.lineStyle == SWT.LINE_SOLID && (lineDashes == null || lineDashes.length == 0)) {
				return;
			}
			this.lineDashes = null;
			this.lineStyle = SWT.LINE_SOLID;
		}
	}

	@Override
	public void setLineJoin(int join) {
		if (join != SWT.JOIN_MITER && join != SWT.JOIN_ROUND && join != SWT.JOIN_BEVEL) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.lineJoin = join;
	}

	@Override
	public void setXORMode(boolean xor) {

		this.xorModeActive = xor;

	}

	@Override
	public void setTextAntialias(int antialias) {
		this.textAntiAlias = antialias;
	}

	static PaletteData getPaletteData(ColorType colorType) {
		// Note: RGB values here should be representative of a palette.

		// TODO test all mappings here
		return switch (colorType) {
		case ALPHA_8 -> new PaletteData(new RGB[] { new RGB(255, 255, 255), new RGB(0, 0, 0) });
		case RGB_565 -> new PaletteData(0xF800, 0x07E0, 0x001F); // Mask for RGB565
		case ARGB_4444 -> new PaletteData(0x0F00, 0x00F0, 0x000F); // Mask for ARGB4444
		case RGBA_8888 -> {
			final var p = new PaletteData(0xFF000000, 0x00FF0000, 0x0000FF00); // Standard RGBA masks
			yield p;
		}
		case BGRA_8888 -> new PaletteData(0x0000FF00, 0x00FF0000, 0xFF000000);
		case RGBA_F16 -> new PaletteData(new RGB[] { new RGB(255, 0, 0), // Example red
				new RGB(0, 255, 0), // Example green
				new RGB(0, 0, 255) }); // Example red // Example green // Example blue
		case RGBA_F32 -> new PaletteData(new RGB[] { new RGB(255, 165, 0), // Example orange
				new RGB(0, 255, 255), // Example cyan
				new RGB(128, 0, 128) }); // Example orange // Example cyan // Example purple
		default -> throw new IllegalArgumentException("Unknown Skija ColorType: " + colorType);
		};
	}

	static int getImageDepth(ColorType colorType) {
		// TODO test all mappings
		return switch (colorType) {
		case ALPHA_8 -> 8;
		case RGB_565 -> 16;
		case ARGB_4444 -> 16;
		case RGBA_8888 -> 32;
		case BGRA_8888 -> 32;
		case RGBA_F16 -> /*
		 * Typically could represent more colors, but SWT doesn't support floating-point
		 */ /* depths. */ 64; // This is theoretical; SWT will usually not handle more than 32
		case RGBA_F32 -> /* Same as RGBA_F16 with regards to SWT support */ 128; // Theoretical; actual handling
		// requires custom treatment
		default -> throw new IllegalArgumentException("Unknown Skija ColorType: " + colorType);
		};
	}

	static ColorAlphaType determineAlphaType(ImageData imageData) {
		// TODO test all mappings
		if (imageData.alphaData == null && imageData.alpha == -1) {
			// no alpha
			return ColorAlphaType.OPAQUE;
		}

		if (imageData.alphaData != null || imageData.alpha < 255) {
			// alpha data available
			return ColorAlphaType.UNPREMUL;
		}

		// usually without additional information
		return ColorAlphaType.UNPREMUL;
	}

	private static Map<ColorType, int[]> createColorTypeMap() {
		if (colorTypeMap != null) {
			return colorTypeMap;
		}

		colorTypeMap = new HashMap<>();

		// define pixel order for skija ColorType
		// what to do if the number of ints is not 4?
		colorTypeMap.put(ColorType.ALPHA_8, new int[] { 0 }); // only Alpha
		colorTypeMap.put(ColorType.RGB_565, new int[] { 0, 1, 2 }); // RGB
		colorTypeMap.put(ColorType.ARGB_4444, new int[] { 1, 2, 3, 0 }); // ARGB
		colorTypeMap.put(ColorType.RGBA_8888, new int[] { 0, 1, 2, 3 }); // RGBA
		colorTypeMap.put(ColorType.RGB_888X, new int[] { 0, 1, 2, 3 }); // RGB, ignore X
		colorTypeMap.put(ColorType.BGRA_8888, new int[] { 2, 1, 0, 3 }); // BGRA
		colorTypeMap.put(ColorType.RGBA_1010102, new int[] { 0, 1, 2, 3 }); // RGBA
		colorTypeMap.put(ColorType.BGRA_1010102, new int[] { 2, 1, 0, 3 }); // BGRA
		colorTypeMap.put(ColorType.RGB_101010X, new int[] { 0, 1, 2, 3 }); // RGB, ignore X
		colorTypeMap.put(ColorType.BGR_101010X, new int[] { 2, 1, 0, 3 }); // BGR, ignore X
		colorTypeMap.put(ColorType.RGBA_F16NORM, new int[] { 0, 1, 2, 3 }); // RGBA
		colorTypeMap.put(ColorType.RGBA_F16, new int[] { 0, 1, 2, 3 }); // RGBA
		colorTypeMap.put(ColorType.RGBA_F32, new int[] { 0, 1, 2, 3 }); // RGBA
		colorTypeMap.put(ColorType.R8G8_UNORM, new int[] { 0, 1 }); // RG
		colorTypeMap.put(ColorType.A16_FLOAT, new int[] { 0 }); // Alpha
		colorTypeMap.put(ColorType.R16G16_FLOAT, new int[] { 0, 1 }); // RG
		colorTypeMap.put(ColorType.A16_UNORM, new int[] { 0 }); // Alpha
		colorTypeMap.put(ColorType.R16G16_UNORM, new int[] { 0, 1 }); // RG
		colorTypeMap.put(ColorType.R16G16B16A16_UNORM, new int[] { 0, 1, 2, 3 }); // RGBA

		return colorTypeMap;
	}

	public static int[] getPixelOrder(ColorType colorType) {
		final Map<ColorType, int[]> colorTypeMap = createColorTypeMap();
		return colorTypeMap.get(colorType);
	}

	@Override
	public Color getBackground() {
		return resources.getBackground();
	}

	@Override
	public Device getDevice() {
		return device;
	}

	@Override
	public org.eclipse.swt.graphics.Font getFont() {
		return this.resources.getFont();
	}

	@Override
	public boolean isDisposed() {
		return surface.isClosed();
	}

	/**
	 * Converts an SWT Pattern to a Skija Shader.
	 *
	 * @param pattern the SWT Pattern to convert
	 * @return the Skija Shader or null if conversion fails
	 */
	private Shader convertSWTPatternToSkijaShader(Pattern pattern) {

		final var props = PatternProperties.get(pattern);

		final var sc = getScaler();

		if (props.getImage() == null) {

			final int col1 = convertSWTColorToSkijaColor(props.getColor1(), props.getAlpha1());
			final int col2 = convertSWTColorToSkijaColor(props.getColor2(), props.getAlpha2());

			final var gs = new GradientStyle(FilterTileMode.REPEAT, true, null);
			final Shader s = Shader.makeLinearGradient(sc.autoScaleUp(props.getBaseX1()),
					sc.autoScaleUp(props.getBaseY1()), sc.autoScaleUp(props.getBaseX2()),
					sc.autoScaleUp(props.getBaseY2()), new int[] { col1, col2 }, null, gs);

			return s;
		}

		final var image = convertSWTImageToSkijaImage(props.getImage(), sc.getNativeZoom());
		return image.makeShader(FilterTileMode.REPEAT);

	}

	@Override
	public FontMetrics getFontMetrics() {

		final var font = getSkiaFont();
		final var m = font.getMetrics();

		final var fe = new FontMetricsExtension(new SkiaFontMetrics(m));
		return fe;
	}

	@Override
	public Drawable getDrawable() {
		return canvas;
	}

	@Override
	public void textLayoutDraw(TextLayout textLayout, GC gc, int xInPoints, int yInPoints, int selectionStart,
			int selectionEnd, Color selectionForeground, Color selectionBackground, int flags) {

		final var rectangle = textLayout.getBounds();

		Image i = null;
		GC nativeGC = null;
		try {
			i = new Image(device, rectangle.width, rectangle.height);
			nativeGC = new GC(i);

			textLayout.draw(nativeGC, 0, 0, selectionStart, selectionEnd, selectionForeground, selectionBackground,
					flags);

			drawImage(i, rectangle.x, rectangle.y, rectangle.width, rectangle.height, xInPoints, yInPoints,
					rectangle.width, rectangle.height);
		} finally {
			if (nativeGC != null) {
				nativeGC.dispose();
			}
			if (i != null) {
				i.dispose();
			}
		}

	}

}
