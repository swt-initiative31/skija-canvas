package org.eclipse.swt.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;

public class SnippetSkiaDirectDrawing {

	public static void main(String[] arg) {

		SkiaRasterCanvasExtension.SKIA_TEST_PERFORMANCE = true;

		final Display d = new Display();
		final Shell s = new Shell(d);

		final Canvas c = new SkiaRasterCanvas(s, SWT.DOUBLE_BUFFERED | SWT.FILL );

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