package org.eclipse.swt.examples.skia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

public class SnippetImageToPNG {

	final static boolean DELETE_OUTPUT_DIR = false;
	
	record FormatInfo(String format, int type) {
	}

	private final static List<FormatInfo> formats = List.of(
			new FormatInfo(".bmp", SWT.IMAGE_BMP),
			new FormatInfo(".gif", SWT.IMAGE_GIF),
			new FormatInfo(".ico", SWT.IMAGE_ICO),
			new FormatInfo(".jpef", SWT.IMAGE_JPEG),
			new FormatInfo(".png", SWT.IMAGE_PNG)
	);
	

	private static final String IMAGES_PATH = "images";
	private final static ArrayList<Image> swtImages = new ArrayList<Image>();

	public static void main(String[] args) {
		Display display = new Display();

		// Load images

		swtImages.add(Display.getDefault().getSystemImage(SWT.ICON_QUESTION));
		swtImages.add(Display.getDefault().getSystemImage(SWT.ICON_ERROR));
		swtImages.add(Display.getDefault().getSystemImage(SWT.ICON_INFORMATION));

		List<File> imageFiles = new ArrayList<>();
		// Use working directory reliably
		File imagesDir = new File(System.getProperty("user.dir"), IMAGES_PATH);
		if (imagesDir.exists() && imagesDir.isDirectory()) {
			for (File f : imagesDir.listFiles()) {
				String name = f.getName().toLowerCase();
				if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")
						|| name.endsWith(".bmp") || name.endsWith(".ico")) {
					try {
						swtImages.add(new Image(display, f.getAbsolutePath()));
						imageFiles.add(f);
					} catch (Exception e) {
						// skip invalid image
					}
				}
			}
		}

		int i = 0;
		File f = new File("./out");

		if (f.exists()) {
			for (File oldFile : f.listFiles()) {
				oldFile.delete();
			}
		}
		f.mkdirs();
		
		for(FormatInfo format : formats) {
			for (Image img : swtImages) {
				ImageLoader loader = new ImageLoader();
				loader.data = new ImageData[] { img.getImageData() };
				String outPath = "./out/out" + i + ".png";
				loader.save(outPath, SWT.IMAGE_PNG);
				// Transparenzprüfung
				ImageData pngData = new ImageLoader().load(outPath)[0];
				boolean hasAlpha = pngData.alphaData != null || pngData.alpha != -1;
				System.out.println("PNG: " + outPath + " hasAlpha=" + hasAlpha + " alphaData="
						+ (pngData.alphaData != null ? pngData.alphaData.length : "null") + " alpha=" + pngData.alpha);
				i++;
			}
			
		}


		if (DELETE_OUTPUT_DIR) {
			for (File oldFile : f.listFiles()) {
				oldFile.delete();
			}
			f.delete();

		}
	}
	

	
}
