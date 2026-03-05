package org.eclipse.swt.examples.skia;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

public class SnippetImageToPNG {

	final static boolean DELETE_OUTPUT_DIR = true;

	private static final String IMAGES_PATH = "images";
	private static final int IMAGE_SPACING = 10;
	private static final int CANVAS_WIDTH = 300;
	private static final int CANVAS_HEIGHT = 600;
	private final static ArrayList<Image> swtImages = new ArrayList<Image>();

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Compare Images: GC vs Skija");
		shell.setLayout(new GridLayout(2, true));

		Composite leftComposite = new Composite(shell, SWT.BORDER);
		leftComposite.setLayout(new FillLayout());
		leftComposite.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
		Canvas gcCanvas = new Canvas(leftComposite, SWT.V_SCROLL | SWT.DOUBLE_BUFFERED);
		gcCanvas.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
		ScrollBar gcScrollBar = gcCanvas.getVerticalBar();

		Composite rightComposite = new Composite(shell, SWT.BORDER);
		rightComposite.setLayout(new FillLayout());
		rightComposite.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
		Canvas skiaCanvas = new Canvas(rightComposite, SWT.V_SCROLL | SWT.SKIA);
		skiaCanvas.setLayoutData(new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, true));
		ScrollBar skiaScrollBar = skiaCanvas.getVerticalBar();

		// Load images
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

		if (DELETE_OUTPUT_DIR) {
			for (File oldFile : f.listFiles()) {
				oldFile.delete();
			}
			f.delete();

		}
	}
}
