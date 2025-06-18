package org.eclipse.swt.widgets;

import org.eclipse.swt.*;

public class SnippetShell {

	public static void main (String [] args) {
		Display display = new Display ();
		Shell shell = new Shell(display);
		shell.setText("Snippet 1");

		SkiaRasterCanvas src = new SkiaRasterCanvas(shell, 0);

		shell.addListener(SWT.Resize, e -> onResize(e, src));

		shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}

	private static void onResize(Event e,Canvas c) {


		var ca = c.getShell().getClientArea();

		c.setSize(ca.width, ca.height);


	}


}
