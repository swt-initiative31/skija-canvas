package org.eclipse.swt.examples.skia;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

public class SnippetAlpha {

	final static boolean USE_SKIA = false; // Set to true to use Skia rendering, false for default SWT rendering
	
    public static void main(String[] args) {
        final Display display = new Display();
        final Shell shell = new Shell(display);
        shell.setText("Snippet Alpha");
        
        var style = SWT.DOUBLE_BUFFERED   | (USE_SKIA ? SWT.SKIA : SWT.NONE);
        
        final Canvas canvas = new Canvas(shell,style);
        canvas.setSize(400, 400);
        shell.setSize(420, 440);

        canvas.addListener(SWT.Paint, SnippetAlpha::onPaint);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    private static void onPaint(Event e) {
    	
    	e.gc.setAdvanced(true);
    	
    	e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_RED));
    	e.gc.fillRectangle(0, 0, 100, 100);

    	// on windows alpha in colors is not supported, what about linux??
    	Color c = new Color(e.display, new RGBA(0, 0, 255, 50));
    	e.gc.setBackground(c);
    	e.gc.fillRectangle(200, 0, 100, 100);
    	
    }
	
}
