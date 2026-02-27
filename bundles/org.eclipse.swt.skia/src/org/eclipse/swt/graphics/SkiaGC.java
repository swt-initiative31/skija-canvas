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

package org.eclipse.swt.graphics;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ISkiaCanvas;
import org.eclipse.swt.widgets.SkiaResources;

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
import io.github.humbleui.skija.PathEffect;
import io.github.humbleui.skija.PathFillMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

public class SkiaGC implements IExternalGC {

	static final float[] LINE_DOT_PATTERN = new float[] { 3, 3 };
	static final float[] LINE_DASH_PATTERN = new float[] { 18, 6 };
	static final float[] LINE_DASHDOT_PATTERN = new float[] { 9, 6, 3, 6 };
	static final float[] LINE_DASHDOTDOT_PATTERN = new float[] { 9, 3, 3, 3, 3, 3 };

	private final int TEXT_MARGIN_TOP = 2;
	private final int TEXT_MARGIN_SIDE = 1;

	private final Surface surface;

	private final float baseSymbolHeight = 0; // Height of symbol with "usual" height, like "T", to be vertically
	// centered
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
	// private final boolean hasAlphaLayer = false;
	private Pattern foregroundPattern;
	private Pattern backgroundPattern;

	private final Point originalDrawingSize;
	private final Drawable drawable;
	private final Display device;

	private static Map<ColorType, int[]> colorTypeMap = null;
	private Matrix33 currentTransform = Matrix33.IDENTITY;

	private SamplingMode interpolationMode = SamplingMode.DEFAULT;

	private boolean isClipSet;
	private Rectangle currentClipBounds;
	private Region currentClipRegion;

	private final ISkiaCanvas skiaExtension;
	private final SkiaResources resources;
	private boolean XORModeActive;
	private final int style;
	private int textAntiAlias;

	public SkiaGC(org.eclipse.swt.widgets.Canvas canvas, ISkiaCanvas exst, int style) {
		this.drawable = canvas;
		final org.eclipse.swt.widgets.Canvas c = (org.eclipse.swt.widgets.Canvas) drawable;
		device = c.getDisplay();
		originalDrawingSize = extractSize(drawable);
		currentClipBounds = new Rectangle(0, 0, originalDrawingSize.x, originalDrawingSize.y);
		this.surface = exst.getSurface();
		this.skiaExtension = exst;
		this.resources = skiaExtension.getResources();
		this.style = style;
	}

	private static Point extractSize(Drawable drawable) {
		Point size = new Point(0, 0);
		if (drawable instanceof final Image image) {
			final var imageBounds = image.getBounds();
			size.x = imageBounds.width;
			size.y = imageBounds.height;
		} else if (drawable instanceof final Control control) {
			size = control.getSize();
		} else if (drawable instanceof final Device device) {
			final var deviceBounds = device.getBounds();
			size.x = deviceBounds.width;
			size.y = deviceBounds.height;
		}
		return size;
	}

	private static boolean isEmpty(Point area) {
		return area.x <= 0 || area.y <= 0;
	}

	@Override
	public void dispose() {

		resources.resetBaseColors();

		surface.getCanvas().restoreToCount(0);
		surface.getCanvas().resetMatrix();

	}

	private void performDraw(Consumer<Paint> operations) {
		try (final Paint paint = new Paint()) {
			paint.setAlpha(alpha);
			paint.setColor(convertSWTColorToSkijaColor(getForeground(), this.alpha));

			paint.setAntiAlias(this.antialias == SWT.OFF);
			if (this.XORModeActive) {
				paint.setBlendMode(BlendMode.DIFFERENCE);
			} else {
				paint.setBlendMode(BlendMode.SRC_OVER);
			}
			paint.setMode(PaintMode.STROKE);

			if (this.foregroundPattern != null && !this.foregroundPattern.isDisposed()) {
				final Shader shader = convertSWTPatternToSkijaShader(this.foregroundPattern);
				if (shader != null) {
					paint.setShader(shader);
				}
			}

			paint.setStrokeWidth(lineWidth);

			final PaintStrokeCap cap = getSkijaLineCap();
			paint.setStrokeCap(cap);

			switch (this.lineStyle) {
			case SWT.LINE_DOT:
				paint.setPathEffect(PathEffect.makeDash(new float[] { 1f * lineWidth, 1f * lineWidth }, 0.0f));
				break;
			case SWT.LINE_DASH:
				paint.setPathEffect(PathEffect.makeDash(new float[] { 3f * lineWidth, 1f * lineWidth }, 0.0f));
				break;
			case SWT.LINE_DASHDOT:
				paint.setPathEffect(PathEffect.makeDash(
						new float[] { 3f * lineWidth, 1f * lineWidth, 1f * lineWidth, 1f * lineWidth }, 0.0f));
				break;
			case SWT.LINE_DASHDOTDOT:
				paint.setPathEffect(PathEffect.makeDash(new float[] { 3f * lineWidth, 1f * lineWidth, 1f * lineWidth,
						1f * lineWidth, 1f * lineWidth, 1f * lineWidth }, 0.0f));
				break;
			default:
				paint.setPathEffect(null);
				break;
			}

			operations.accept(paint);
		}
	}

