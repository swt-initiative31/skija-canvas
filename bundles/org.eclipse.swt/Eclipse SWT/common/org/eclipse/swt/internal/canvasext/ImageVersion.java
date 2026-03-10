package org.eclipse.swt.internal.canvasext;

/**
 * This class is used to track the version of an image. Whenever a GC is created on an image, its version will be incremented.
 */
public class ImageVersion {

	private int version;

	public ImageVersion(int version) {
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

}
