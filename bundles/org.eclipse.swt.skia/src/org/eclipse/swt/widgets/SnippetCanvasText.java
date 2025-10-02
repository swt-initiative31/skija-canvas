package org.eclipse.swt.widgets;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

public class SnippetCanvasText {

	final static int LETTERS_PER_LINE = 80;
	final static int LINES = 30;

	static String[] text = new String[LINES];

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText("Snippet Canvas");
		// here you can switch between Canvas SkiaRasterCanvas and SkiaCanvas
		SkiaConfiguration.activateSkiaGl();
		final Canvas c = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		SkiaConfiguration.resetCanvasConfiguration();

		final StringBuilder b = new StringBuilder();

		for (int j = 0; j < LINES; j++) {
			text[j] = generateText(LETTERS_PER_LINE);
		}

		c.setSize(100, 100);

		shell.addListener(SWT.Resize, e -> onResize(e, c));
		c.addListener(SWT.Paint, SnippetCanvasText::onPaint);
		c.addListener(SWT.Paint, SnippetCanvasText::onPaint2);

		shell.setSize(1000, LETTERS_PER_LINE * 3 + 80);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	static long startTime = System.currentTimeMillis();
	private static boolean printFrameRate = true;
	private static int frames;
	private static long lastFrame;
	private static long framesToDraw;

	private static void onPaint(Event e) {


		final var s = ((Canvas) e.widget);

		//		surface.getCanvas().clear(0xFFFFFFFF);

		final Point size = s.getSize();

		long currentPosTime = System.currentTimeMillis() - startTime;

		currentPosTime = currentPosTime % 10000;

		final double position = (double) currentPosTime / (double) 10000;

		final int shift = (int) (position * size.x);
		final int shiftDown = 20;


		for(int j = 0 ; j < LINES; j++) {
			e.gc.drawText(text[j], shift, shiftDown + 20 * j);
		}


		//		int colorAsRGB = 0xFF42FFF4;
		//		int colorRed = 0xFFFF0000;
		//		int colorGreen = 0xFF00FF00;
		//		int colorBlue = 0xFF0000FF;
		//
		//		e.gc.setForeground(s.getDisplay().getSystemColor(SWT.COLOR_RED));


	}

	private static void onPaint2(Event e) {

		final var s = ((Canvas) e.widget);

		if (printFrameRate) {

			if (System.currentTimeMillis() - lastFrame > 1000) {
				// System.out.println("Frames: " + frames);
				framesToDraw = frames;

				frames = 0;
				lastFrame = System.currentTimeMillis();
			}
			frames++;

			e.gc.drawText("Frames: " + framesToDraw, 10, 10);

		}

		s.redraw();
		;

	}

	private static void onResize(Event e, Canvas c) {

		final var ca = c.getShell().getClientArea();

		c.setSize(ca.width, ca.height);

	}
	public static String generateText(int textLength) {
		final int leftLimit = 97; // letter 'a'
		final int rightLimit = 122; // letter 'z'
		final Random random = new Random();

		final String generatedString = random.ints(leftLimit, rightLimit + 1)
				.limit(textLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();


		return generatedString;
	}


}