	// private void performDraw(Consumer<Paint> operations) {
	//
	// performDraw(operations);
	//
	// if(true) {
	// return;
	// }
	//
	// performForegroundDraw(paint -> {
	//
	// paint.setStrokeWidth(lineWidth);
	//
	// final PaintStrokeCap cap = getSkijaLineCap();
	// paint.setStrokeCap(cap); // e
	//
	// // if (lineStyle != SWT.LINE_SOLID && lineStyle != SWT.LINE_DASH && lineStyle
	// !=
	// // SWT.LINE_DOT
	// // && lineStyle != SWT.LINE_DASHDOT && lineStyle != SWT.LINE_DASHDOTDOT
	// // && lineStyle != SWT.LINE_CUSTOM) {
	// // SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	// // }
	//
	// switch (this.lineStyle) {
	// case SWT.LINE_DOT:
	// paint.setPathEffect(PathEffect.makeDash(new float[] { 1f * lineWidth, 1f *
	// lineWidth }, 0.0f));
	// break;
	// case SWT.LINE_DASH:
	// paint.setPathEffect(PathEffect.makeDash(new float[] { 3f * lineWidth, 1f *
	// lineWidth }, 0.0f));
	// break;
	// case SWT.LINE_DASHDOT:
	// paint.setPathEffect(PathEffect.makeDash(
	// new float[] { 3f * lineWidth, 1f * lineWidth, 1f * lineWidth, 1f * lineWidth
	// }, 0.0f));
	// break;
	// case SWT.LINE_DASHDOTDOT:
	// paint.setPathEffect(PathEffect.makeDash(new float[] { 3f * lineWidth, 1f *
	// lineWidth, 1f * lineWidth,
	// 1f * lineWidth, 1f * lineWidth, 1f * lineWidth }, 0.0f));
	// break;
	// default:
	// paint.setPathEffect(null);
	// break;
	// }
	//
	// // applyForegroundPattern(paint);
	// // paint.setMode(PaintMode.STROKE);
	// // paint.setStrokeWidth(lineWidth > 0 ? Math.max(lineWidth, 1) : 1);
	// // paint.setAntiAlias(true);
	// //
	// // // Apply line cap setting
	// // final PaintStrokeCap skijaLineCap = switch (lineCap) {
	// // case SWT.CAP_ROUND -> PaintStrokeCap.ROUND;
	// // case SWT.CAP_SQUARE -> PaintStrokeCap.SQUARE;
	// // case SWT.CAP_FLAT -> PaintStrokeCap.BUTT;
	// // default -> PaintStrokeCap.BUTT;
	// // };
	// // paint.setStrokeCap(skijaLineCap);
	// //
	// // // Apply line join setting
	// // final PaintStrokeJoin skijaLineJoin = switch (lineJoin) {
	// // case SWT.JOIN_MITER -> PaintStrokeJoin.MITER;
	// // case SWT.JOIN_ROUND -> PaintStrokeJoin.ROUND;
	// // case SWT.JOIN_BEVEL -> PaintStrokeJoin.BEVEL;
	// // default -> PaintStrokeJoin.BEVEL;
	// // };
	// // paint.setStrokeJoin(skijaLineJoin);
	// // // Apply line dash pattern based on line style
	// // final PathEffect pathEffect = createPathEffectForLineStyle();
	// // if (pathEffect != null) {
	// // paint.setPathEffect(pathEffect);
	// // }
	// operations.accept(paint);
	//
	// });
	// }

	private PaintStrokeCap getSkijaLineCap() {
		if ((this.lineCap == SWT.CAP_SQUARE)) {
			return PaintStrokeCap.SQUARE;
		}
		if (this.lineCap == SWT.CAP_ROUND) {
			return PaintStrokeCap.ROUND;
		}
		return PaintStrokeCap.BUTT;
	}

	/**
	 * Creates a PathEffect for the current line style and dash pattern.
	 *
	 * @return PathEffect for dashed lines, or null for solid lines
	 */
	private PathEffect createPathEffectForLineStyle() {
		float[] dashPattern = null;
		final float effectiveLineWidth = lineWidth > 0 ? DPIScaler.autoScaleUp(lineWidth) : 1;

		switch (lineStyle) {
		case SWT.LINE_SOLID:
			return null; // No path effect needed for solid lines

		case SWT.LINE_DOT:
			if (effectiveLineWidth == 1) {
				dashPattern = LINE_DOT_PATTERN.clone();
			} else {
				// Scale pattern based on line width
				dashPattern = new float[] { effectiveLineWidth, effectiveLineWidth };
			}
			break;

		case SWT.LINE_DASH:
			if (effectiveLineWidth == 1) {
				dashPattern = LINE_DASH_PATTERN.clone();
			} else {
				// Scale pattern based on line width
				dashPattern = new float[] { 6 * effectiveLineWidth, 2 * effectiveLineWidth };
			}
			break;

		case SWT.LINE_DASHDOT:
			if (effectiveLineWidth == 1) {
				dashPattern = LINE_DASHDOT_PATTERN.clone();
			} else {
				// Scale pattern based on line width
				dashPattern = new float[] { 3 * effectiveLineWidth, 2 * effectiveLineWidth, effectiveLineWidth,
						2 * effectiveLineWidth };
			}
			break;

		case SWT.LINE_DASHDOTDOT:
			if (effectiveLineWidth == 1) {
				dashPattern = LINE_DASHDOTDOT_PATTERN.clone();
			} else {
				// Scale pattern based on line width
				dashPattern = new float[] { 3 * effectiveLineWidth, effectiveLineWidth, effectiveLineWidth,
						effectiveLineWidth, effectiveLineWidth, effectiveLineWidth };
			}
			break;

		case SWT.LINE_CUSTOM:
			if (lineDashes != null && lineDashes.length > 0) {
				dashPattern = lineDashes.clone();
			}
			break;

		default:
			return null;
		}

		if (dashPattern != null && dashPattern.length > 0) {
			// Ensure even number of elements (on/off pairs)
			if (dashPattern.length % 2 != 0) {
				final float[] evenPattern = new float[dashPattern.length * 2];
				System.arraycopy(dashPattern, 0, evenPattern, 0, dashPattern.length);
				System.arraycopy(dashPattern, 0, evenPattern, dashPattern.length, dashPattern.length);
				dashPattern = evenPattern;
			}

			// Apply dash offset (scaled for DPI)
			final float scaledDashOffset = DPIScaler.autoScaleUp(dashOffset);
			return PathEffect.makeDash(dashPattern, scaledDashOffset);
		}

		return null;
	}

