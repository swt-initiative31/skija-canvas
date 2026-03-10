package org.eclipse.swt.internal.canvasext;

import org.eclipse.swt.graphics.*;

/**
 * This class is used to track the version of an image. Whenever a GC is created on an image, its version will be incremented.
 */
public class ImageVersion {

	private int version;

	public ImageVersion(int version) {
		this.version = version;
	}

	public static ImageVersion getVersion(Image image) {
		return image.getImageVersion();
	}

	public int getVersion() {
		return version;
	}

}
