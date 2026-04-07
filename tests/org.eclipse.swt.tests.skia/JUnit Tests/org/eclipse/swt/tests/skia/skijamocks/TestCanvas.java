package org.eclipse.swt.tests.skia.skijamocks;

import java.util.ArrayList;
import java.util.List;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;

public class TestCanvas extends Canvas {

	public List<DrawCommand> executedCommands = new ArrayList<>();
	public Canvas canvas;

	public TestCanvas(Canvas c) {
		super(c._ptr, true, c._owner);
		this.canvas = c;
	}

	@Override
	public Canvas drawString(String s, float x, float y, Font font, Paint paint) {

		executedCommands.add(new DrawStringCommand(s, x, y, font, paint));

		canvas.drawString(s, x, y, font, paint);

		return this;
	}


}
