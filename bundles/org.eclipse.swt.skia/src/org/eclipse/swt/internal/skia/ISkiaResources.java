package org.eclipse.swt.internal.skia;

public interface ISkiaResources {
	void setBackground(org.eclipse.swt.graphics.Color color);
	void setForeground(org.eclipse.swt.graphics.Color color);
	org.eclipse.swt.graphics.Color getForeground();
	org.eclipse.swt.graphics.Color getBackground();
	void setFont(org.eclipse.swt.graphics.Font font);
	io.github.humbleui.skija.Font getSkiaFont();
	org.eclipse.swt.internal.canvasext.IDpiScaler getScaler();
	org.eclipse.swt.graphics.Font getFont();
	void resetBaseColors();
	void cacheImage(org.eclipse.swt.graphics.Image swtImage, int zoom, io.github.humbleui.skija.Image skijaImage);
	io.github.humbleui.skija.Image getCachedImage(org.eclipse.swt.graphics.Image swtImage, int zoom);
	void cacheTextImage(String text, org.eclipse.swt.internal.canvasext.FontProperties fontProperties, boolean transparent, int background, int foreground, boolean antiAlias, io.github.humbleui.skija.Image skijaImage);
	io.github.humbleui.skija.Image getTextImage(String text, org.eclipse.swt.internal.canvasext.FontProperties fontProperties, boolean transparent, int background, int foreground, boolean antialias);
	String[] getTextSplits(String inputText, int flags);
	org.eclipse.swt.graphics.Point textExtent(String text, int flags);
	org.eclipse.swt.graphics.Point textExtent(String string);
}