package org.eclipse.swt.widgets;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

public class SnippetCanvasCompare {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Snippet Canvas Compare");

		{
			Canvas c1 = new Canvas(shell, SWT.DOUBLE_BUFFERED);
			c1.setSize(100, 100);
			shell.addListener(SWT.Resize, e -> onResize(e, c1, 1));
			c1.addListener(SWT.Paint, e -> onPaint(e));
		}
		{
			Canvas c2 = new SkiaRasterCanvas(shell, SWT.DOUBLE_BUFFERED);
			c2.setSize(100, 100);
			shell.addListener(SWT.Resize, e -> onResize(e, c2, 2));
			c2.addListener(SWT.Paint, e -> onPaint(e));
		}
		

		shell.setSize(1000, 1000);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	private static void onPaint(Event e) {

		Display d = e.widget.getDisplay();
		GC gc = e.gc;

		gc.setForeground(d.getSystemColor(SWT.COLOR_RED));
		gc.drawRectangle(new Rectangle(0, 0, 100, 100));

		gc.setForeground(d.getSystemColor(SWT.COLOR_BLUE));
		gc.drawRectangle(new Rectangle(100, 0, 100, 100));

		gc.setForeground(d.getSystemColor(SWT.COLOR_GREEN));
		gc.drawRectangle(new Rectangle(0, 100, 100, 100));

		var img = d.getSystemImage(SWT.ICON_QUESTION);
		gc.drawImage(img, 100, 100);

		gc.setForeground(d.getSystemColor(SWT.COLOR_RED));
		gc.drawLine(200, 100, 100, 200);

		gc.setForeground(d.getSystemColor(SWT.COLOR_CYAN));
		gc.drawFocus(200, 0, 100, 100);

		gc.setForeground(d.getSystemColor(SWT.COLOR_RED));
		gc.drawArc(200, 100, 100, 100, 90, 200);

		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_CYAN));
		gc.drawOval(0, 300, 100, 50);

		Path p = new Path(d);
		p.addRectangle(0, 400, 100, 100);
		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_GREEN));
		gc.drawPath(p);

		gc.setForeground(d.getSystemColor(SWT.COLOR_GRAY));
		gc.drawRoundRectangle(0, 500, 100, 100, 50, 50);

		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_RED));
		gc.drawPoint(5, 600);

		var bo = img.getBounds();
		gc.drawImage(img, 0, 0, bo.width /2 , bo.height /2 ,    20 , 600, bo.width * 2, bo.height *2 );

		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_MAGENTA));
		gc.drawText("Test\nTest2", 300, 0);
		gc.drawText("Test2\nTest3", 300, 100, false);
		gc.drawText("Transparent Text", 300,200, true);


		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_GRAY));
		gc.drawPolygon(new int[] {
				400,2, //
				500,100, //
				400,100, //
				500,2 //
		});


		gc.setForeground(d.getSystemColor(SWT.COLOR_DARK_YELLOW));
		gc.drawPolyline(new int[] {
				400,50, //
				500,100, //
				600,200

		});

		gc.setBackground(d.getSystemColor(SWT.COLOR_DARK_YELLOW));
		gc.fillArc(100, 300, 100, 100, 90, 200);

		gc.setBackground(d.getSystemColor(SWT.COLOR_DARK_GREEN));
		gc.fillGradientRectangle(100, 400, 100, 100, false);

		gc.fillOval(100, 500, 100, 50);


		Path p2 = new Path(d);
		p2.addRectangle(100, 600, 100, 100);
		gc.fillPath(p2);

		gc.fillPolygon(new int[] {
				100,700, //
				200,800, //
				100,800, //
				200,700 //
		});

		gc.fillRectangle(new Rectangle(100, 800, 100, 100));

		gc.fillRoundRectangle(200, 800, 100, 100, 20, 20);
		
	}

	private static void onResize(Event e, Canvas c, int index) {


		var ca = c.getShell().getClientArea();
		if(index == 2) {
			c.setBounds(new Rectangle(ca.width / 2, 0, ca.width / 2, ca.height));
		}
		else {
			c.setBounds(new Rectangle(0, 0, ca.width / 2, ca.height));
		}


	}

}
