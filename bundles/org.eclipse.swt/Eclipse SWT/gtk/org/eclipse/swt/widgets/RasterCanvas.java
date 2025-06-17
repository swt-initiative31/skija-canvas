package org.eclipse.swt.widgets;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.cairo.*;
import org.eclipse.swt.widgets.RasterImageInfo.*;

abstract class RasterCanvas extends Canvas implements Listener, IExternalCanvas{

	final static int OVERLAY_PIXEL = 100;
	final static boolean DYNAMIC_FPS = true;
	int fps = 1000;  // value between 1 and 1000 possible

	private Point currentAreaSize = null;
	private boolean surfaceIsEmpty = true;


	private long cairoSurface;
	private long pointer;
	private long lastRedraw;

	public RasterCanvas(Composite parent, int style) {
		super(parent, style);
		addListener(SWT.Resize	, this);
	}

	@Override
	public void handleEvent(Event e) {

		if(e.type == SWT.Resize) {

			var s = getSize();
			if (currentAreaSize != null && currentAreaSize.x > s.x && currentAreaSize.y > s.y)
				return;

			preResize(e);
			setupSurface();
			createSurface(pointer, currentAreaSize, new RasterImageInfo(false, RasterColorType.ARBG32));
		}


	}


	@Override
	public Object paint (PaintEventSender p,long widget, long cairo) {

		if (System.currentTimeMillis() - lastRedraw > (1000 / fps) || surfaceIsEmpty) {
			long paintStartTime = System.currentTimeMillis();
			doPaint(p);
			long paintingTime = System.currentTimeMillis() - paintStartTime;

			if (DYNAMIC_FPS) {

				// Reduce FPS dependent on the necessary painting time in order to prevent a main thread block.
				// time limits and fps are heuristic values.

				if (paintingTime < 10)
					fps = 60;
				else if (paintingTime < 50)
					fps = 30;
				else if (paintingTime < 50)
					fps = 30;
				else if (paintingTime < 150)
					fps = 10;
				else
					fps = 1;
			}

			surfaceIsEmpty = false;
			pushToCanvas(cairo);
			lastRedraw = System.currentTimeMillis();
		} else {
			pushToCanvas(cairo);
			redraw();
		}

		return null;

	}

	private void pushToCanvas(long cairo) {

		Cairo.cairo_set_source_surface(cairo, cairoSurface, 0, 0);
		Cairo.cairo_paint(cairo);
		Cairo.cairo_surface_flush(cairoSurface);
		Cairo.cairo_surface_flush(cairo);

	}

	void setupSurface() {


		if (this.pointer != 0) {
			Cairo.cairo_surface_destroy(cairoSurface);
			cairoSurface = 0;
			C.free(this.pointer);
			this.pointer = 0;
		}

		var s = getSize();

		currentAreaSize = new Point(s.x + OVERLAY_PIXEL, s.y + OVERLAY_PIXEL);

		int width = currentAreaSize.x;
		int height = currentAreaSize.y;

		int size = width * height * 4;
		this.pointer = C.malloc(size);

		cairoSurface = Cairo.cairo_image_surface_create_for_data(this.pointer, Cairo.CAIRO_FORMAT_ARGB32, width, height,
				4 * width);
	}

	abstract void preResize(Event e) ;

	abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender p);

}
