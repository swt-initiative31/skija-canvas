package org.eclipse.swt.tests.skia.skijamocks;

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;

public class PaintData {
    public int color;
    public float strokeWidth;
    public PaintStrokeCap strokeCap;
    public PaintStrokeJoin strokeJoin;
    public BlendMode blendMode;
    public int alpha;
    public boolean antiAlias;

    public static PaintData extractData(Paint p) {
        PaintData data = new PaintData();
        data.color = p.getColor();
        data.strokeWidth = p.getStrokeWidth();
        data.strokeCap = p.getStrokeCap();
        data.strokeJoin = p.getStrokeJoin();
        data.blendMode = p.getBlendMode();
        data.alpha = p.getAlpha();
        data.antiAlias = p.isAntiAlias();
        return data;
    }
}