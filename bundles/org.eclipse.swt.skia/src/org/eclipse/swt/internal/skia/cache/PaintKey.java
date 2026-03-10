package org.eclipse.swt.internal.skia.cache;

public class PaintKey {
	// Basic paint attributes
	private int color;
	private int alpha;
	private int style; // e.g. 0=FILL, 1=STROKE, 2=STROKE_AND_FILL
	private float strokeWidth;
	private float strokeMiter;
	private int strokeCap; // 0=Butt, 1=Round, 2=Square
	private int strokeJoin; // 0=Miter, 1=Round, 2=Bevel
	private int blendMode;
	private int filterQuality;
	// Complex ojects as reference or key
	private Object shader;
	private Object pathEffect;
	private Object colorFilter;
	private Object maskFilter;

	public PaintKey() {
	}

	public void setColor(int color) {
		this.color = color;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	public void setStyle(int style) {
		this.style = style;
	}

	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
	}

	public void setStrokeMiter(float strokeMiter) {
		this.strokeMiter = strokeMiter;
	}

	public void setStrokeCap(int strokeCap) {
		this.strokeCap = strokeCap;
	}

	public void setStrokeJoin(int strokeJoin) {
		this.strokeJoin = strokeJoin;
	}

	public void setBlendMode(int blendMode) {
		this.blendMode = blendMode;
	}

	public void setFilterQuality(int filterQuality) {
		this.filterQuality = filterQuality;
	}

	public void setShader(Object shader) {
		this.shader = shader;
	}

	public void setPathEffect(Object pathEffect) {
		this.pathEffect = pathEffect;
	}

	public void setColorFilter(Object colorFilter) {
		this.colorFilter = colorFilter;
	}

	public void setMaskFilter(Object maskFilter) {
		this.maskFilter = maskFilter;
	}

	// Accessors for all fields
	public int getColor() {
		return color;
	}

	public int getAlpha() {
		return alpha;
	}

	public int getStyle() {
		return style;
	}

	public float getStrokeWidth() {
		return strokeWidth;
	}

	public float getStrokeMiter() {
		return strokeMiter;
	}

	public int getStrokeCap() {
		return strokeCap;
	}

	public int getStrokeJoin() {
		return strokeJoin;
	}

	public int getBlendMode() {
		return blendMode;
	}

	public int getFilterQuality() {
		return filterQuality;
	}

	public Object getShader() {
		return shader;
	}

	public Object getPathEffect() {
		return pathEffect;
	}

	public Object getColorFilter() {
		return colorFilter;
	}

	public Object getMaskFilter() {
		return maskFilter;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PaintKey paintKey = (PaintKey) o;
		if (color != paintKey.color) {
			return false;
		}
		if (alpha != paintKey.alpha) {
			return false;
		}
		if (style != paintKey.style) {
			return false;
		}
		if (Float.compare(paintKey.strokeWidth, strokeWidth) != 0) {
			return false;
		}
		if (Float.compare(paintKey.strokeMiter, strokeMiter) != 0) {
			return false;
		}
		if (strokeCap != paintKey.strokeCap) {
			return false;
		}
		if (strokeJoin != paintKey.strokeJoin) {
			return false;
		}
		if (blendMode != paintKey.blendMode) {
			return false;
		}
		if (filterQuality != paintKey.filterQuality) {
			return false;
		}
		if (shader != null ? !shader.equals(paintKey.shader) : paintKey.shader != null) {
			return false;
		}
		if (pathEffect != null ? !pathEffect.equals(paintKey.pathEffect) : paintKey.pathEffect != null) {
			return false;
		}
		if (colorFilter != null ? !colorFilter.equals(paintKey.colorFilter) : paintKey.colorFilter != null) {
			return false;
		}
		return maskFilter != null ? maskFilter.equals(paintKey.maskFilter) : paintKey.maskFilter == null;
	}

	@Override
	public int hashCode() {
		int result = color;
		result = 31 * result + alpha;
		result = 31 * result + style;
		result = 31 * result + Float.floatToIntBits(strokeWidth);
		result = 31 * result + Float.floatToIntBits(strokeMiter);
		result = 31 * result + strokeCap;
		result = 31 * result + strokeJoin;
		result = 31 * result + blendMode;
		result = 31 * result + filterQuality;
		result = 31 * result + (shader != null ? shader.hashCode() : 0);
		result = 31 * result + (pathEffect != null ? pathEffect.hashCode() : 0);
		result = 31 * result + (colorFilter != null ? colorFilter.hashCode() : 0);
		result = 31 * result + (maskFilter != null ? maskFilter.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "PaintKey{" + "color=" + color + ", alpha=" + alpha + ", style=" + style + ", strokeWidth=" + strokeWidth
				+ ", strokeMiter=" + strokeMiter + ", strokeCap=" + strokeCap + ", strokeJoin=" + strokeJoin
				+ ", blendMode=" + blendMode + ", filterQuality=" + filterQuality + ", shader=" + shader
				+ ", pathEffect=" + pathEffect + ", colorFilter=" + colorFilter + ", maskFilter=" + maskFilter + '}';
	}

}