package org.eclipse.swt.widgets;

import java.util.*;
import java.util.concurrent.locks.*;

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.cairo.*;

import io.github.humbleui.skija.*;

public class SkiaRasterCanvas extends Canvas{

public SkiaRasterCanvas(Composite parent, int style) {
	super(parent, style);

	// TODO make sure this works really really reliable, take care of the memory
	// handling!!
	this.addControlListener(new ControlAdapter() {

		@Override
		public void controlResized(ControlEvent e) {

			lock.lock();

			setupSurface(e);

			lock.unlock();

		}

	});

}


	long cairoSurface;
	Surface surface;

	static long startTime = System.currentTimeMillis();
	static long lastStart = System.currentTimeMillis();

	static int draws = 0;

	final Lock lock = new ReentrantLock();

	Map<Long, Long> cairoSurfaceCleanup = new HashMap<>();

	protected long bytesPointer;
	protected Pixmap p;
	long pointer;

	void setupSurface(ControlEvent e) {


		if (surface != null && !surface.isClosed())
			surface.close();

		if (p != null && !p.isClosed())
			p.close();

		if (this.pointer != 0) {
			Cairo.cairo_surface_destroy(cairoSurface);
			cairoSurface = 0;
			C.free(this.pointer);
			this.pointer = 0;
		}

			var s = this.getSize();
			int width = s.x;
			int height = s.y;

			int size = width * height * 4;
			this.pointer = C.malloc(size);

			var info = ImageInfo.makeN32(width, height, ColorAlphaType.UNPREMUL);
	//this.buffer = ByteBuffer.allocateDirect(size);
			this.p = Pixmap.make(info, this.pointer, 4 * width);
			surface = Surface.makeRasterDirect(p);

			System.out.println("CairoSurface: create " + System.currentTimeMillis());
			cairoSurface = Cairo.cairo_image_surface_create_for_data(this.pointer, Cairo.CAIRO_FORMAT_ARGB32, width, height,
					4 * width);
			cleanupOldCairoSurface();
	}

	private void cleanupOldCairoSurface() {

		long current = System.currentTimeMillis();

		java.util.List<Long> remove = new ArrayList<>();

		for (var e : cairoSurfaceCleanup.entrySet()) {

			if (current - e.getKey() > 100) {
				Cairo.cairo_destroy(e.getKey());

				remove.add(e.getKey());

			}

		}

		remove.forEach(e -> cairoSurfaceCleanup.remove(remove));

	}

	@Override
	long gtk_draw (long widget, long cairo) {


		surface.getCanvas().clear(0xFFFFFFFF);

		GCData data = new GCData ();
		data.cairo = cairo;
		data.device = getDisplay();

		var b = getBounds();


		Event event = new Event ();
		event.count = 1;
		Rectangle eventBounds = new Rectangle (0, 0, b.width, b.height);
		event.setBounds (eventBounds);

		SkiaGC gc = new SkiaGC(this, surface);
		gc.setClipping (0, 0, b.width, b.height);
		gc.handle = cairoSurface;
		gc.device = data.device;
		gc.data = data;
		gc.surface = surface;

		event.gc = gc;

		sendEvent (SWT.Paint, event);

		Cairo.cairo_set_source_surface(cairo, cairoSurface, 0, 0);
		Cairo.cairo_paint(cairo);
		Cairo.cairo_surface_flush(cairoSurface);
		Cairo.cairo_surface_flush(cairo);


		gc.dispose ();
		event.gc = null;
		return 0;
	}

	public static GC gtk_new(Drawable drawable, GCData data) {

		GC	gc = new GC();

		SkiaRasterCanvas src = (SkiaRasterCanvas) drawable;
		gc.handle = src.cairoSurface;
		gc.device = data.device;
		gc.data = data;
		return gc;
	}

}
