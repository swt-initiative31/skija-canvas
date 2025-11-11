package org.eclipse.swt.graphics;

import java.util.HashMap;

import org.eclipse.swt.graphics.RegionLog.OpType;
import org.eclipse.swt.graphics.RegionLog.Operation;
import org.eclipse.swt.widgets.ISkiaCanvas;

import io.github.humbleui.skija.Region.Op;
import io.github.humbleui.types.IRect;

public class SkiaRegionCalculator {

    final static HashMap<OpType, Op> operationMapping = new HashMap<OpType, Op>();

    static {
	operationMapping.put(OpType.ADD, Op.UNION);
	operationMapping.put(OpType.SUBTRACT, Op.DIFFERENCE);
	operationMapping.put(OpType.INTERSECT, Op.INTERSECT);
    }

    private final org.eclipse.swt.graphics.Region region;
    private io.github.humbleui.skija.Region calculatedRegion;
    private final ISkiaCanvas skiaExtension;

    public SkiaRegionCalculator(org.eclipse.swt.graphics.Region r, ISkiaCanvas skiaExtension) {
	this.region = r;
	this.skiaExtension = skiaExtension;
    }

    io.github.humbleui.skija.Region getSkiaRegion() {

	if (calculatedRegion != null) {
	    return calculatedRegion;
	}

	final io.github.humbleui.skija.Region reg = new io.github.humbleui.skija.Region();

	final RegionLogProvider prov = new RegionLogProvider(region);
	for (final var o : prov.getLog().getOperations()) {

	    apply(o, reg);

	}

	calculatedRegion = reg;

	return calculatedRegion;

    }

    private void apply(Operation o, io.github.humbleui.skija.Region reg) {
	final Op skiaOperation = operationMapping.get(o.type());
	if (skiaOperation != null) {
	    executeOperation(skiaOperation, o.executionObject(), reg);
	    return;
	}

	if (OpType.TRANSLATE.equals(o.type())) {
	    if (o.executionObject() instanceof final Point p) {

		if (this.skiaExtension != null && this.skiaExtension.getTransformation() != null) {
		    // in this case we only expect the standard transformation for mac
		    reg.translate(p.x, -p.y);
		} else {
		    reg.translate(p.x, p.y);
		}

		return;
	    }
	}

	throw new IllegalStateException("Unknown type and object: " + o);

    }

    private void executeOperation(Op skiaOperation, Object ob, io.github.humbleui.skija.Region reg) {

	// polygon
	if (ob instanceof final int[] polygon) {
	    final var tempReg = createPolygonSkiaRegion(polygon);
	    reg.op(tempReg, skiaOperation);
	} else if (ob instanceof final Rectangle rec) {
	    final var irect = createIRect(rec);
	    reg.op(irect, skiaOperation);
	} else if (ob instanceof final Region otherReg) {
	    final SkiaRegionCalculator src = new SkiaRegionCalculator(otherReg, skiaExtension);
	    reg.op(src.getSkiaRegion(), skiaOperation);
	}

    }

    private io.github.humbleui.skija.Region createIRect(Rectangle rec) {
	final var rect = new IRect(rec.x, rec.y, rec.x + rec.width, rec.y + rec.height);

	return createPolygonSkiaRegion(new int[] { rect.getLeft(), rect.getTop(), rect.getRight(), rect.getTop(),
		rect.getRight(), rect.getBottom(), rect.getLeft(), rect.getBottom() });

    }

    private io.github.humbleui.skija.Region createPolygonSkiaRegion(int[] polygon) {

	final io.github.humbleui.skija.Region r = new io.github.humbleui.skija.Region();

	final var p = new io.github.humbleui.skija.Path();
	p.addPoly(toFloat(polygon), true);

	final Point maxV = getMax(polygon);

	if (this.skiaExtension != null && this.skiaExtension.getTransformation() != null) {
	    p.transform(this.skiaExtension.getTransformation());
	    final int width = this.skiaExtension.getSurface().getWidth();
	    final int height = this.skiaExtension.getSurface().getHeight();
	    r.setRect(new IRect(0, 0, width, height));
	} else {
	    r.setRect(new IRect(0, 0, maxV.x, maxV.y));
	}

	r.setPath(p, r);

	return r;
    }

    private static Point getMax(int[] polygon) {

	final var p = new Point(0, 0);

	for (int i = 0; i < polygon.length; i++) {

	    if (i % 2 == 0) {
		p.x = Math.max(polygon[i], p.x);
	    }
	    if (i % 2 == 1) {
		p.y = Math.max(polygon[i], p.y);
	    }

	}

	return p;
    }

    private static float[] toFloat(int[] arr) {

	final float[] r = new float[arr.length];

	for (int i = 0; i < arr.length; i++) {
	    r[i] = arr[i];
	}

	return r;

    }

}
