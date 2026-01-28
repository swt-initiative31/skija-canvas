package org.eclipse.swt.graphics;

public class FontProperties {

	public int lfHeight;
	public int lfWidth;
	public int lfEscapement;
	public int lfOrientation;
	public int lfWeight;
	public byte lfItalic;
	public byte lfUnderline;
	public byte lfStrikeOut;
	public String name;

	private FontProperties() {

	}

	public static FontProperties getFontProperties(FontData fd) {
		var fp = new FontProperties();
		var d = fd.data;

		fp.name = fd.getName();
		fp.lfHeight = fd.getHeight();
		fp.lfItalic = d.lfItalic;
		fp.lfEscapement = d.lfEscapement;
		fp.lfOrientation = d.lfOrientation;
		fp.lfStrikeOut = d.lfStrikeOut;
		fp.lfUnderline = d.lfUnderline;
		fp.lfWeight = d.lfWeight;
		fp.lfWidth = d.lfWidth;

		return fp;
	}

	public static FontProperties getFontProperties(Font font) {
		return getFontProperties(font.getFontData()[0]);
	}

}
