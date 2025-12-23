package org.eclipse.swt.examples.skia;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.SkiaGlCanvas;

public class SnippetSkiaDirectDrawing {

	public static void main(String[] arg) {


		final Display d = new Display();
		final Shell s = new Shell(d);

		final Canvas c = new SkiaGlCanvas(s, SWT.DOUBLE_BUFFERED | SWT.FILL );

		s.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				c.setSize(s.getSize());

			}

			@Override
			public void controlMoved(ControlEvent e) {
				// TODO Auto-generated method stub

			}
		});

		s.open();

		while (!s.isDisposed()) {
			d.readAndDispatch();
			c.redraw();
		}

		d.close();

	}

}