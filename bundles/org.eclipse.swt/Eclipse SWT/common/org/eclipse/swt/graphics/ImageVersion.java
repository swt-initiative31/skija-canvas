package org.eclipse.swt.graphics;

/**
 * @noreference This class is not intended to be referenced by clients.
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
