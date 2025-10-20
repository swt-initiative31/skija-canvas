package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

public class SnippetCanvasCaret {

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display);
		shell.setText("SnippetCanvasCaret");
		shell.setLayout(new GridLayout(3, true));

		final Canvas c1 = new Canvas(shell, SWT.FILL);
		c1.setBackground(display.getSystemColor(SWT.COLOR_GREEN));

		final Canvas c2 = new SkiaRasterCanvas(shell, SWT.FILL);
		c2.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
		//		c2.setForeground(display.getSystemColor(SWT.COLOR_BLACK));

		final Canvas c3 = new SkiaGlCanvas(shell, SWT.FILL);
		c3.setBackground(display.getSystemColor(SWT.COLOR_CYAN));

		setGridLayout(c1, c2, c3);

		setCaret(c1);
		setCaret(c2);
		setCaret(c3);

		drawText(c1);
		drawText(c2);
		drawText(c3);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static void setGridLayout(Canvas... canvases) {

		final var g = new GridData();
		g.grabExcessHorizontalSpace = true;
		g.grabExcessVerticalSpace = true;
		g.horizontalAlignment = GridData.FILL;
		g.verticalAlignment = GridData.FILL;
		g.minimumHeight = 150;
		for (final var c : canvases) {

			c.setLayoutData(g);

		}

	}

	private static void drawText(Canvas... canvases) {

		for (final var c : canvases) {
			c.addPaintListener(
					e -> e.gc.drawText("This is a dummy text, which should somehow work with a caret", 5, 10));
		}

	}

	private static void setCaret(Canvas... canvases) {
		for (final var c : canvases) {
			final Caret caret = new Caret(c, SWT.NONE);
			caret.setBounds(10, 10, 2, 32);
			caret.setFocus();
		}

	}
}