	private void performDrawText(Consumer<Paint> operations) {

		performDraw(paint -> {
			paint.setMode(PaintMode.FILL);
			operations.accept(paint);
		});

		// performForegroundDraw(paint -> {
		// applyForegroundPattern(paint);
		// operations.accept(paint);
		// });
	}

	private void performDrawFilled(Consumer<Paint> operations) {
		performDraw(paint -> {
			paint.setMode(PaintMode.FILL);
			paint.setAntiAlias(true);
			applyBackgroundPattern(paint);
			operations.accept(paint);
		});
	}

	private void applyBackgroundPattern(Paint paint) {
		if (backgroundPattern != null && !backgroundPattern.isDisposed()) {
			final Shader shader = convertSWTPatternToSkijaShader(backgroundPattern);
			if (shader != null) {
				paint.setShader(shader);
				return;
			}
		}
		// Fallback to backGround color if no pattern or pattern conversion failed
		if (this.alpha < 255) {
			paint.setColor(convertSWTColorToSkijaColor(getBackground(), this.alpha));
		} else {
			paint.setColor(convertSWTColorToSkijaColor(getBackground()));
		}

	}

	/**
	 * Applies the foreground pattern to the paint object if one is set. If no
	 * pattern is set, uses the foreground color.
	 *
	 * @param paint the Paint object to apply the pattern to
	 */
	private void applyForegroundPattern(Paint paint) {
		if (foregroundPattern != null && !foregroundPattern.isDisposed()) {
			final Shader shader = convertSWTPatternToSkijaShader(foregroundPattern);
			if (shader != null) {
				paint.setShader(shader);
				return;
			}
		}
		// Fallback to foreground color if no pattern or pattern conversion failed
		paint.setColor(convertSWTColorToSkijaColor(getForeground()));
	}

	// private void performDrawPoint(Consumer<Paint> operations) {
	// performDraw(paint -> {
	//// paint.setColor(convertSWTColorToSkijaColor(getForeground()));
	// paint.setMode(PaintMode.FILL);
	//// paint.setAntiAlias(false);
	// operations.accept(paint);
	// });
	// }

	private String[] splitString(String text) {
		String[] lines = new String[1];
		int start = 0, pos;
		do {
			pos = text.indexOf('\n', start);
			if (pos == -1) {
				lines[lines.length - 1] = text.substring(start);
			} else {
				final boolean crlf = (pos > 0) && (text.charAt(pos - 1) == '\r');
				lines[lines.length - 1] = text.substring(start, pos - (crlf ? 1 : 0));
				start = pos + 1;
				final String[] newLines = new String[lines.length + 1];
				System.arraycopy(lines, 0, newLines, 0, lines.length);
				lines = newLines;
			}
		} while (pos != -1);
		return lines;
	}

	// @Override
	// public void commit() {
	// if (isEmpty(originalDrawingSize)) {
	// return;
	// }
	// io.github.humbleui.skija.Image im = surface.makeImageSnapshot();
	// byte[] imageBytes = EncoderPNG.encode(im).getBytes();
	//
	// Image transferImage = new Image(innerGC.getDevice(), new
	// ByteArrayInputStream(imageBytes));
	//
	// Point drawingSizeInPixels = DPIScaler.autoScaleUp(originalDrawingSize);
	// innerGC.drawImage(transferImage, 0, 0, drawingSizeInPixels.x,
	// drawingSizeInPixels.y, //
	// 0, 0, originalDrawingSize.x, originalDrawingSize.y);
	// transferImage.dispose();
	// }

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

	@Override
	public void drawImage(Image image, int x, int y) {
		final var imgBounds = image.getBounds();
		drawImage(image, 0, 0, imgBounds.width, imgBounds.height, x, y, imgBounds.width, imgBounds.height);
	}

