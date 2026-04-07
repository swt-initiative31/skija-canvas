package org.eclipse.swt.tests.skia.skijamocks;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontSlant;

public class FontData {

	public float size;
	public String name;
	public FontSlant slant;
	public int weight;
	public int width;

	public static FontData extractData(Font f) {

		FontData data = new FontData();
		data.size = f.getSize();
		var typeface = f.getTypeface();
		data.name = typeface.getFamilyName();
		var style = typeface.getFontStyle();
		data.slant = style.getSlant();
		data.weight = style.getWeight();
		data.width = style.getWidth();

		return data;

	}

}
