package org.eclipse.swt.widgets;

import io.github.humbleui.skija.*;

public class SnippetShell {

	public static void main(String[] arg) {
		System.out.println("Test");

		ImageInfo i = ImageInfo.makeN32(300, 300, ColorAlphaType.UNPREMUL);
		try(Surface s = Surface.makeRaster(i)){

		}

	}


}