	@Override
	public void drawImage(Image image, int destX, int destY, int destWidth, int destHeight) {
		drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, destX, destY, destWidth, destHeight);

	}

	@Override
	public void drawImage(Image image, int srcX, int srcY, int srcWidth, int srcHeight, int destX, int destY,
			int destWidth, int destHeight) {
		if (image == null) {
			System.out.println("SkijaGC.drawImage(..): Error draw image that is null!!");
			return;
		}

		int factor = Math.round(Math.max(destWidth / srcWidth, destHeight / srcHeight));

		if (factor == 0) {
			factor = 1;
		}

		final Canvas canvas = surface.getCanvas();

		final var fgp = getForegroundPaint();
		fgp.setAlpha(alpha);

		// TODO create an image cache, instead of recreating the skija image every time
		canvas.drawImageRect(convertSWTImageToSkijaImage(image, 100 * factor),
				createScaledRectangle(srcX * factor, srcY * factor, srcWidth * factor, srcHeight * factor),
				createScaledRectangle(destX, destY, destWidth, destHeight), this.interpolationMode, fgp, false);
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

		// throw new UnsupportedOperationException("Unsupported SWT ColorType: " +
		// Integer.toBinaryString(palette.redMask)
		// + "__" + Integer.toBinaryString(palette.greenMask) + "__" +
		// Integer.toBinaryString(palette.blueMask));
	}

	private static io.github.humbleui.skija.Image convertSWTImageToSkijaImage(Image swtImage, int zoom) {
		final ImageData imageData = swtImage.getImageData(zoom);
		return convertSWTImageToSkijaImage(imageData);
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

		final ImageData transparencyData = imageData.getTransparencyMask();
		final byte[] convertedData = new byte[imageData.width * imageData.height * 4];
		byte defaultAlpha = (byte) 255;

		final var source = imageData.data;
		final int bytesPerPixel = source.length / (imageData.width * imageData.height);

		final var alphaData = imageData.alphaData;
		if (imageData.alpha != -1) {
			defaultAlpha = (byte) imageData.alpha;
		}

		final boolean byteSourceContainsAlpha = bytesPerPixel > 3;

		final RGB[] cols = imageData.palette.colors;

		for (int y = 0; y < imageData.height; y++) {
			for (int x = 0; x < imageData.width; x++) {
				final int pixel = imageData.getPixel(x, y);
				final int arrayPos = (y * imageData.width + x);

				byte r = (byte) ((pixel & imageData.palette.redMask) >>> -imageData.palette.redShift);
				byte g = (byte) ((pixel & imageData.palette.greenMask) >>> -imageData.palette.greenShift);
				byte b = (byte) ((pixel & imageData.palette.blueMask) >>> -imageData.palette.blueShift);

				if (cols != null) {
					final RGB rgb = cols[pixel];

					r = (byte) rgb.red;
					b = (byte) rgb.blue;
					g = (byte) rgb.green;
				}

				byte a = (byte) 255;
				if (transparencyData != null) {
					if (transparencyData.getPixel(x, y) != 1) {
						a = (byte) 0;
					}
				}

				a = (byte) imageData.getAlpha(x, y);

				final var index = arrayPos * 4;

				convertedData[index + 0] = r;
				convertedData[index + 1] = g;
				convertedData[index + 2] = b;
				convertedData[index + 3] = a;

				// if (alphaData != null && alphaData.length > arrayPos) {
				// convertedData[index + 3] = alphaData[arrayPos];
				// } else if (imageData.alpha != -1) {
				// convertedData[index + 3] = defaultAlpha;
				// } else if(!byteSourceContainsAlpha) {
				// convertedData[index + 3] = defaultAlpha;
				// }
			}
		}

		return convertedData;
	}

	static ImageData convertToSkijaImageData(io.github.humbleui.skija.Image image) {
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
			e.printStackTrace();
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

	public static byte[] convertPremulToUnpremul(byte[] premulColor) {
		if (premulColor.length != 4) {
			throw new IllegalArgumentException("Input array must have a length of 4.");
		}

		final int rPremul = premulColor[0] & 0xFF;
		final int gPremul = premulColor[1] & 0xFF;
		final int bPremul = premulColor[2] & 0xFF;
		final int a = premulColor[3] & 0xFF;

		if (a == 0) {
			// no conversion necessary if alpha is 0
			return new byte[] { (byte) rPremul, (byte) gPremul, (byte) bPremul, (byte) a };
		}

		// convert premul -> unpremul
		int r = (rPremul * 255) / a;
		int g = (gPremul * 255) / a;
		int b = (bPremul * 255) / a;

		// Ensure values are within the valid range [0, 255]
		r = Math.min(255, Math.max(0, r));
		g = Math.min(255, Math.max(0, g));
		b = Math.min(255, Math.max(0, b));

		return new byte[] { (byte) r, (byte) g, (byte) b, (byte) a };
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
		performDraw(paint -> surface.getCanvas().drawLine(DPIScaler.autoScaleUp(x1) + scaledOffsetValue,
				DPIScaler.autoScaleUp(y1) + scaledOffsetValue, DPIScaler.autoScaleUp(x2) + scaledOffsetValue,
				DPIScaler.autoScaleUp(y2) + scaledOffsetValue, paint));
	}

	@Override
	public Color getForeground() {
		return this.resources.getForeground();
	}

	private Paint getForegroundPaint() {
		return this.resources.getForegroundPaint();
	}

	private Paint getBackgroundPaint() {
		return this.resources.getBackgroundPaint();
	}

	@Override
	public void drawText(String string, int x, int y) {
		drawText(string, x, y, false);
	}

	@Override
	public void drawText(String string, int x, int y, boolean isTransparent) {
		drawText(string, x, y, isTransparent ? SWT.TRANSPARENT : SWT.NONE);
	}

	@Override
	public void drawText(String text, int x, int y, int flags) {
		if (text == null) {
			return;
		}
		if (text.contains("\t")) {
			text = expandTabs(text, x);
		}
		drawTextBlob(text, flags, x, y);
	}

	/**
	 * Expands tab characters (\t) in the text to position-dependent spaces, so that
	 * the next character aligns to the next tab stop (every 8 average character
	 * widths by default). The expansion is based on the current x position and the
	 * average character width of the font.
	 *
	 * @param text   The input text containing tab characters
	 * @param startX The starting x position in pixels (used to calculate tab
	 *               alignment)
	 * @return The text with tabs expanded to spaces, aligned to the next tab stop
	 */
	private String expandTabs(String text, int startX) {
		final StringBuilder result = new StringBuilder();
		int currentX = 0;
		final int spaceWidth = textExtent(" ").x;
		final float _avgCharWidth = getSkiaFont().getMetrics()._avgCharWidth;
		int avgCharWidth = (int) _avgCharWidth;
		if (avgCharWidth <= 0) {
			avgCharWidth = spaceWidth > 0 ? spaceWidth : 1;
		}
		final int tabSpacingPx = 8 * avgCharWidth;
		for (int i = 0; i < text.length(); i++) {
			final char ch = text.charAt(i);
			if (ch == '\t') {
				final int offsetInTab = tabSpacingPx > 0 ? (currentX - startX) % tabSpacingPx : 0;
				final int nextTabX = currentX + (tabSpacingPx - offsetInTab);
				while (currentX < nextTabX) {
					result.append(' ');
					currentX += textExtent(" ").x;
				}
			} else {
				final String s = String.valueOf(ch);
				final int charWidth = textExtent(s).x;
				result.append(ch);
				currentX += charWidth;
			}
		}
		return result.toString();
	}

	// y position in drawTextBlob() is the text baseline, e.g., the bottom of "T"
	// but the middle of "y"
	// So center a base symbol (like "T") in the desired text box (according to
	// parameter y being the top left text box position and the text box height
	// according to font metrics)
	private Point calculateSymbolCenterPoint(int x, int y) {
		final int top = (int) (y - getSkiaFont().getMetrics().getAscent());
		return new Point(x, top);
	}

	private io.github.humbleui.skija.Font getSkiaFont() {
		return this.resources.getSkiaFont();
	}

	static String withCrLf(String string) {
		/* Create a new string with the CR/LF line terminator. */
		int i = 0;
		final int length = string.length();
		final StringBuilder result = new StringBuilder(length);
		while (i < length) {
			int j = string.indexOf('\n', i);
			if (j > 0 && string.charAt(j - 1) == '\r') {
				result.append(string.substring(i, j + 1));
				i = j + 1;
			} else {
				if (j == -1) {
					j = length;
				}
				result.append(string.substring(i, j));
				i = j;
				if (i < length) {
					result.append("\r\n"); //$NON-NLS-1$
					i++;
				}
			}
		}

		/* Avoid creating a copy of the string if it has not changed */
		if (string.length() == result.length()) {
			return string;
		}
		return result.toString();
	}

	private void drawTextBlob(String inputText, int flags, int x, int y) {

		if (this.surface.getWidth() < x || this.surface.getHeight() < y) {
			return;
		}

		// final var i = this.resources.getTextImage(text, flags);
		// if (i != null) {
		// if (x < this.surface.getWidth() && y < this.surface.getHeight()) {
		// final var r = resources.getScaler().scaleSize(x, y);
		// surface.getCanvas().drawImage(i, r.x, r.y);
		// }
		// return;
		// }

		final var fullText = replaceMnemonics(inputText);

		final var f = getSkiaFont();

		final var splits = splitString(fullText); // $NON-NLS-1$

		performDrawText(fgp -> {
			fgp.setAntiAlias(false);
			fgp.setMode(PaintMode.FILL);

			f.setSubpixel(false);
			f.setEdging(FontEdging.ALIAS);

			fgp.setStrokeWidth(1);
			fgp.setStrokeCap(PaintStrokeCap.BUTT);
			fgp.setPathEffect(null);

			final int heightDiff = 0;
			final int index = 0;
			for (final var text : splits) { // $NON-NLS-1$

				final var rect = f.measureText(text, fgp);

				final var textWidth = rect.getWidth();
				final var textHeight = rect.getHeight();

				final var metric = f.getMetrics();
				final var asc = metric.getAscent();
				final var des = metric.getDescent();
				final var leading = metric.getLeading();

				final var ascI = (int) Math.ceil(Math.abs(asc));
				final var desI = (int) Math.ceil(Math.abs(des));
				final var heightI = ascI + desI;

				final int width = 1;

				final io.github.humbleui.skija.Image img;

				// heightDiff = -(int) Math.ceil(heightI - textHeight);
				final var r = resources.getScaler().scaleSize(x, y);
				surface.getCanvas().drawString(text, r.x, r.y + ascI + heightDiff, f, fgp);

				//				try (var subSurface = skiaExtension.createSupportSurface((int) Math.ceil(textWidth) + width, heightI)) {
				//
				//					if (isTransparent(flags)) {
				//						subSurface.getCanvas().clear(0x00000000);
				//					} else {
				//						subSurface.getCanvas().clear(getBackgroundPaint().getColor());
				//					}
				//
				//					final var skiaFM = getFontMetrics();
				//
				//					index++;
				//					heightDiff += heightI + 1;
				//
				//					img = subSurface.makeImageSnapshot();
				//					this.resources.setTextImage(text, flags, img);
				//
				//				}
				// if (x < this.surface.getWidth() && y < this.surface.getHeight()) {
				//
				// surface.getCanvas().drawImage(img, r.x, r.y + heightDiff);
				// }
			}
		});

	}

	private static boolean isTransparent(int flags) {
		return (SWT.TRANSPARENT & flags) != 0;
	}

	private static String replaceMnemonics(String text) {
		final int mnemonicIndex = text.lastIndexOf('&');
		if (mnemonicIndex != -1) {
			text = text.replaceAll("&", "");
			// TODO Underline the mnemonic key
		}
		return text;
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		performDraw(paint -> surface.getCanvas().drawArc(DPIScaler.autoScaleUp(x), DPIScaler.autoScaleUp(y),
				DPIScaler.autoScaleUp(x + width), DPIScaler.autoScaleUp(y + height), -startAngle, -arcAngle, false,
				paint));
	}

	@Override
	public void drawFocus(int x, int y, int width, int height) {
		performDraw(paint -> {
			paint.setPathEffect(PathEffect.makeDash(new float[] { 1.5f, 1.5f }, 0.0f));
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
			performDraw(paint -> surface.getCanvas().drawPath(skijaPath, paint));
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
		try (io.github.humbleui.skija.Path path = new io.github.humbleui.skija.Path()) {
			// Move to first point
			path.moveTo(DPIScaler.autoScaleUp(pointArray[0]), DPIScaler.autoScaleUp(pointArray[1]));
			// Add lines to subsequent points
			for (int i = 2; i < pointArray.length; i += 2) {
				path.lineTo(DPIScaler.autoScaleUp(pointArray[i]), DPIScaler.autoScaleUp(pointArray[i + 1]));
			}
			path.closePath();
			// Draw the polygon outline
			performDraw(paint -> surface.getCanvas().drawPath(path, paint));
		}
		// Restore x-coordinates if mirrored
		if (adjustX) {
			for (int i = 0; i < pointArray.length; i += 2) {
				pointArray[i]++;
			}
		}
	}

	private static io.github.humbleui.skija.Path convertSWTPathToSkijaPath(Path swtPath) {
		if (swtPath == null || swtPath.isDisposed()) {
			return null;
		}
		final PathData data = swtPath.getPathData();
		final io.github.humbleui.skija.Path skijaPath = new io.github.humbleui.skija.Path();
		final float[] pts = data.points;
		final byte[] types = data.types;
		int pi = 0;
		for (final byte type : types) {
			switch (type) {
			case SWT.PATH_MOVE_TO:
				skijaPath.moveTo(DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]));
				break;
			case SWT.PATH_LINE_TO:
				skijaPath.lineTo(DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]));
				break;
			case SWT.PATH_CUBIC_TO:
				skijaPath.cubicTo(DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]),
						DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]),
						DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]));
				break;
			case SWT.PATH_QUAD_TO:
				skijaPath.quadTo(DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]),
						DPIScaler.autoScaleUp(pts[pi++]), DPIScaler.autoScaleUp(pts[pi++]));
				break;
			case SWT.PATH_CLOSE:
				skijaPath.closePath();
				break;
			default:
			}
		}
		return skijaPath;
	}

	@Override
	public void drawPolyline(int[] pointArray) {
		performDraw(paint -> surface.getCanvas().drawPolygon(convertToFloat(pointArray), paint));
	}

	private static int[] createLinesArray(int[] array) {

		final List<Point> pts1 = new LinkedList<>();
		final List<Point> pts2 = new LinkedList<>();

		for (int i = 0; i < (array.length - 1); i = i + 2) {

			pts1.add(new Point(array[i], array[i + 1]));

		}

		pts2.add(pts1.get(0));
		for (int i = 1; i < pts1.size() - 1; i++) {

			pts2.add(pts1.get(i));
			pts2.add(pts1.get(i));

		}
		pts2.add(pts1.get(pts1.size() - 1));

		final int[] res = new int[pts2.size() * 2];

		for (int i = 0; i < pts2.size(); i++) {
			res[2 * i] = pts2.get(i).x;
			res[2 * i + 1] = pts2.get(i).y;
		}

		return res;
	}

	private static float[] convertToFloat(int[] array) {
		final float[] arrayAsFloat = new float[array.length];
		for (int i = 0; i < array.length; i++) {
			arrayAsFloat[i] = DPIScaler.autoScaleUp(array[i]);
		}
		return arrayAsFloat;
	}

	@Override
	public void drawRectangle(int x, int y, int width, int height) {

		// final var p = getForegroundPaint();
		// final var bmode = p.getBlendMode();
		// if (XORModeActive) {
		// p.setBlendMode(BlendMode.DIFFERENCE);
		// }
		//
		// p.setBlendMode(bmode);

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
		performDrawFilled(paint -> surface.getCanvas().drawArc(DPIScaler.autoScaleUp(x), DPIScaler.autoScaleUp(y),
				DPIScaler.autoScaleUp(x + width), DPIScaler.autoScaleUp(y + height), -startAngle, -arcAngle, true,
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
		performDrawGradientFilled(paint -> surface.getCanvas().drawRect(rect, paint), x, y, x2, y2, fromColor, toColor);
	}

	private void performDrawGradientFilled(Consumer<Paint> operations, int x, int y, int x2, int y2, int fromColor,
			int toColor) {
		performDraw(paint -> {
			try (Shader gradient = Shader.makeLinearGradient(DPIScaler.autoScaleUp(x), DPIScaler.autoScaleUp(y),
					DPIScaler.autoScaleUp(x2), DPIScaler.autoScaleUp(y2), new int[] { fromColor, toColor }, null,
					GradientStyle.DEFAULT)) {
				paint.setShader(gradient);
				paint.setAntiAlias(true);
				operations.accept(paint);
			}
		});
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
			performDrawFilled(paint -> surface.getCanvas().drawPath(skijaPath, paint));
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
		try (io.github.humbleui.skija.Path path = new io.github.humbleui.skija.Path()) { // Move to first point
			path.moveTo(DPIScaler.autoScaleUp(pointArray[0]), DPIScaler.autoScaleUp(pointArray[1]));
			// Add lines to subsequent points
			for (int i = 2; i < pointArray.length; i += 2) {
				path.lineTo(DPIScaler.autoScaleUp(pointArray[i]), DPIScaler.autoScaleUp(pointArray[i + 1]));
			}
			// Close the path to form a polygon
			path.closePath();
			path.setFillMode(fillRule == SWT.FILL_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
			// Fill the polygon
			performDrawFilled(paint -> surface.getCanvas().drawPath(path, paint));
		}
	}

	@Override
	public void fillRectangle(int x, int y, int width, int height) {

		// final Paint p = new Paint();
		// p.setAlpha(255);

		// final Shader s = Shader.makeLinearGradient(0,0, 100 , 100 , new int[] {
		// 0xFF00FFFF, 0x00FF00FF} );
		// p.setShader(s);
		//

		// final var s = convertSWTPatternToSkijaShader(backgroundPattern);
		//
		// p.setShader(s);
		// surface.getCanvas().drawRect(createScaledRectangle(x, y, width, height), p);
		performDrawFilled(paint -> surface.getCanvas().drawRect(createScaledRectangle(x, y, width, height), paint));

		// surface.getCanvas().drawRect(createScaledRectangle(x, y, width, height),
		// getBackgroundPaint());
	}

	@Override
	public void fillRoundRectangle(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		performDrawFilled(paint -> surface.getCanvas()
				.drawRRect(createScaledRoundRectangle(x, y, width, height, arcWidth / 2.0f, arcHeight / 2.0f), paint));
	}

	@Override
	public Point textExtent(String text, int flags) {

		final float height = getSkiaFont().getMetrics().getHeight();
		final float width = getSkiaFont().measureTextWidth(replaceMnemonics(text));
		return new Point((int) width, (int) height);
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

		surface.getCanvas().restoreToCount(0);

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
					elements[4], // dx -> transX
					elements[1], // m12 -> skewY
					elements[3], // m22 -> scaleY
					elements[5], // dy -> transY
					0, 0, 1 // perspective elements
			};
			currentTransform = new Matrix33(skijaMat);
			surface.getCanvas().setMatrix(currentTransform);
		}

		surface.getCanvas().save();

	}

	@Override
	public void setAlpha(int alpha) {
		alpha = alpha & 0xFF;

		if (alpha < 0 || alpha > 255) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.alpha = alpha;
		// if (this.alpha != alpha) {
		// if (hasAlphaLayer) {
		// surface.getCanvas().restore();
		// hasAlphaLayer = false;
		// }
		// this.alpha = alpha;
		// if (alpha < 255) {
		// try (Paint layerPaint = new Paint()) {
		// layerPaint.setAlphaf(alpha / 255.0f);
		// surface.getCanvas().saveLayer(null, layerPaint);
		// layerPaint.close();
		// }
		// hasAlphaLayer = true;
		// }
		// }
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
		final float widthHightAutoScaleOffset = DPIScaler.autoScaleUp(1) - 1.0f;
		if (scaledOffsetValue != 0f) {
			return new Rect(rect.getLeft() + scaledOffsetValue, rect.getTop() + scaledOffsetValue,
					rect.getRight() + scaledOffsetValue + widthHightAutoScaleOffset,
					rect.getBottom() + scaledOffsetValue + widthHightAutoScaleOffset);
		}
		return rect;
	}

	private RRect offsetRectangle(RRect rect) {
		final float scaledOffsetValue = getScaledOffsetValue();
		final float widthHightAutoScaleOffset = DPIScaler.autoScaleUp(1) - 1.0f;
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
		final var r = resources.getScaler().scaleBounds(new Rectangle(x, y, width, height), DPIUtil.getDeviceZoom());
		return new Rect(r.x, r.y, r.width + r.x, r.height + r.y);
	}

	private float getScaledOffsetValue() {
		final boolean isDefaultLineWidth = lineWidth == 0;
		if (isDefaultLineWidth) {
			return 0.5f;
		}
		final int effectiveLineWidth = DPIScaler.autoScaleUp(lineWidth);
		if (effectiveLineWidth % 2 == 1) {
			return DPIScaler.autoScaleUp(0.5f);
		}
		return 0f;
	}

	private RRect createScaledRoundRectangle(int x, int y, int width, int height, float arcWidth, float arcHeight) {
		return new RRect(DPIScaler.autoScaleUp(x), DPIScaler.autoScaleUp(y), DPIScaler.autoScaleUp(x + width),
				DPIScaler.autoScaleUp(y + height),
				new float[] { DPIScaler.autoScaleUp(arcWidth), DPIScaler.autoScaleUp(arcHeight) });
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
		attributes.width = DPIScaler.autoScaleDown(attributes.width);
		if (attributes.dash != null) {
			attributes.dash = DPIScaler.autoScaleDown(attributes.dash);
		}
		return attributes;
	}

	LineAttributes getLineAttributesInPixels() {
		return new LineAttributes(lineWidth, SWT.CAP_FLAT, SWT.JOIN_MITER, SWT.LINE_SOLID, null, 0, 10);
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
		io.github.humbleui.skija.Image skijaImage = convertSWTImageToSkijaImage(image, DPIScaler.getDeviceZoom());
		final io.github.humbleui.skija.Image copiedArea = surface.makeImageSnapshot(
				createScaledRectangle(x, y, skijaImage.getWidth(), skijaImage.getHeight()).toIRect());

		if (copiedArea != null) {
			final Surface imageSurface = surface.makeSurface(skijaImage.getWidth(), skijaImage.getHeight());
			final Canvas imageCanvas = imageSurface.getCanvas();
			imageCanvas.drawImage(copiedArea, 0, 0);
			skijaImage = imageSurface.makeImageSnapshot();
			final ImageData imgData = convertToSkijaImageData(skijaImage);
			final Image i = new Image(device, imgData);

			final GC gc = new GC(image);
			gc.drawImage(i, 0, 0);
			gc.dispose();
			i.dispose();
		} else {
			System.err.println(
					"WARN: Area copied at given x,y co-ordinates is null: " + new Throwable().getStackTrace()[0]);
		}
	}

	@Override
	public void copyArea(int srcX, int srcY, int width, int height, int destX, int destY) {

		final io.github.humbleui.skija.Image copiedArea = surface
				.makeImageSnapshot(createScaledRectangle(srcX, srcY, width, height).toIRect());
		surface.getCanvas().drawImage(copiedArea, DPIScaler.autoScaleUp(destX), DPIScaler.autoScaleUp(destY));
	}

	@Override
	public void copyArea(int srcX, int srcY, int width, int height, int destX, int destY, boolean paint) {
		copyArea(srcX, srcY, width, height, destX, destY);
		if (paint) {
			// cut-paste behaviour
			// surface.getCanvas().save();
			// surface.getCanvas().clipRect(createScaledRectangle(srcX, srcY, width,
			// height));
			// surface.getCanvas().clear(0x00000000);
			// surface.getCanvas().restore();
			/** TODO - Implement correct behavior when paint is true **/
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

		System.err.println("WARN: Not implemented yet: " + new Throwable().getStackTrace()[0]);
	}

	@Override
	public int getAdvanceWidth(char ch) {

		final var f = getSkiaFont();

		final var fgp = getForegroundPaint();

		fgp.setAntiAlias(false);
		fgp.setMode(PaintMode.FILL);

		final var textWidth = f.measureTextWidth(String.valueOf(ch), fgp);
		return (int) Math.ceil(textWidth);
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
			lineDashesInt[i] = DPIScaler.autoScaleDownToInt(lineDashes[i]);
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
		return XORModeActive;
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
			canvas.restore();
			isClipSet = false;
		}
		if (path == null) {
			return;
		}
		if (path.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		final io.github.humbleui.skija.Path skijaPath = convertSWTPathToSkijaPath(path);
		if (skijaPath == null) {
			return;
		}
		skijaPath.setFillMode(fillRule == SWT.FILL_EVEN_ODD ? PathFillMode.EVEN_ODD : PathFillMode.WINDING);
		canvas.save();
		isClipSet = true;
		canvas.clipPath(skijaPath, ClipMode.INTERSECT, true);
	}

	@Override
	public void setClipping(Rectangle rect) {

		/**
		 * this is a minimal implementation for set clipping with skija.
		 */

		// skija seems to work with state layer which will be set on top of each other.
		// if more layers will be used a more complex handling is necessary
		final Canvas canvas = surface.getCanvas();
		if (isClipSet) {
			canvas.restore();
			isClipSet = false;
		}
		if (rect == null) {
			currentClipBounds = new Rectangle(0, 0, originalDrawingSize.x, originalDrawingSize.y);
			return;
		}
		currentClipBounds = new Rectangle(rect.x, rect.y, rect.width, rect.height);
		canvas.save();
		canvas.clipRect(createScaledRectangle(rect));
		isClipSet = true;

	}

	@Override
	public void setClipping(Region region) {

		/**
		 * this is a minimal implementation for set clipping with skija.
		 */

		// skija seems to work with state layer which will be set on top of each other.
		// if more layers will be used a more complex handling is necessary
		final Canvas canvas = surface.getCanvas();
		if (isClipSet) {
			canvas.restore();
			isClipSet = false;
		}
		currentClipBounds = new Rectangle(0, 0, originalDrawingSize.x, originalDrawingSize.y);
		currentClipRegion = region;

		if (region == null) {
			return;
		}

		final SkiaRegionCalculator calc = new SkiaRegionCalculator(region, skiaExtension);
		final var skiaRegion = calc.getSkiaRegion();
		canvas.save();
		canvas.clipRegion(skiaRegion);
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
		case SWT.NONE:
			this.interpolationMode = SamplingMode.DEFAULT; // Nearest neighbor
			break;
		case SWT.LOW:
			this.interpolationMode = SamplingMode.LINEAR;
			break;
		case SWT.DEFAULT:
			this.interpolationMode = SamplingMode.MITCHELL;
		case SWT.HIGH:
			this.interpolationMode = SamplingMode.CATMULL_ROM;
			break;
		default:
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
	}

	@Override
	public void setLineAttributes(LineAttributes attributes) {
		if (attributes == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		final float scaledWidth = DPIScaler.autoScaleUp(attributes.width);
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
				newDashes[i] = DPIScaler.autoScaleUp(dash);

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
				newDashes[i] = DPIScaler.autoScaleUp((float) dashes[i]);
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

		this.XORModeActive = xor;

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
	private static Shader convertSWTPatternToSkijaShader(Pattern pattern) {

		final var props = PatternProperties.get(pattern);

		if (props.getImage() == null) {

			final int col1 = convertSWTColorToSkijaColor(props.getColor1(), props.getAlpha1());
			final int col2 = convertSWTColorToSkijaColor(props.getColor2(), props.getAlpha2());

			final var gs = new GradientStyle(FilterTileMode.REPEAT, true, null);
			final Shader s = Shader.makeLinearGradient(props.getBaseX1(), props.getBaseY1(), props.getBaseX2(),
					props.getBaseY2(), new int[] { col1, col2 }, null, gs);

			return s;
		}

		final var image = convertSWTImageToSkijaImage(props.getImage(), 100);
		return image.makeShader(FilterTileMode.REPEAT);

	}

	@Override
	public FontMetrics getFontMetrics() {

		final var font = getSkiaFont();
		final var m = font.getMetrics();

		final var fe = new FontMetricsExtension(new SkiaFontMetrics(m));
		return fe;
	}

}
