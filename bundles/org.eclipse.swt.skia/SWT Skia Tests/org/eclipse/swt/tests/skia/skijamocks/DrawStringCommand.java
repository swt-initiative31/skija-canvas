package org.eclipse.swt.tests.skia.skijamocks;

import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;

public class DrawStringCommand implements DrawCommand {

	public String stringToDraw;
	public float x;
	public float y;
	public FontData font;
	public PaintData paint;

	DrawStringCommand(String s, float x, float y, Font font, Paint paint) {

		this.stringToDraw = s;
		this.x = x;
		this.y = y;
		this.font = FontData.extractData(font);
		this.paint = PaintData.extractData(paint);

	}
}
