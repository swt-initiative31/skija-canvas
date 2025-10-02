package org.eclipse.swt.widgets;

public class RasterImageInfo {

	public RasterImageInfo(boolean premule, RasterColorType colorType) {
		super();
		this.premule = premule;
		this.colorType = colorType;
	}

	public RasterImageInfo(boolean premule, RasterColorType colorType, boolean transform) {
		super();
		this.premule = premule;
		this.colorType = colorType;
		this.transform = transform;
	}

	public boolean premule;
	public RasterColorType colorType;
	public boolean transform;

	public static enum RasterColorType{
		RGBA_8888, BGRA_8888;
	}

}
