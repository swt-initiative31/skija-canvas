package org.eclipse.swt.tests.doubles;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.internal.skia.ISkCanvas;

import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.Region;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

public class SkCanvasDouble implements ISkCanvas {

	int saveCount;
	List<MethodCall> calls = new ArrayList<>();

	@Override
	public int getSaveCount() {
		return saveCount;
	}

	@Override
	public void restoreToCount(int saveCount) {
		calls.add(logCall("restoreToCount", saveCount));
		this.saveCount = saveCount;

	}

	@Override
	public void drawLine(float x0, float y0, float x1, float y1, Paint paint) {
		calls.add(logCall("drawLine", x0, y0, x1, y1, paint));
	}

	@Override
	public void drawArc(int left, int top, int right, int bottom, int startAngle, int sweepAngle, boolean useCenter,
			Paint paint) {
		calls.add(logCall("drawArc", left, top, right, bottom, startAngle, sweepAngle, useCenter, paint));
	}

	@Override
	public void drawRect(Rect rect, Paint paint) {
		calls.add(logCall("drawRect", rect, paint));
	}

	@Override
	public void drawOval(Rect rect, Paint paint) {
		calls.add(logCall("drawOval", rect, paint));
	}

	@Override
	public void drawPath(Path path, Paint paint) {
		calls.add(logCall("drawPath", path, paint));
	}

	@Override
	public void drawPolygon(float[] points, Paint paint) {
		calls.add(logCall("drawPolygon", points, paint));
	}

	@Override
	public void drawRRect(RRect rrect, Paint paint) {
		calls.add(logCall("drawRRect", rrect, paint));
	}

	@Override
	public void drawImage(Image image, int x, int y) {
		calls.add(logCall("drawImage", image, x, y));
	}

	@Override
	public void drawImageRect(Image image, Rect src, Rect dst, SamplingMode sampling, Paint paint, boolean strict) {
		calls.add(logCall("drawImageRect", image, src, dst, sampling, paint, strict));
	}

	@Override
	public void setMatrix(Matrix33 matrix) {
		calls.add(logCall("setMatrix", matrix));
	}

	@Override
	public void save() {
		calls.add(logCall("save"));
		saveCount++;
	}

	@Override
	public void clipRect(Rect rect) {
		calls.add(logCall("clipRect", rect));
	}

	@Override
	public void clear(int color) {
		calls.add(logCall("clear", color));
	}

	@Override
	public void restore() {
		calls.add(logCall("restore"));
	}

	@Override
	public void clipPath(Path skijaPath, ClipMode intersect, boolean b) {
		calls.add(logCall("clipPath", skijaPath, intersect, b));
	}

	@Override
	public void drawString(String text, int x, int i, Font f, Paint fgp) {
		calls.add(logCall("drawString", text, x, i, f, fgp));
	}

	@Override
	public void clipRegion(Region skiaRegion) {
		calls.add(logCall("clipRegion", skiaRegion));
	}

	private static MethodCall logCall(String methodName, Object... args) {
		return new MethodCall("SkCanvasDouble", methodName, args);
	}

}