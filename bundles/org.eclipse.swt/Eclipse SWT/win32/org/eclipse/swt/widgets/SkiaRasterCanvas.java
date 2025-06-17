package org.eclipse.swt.widgets;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.win32.*;

import io.github.humbleui.skija.*;

public class SkiaRasterCanvas extends Canvas {

	public Surface surface;

	// DC memory position
	public long memHdc;
	public long hwnd;
	// surface pointing to memDC
	private long hBitmap;

	private boolean triggerRedraw;

	public SkiaRasterCanvas(Composite parent, int style) {
		super(parent, style);

		addListener(SWT.Resize, e -> onResize(e));
	}


	private void onResize(Event e) {
		System.out.println("Setup DC");
		hwnd = handle;
		closeDC();
		setupMemoryDC();
		redraw();
	}

	@Override
	LRESULT WM_PAINT(long wParam, long lParam) {
		/* Process WM_PAINT */

		// This is a complete override of the paint logic

		if (isDisposed())
			return null;

		surface.getCanvas().clear(0xFFFFFFFF);
		doPaint();

		var r = OS.DefWindowProc(hwnd, (int) OS.WM_PAINT, wParam, lParam);

		LRESULT l = new LRESULT(r);
		if(triggerRedraw)
		{
			super.redraw();
			triggerRedraw = false;
		}
		return l;
	}

	protected void doPaint() {

		if (surface == null)
			return;

		if (false) {
		} else {

			Point size = getSize();

			PAINTSTRUCT ps = new PAINTSTRUCT();
//			long hdc = OS.BeginPaint(hwnd, ps);

			GCData data = new GCData ();
			data.hwnd = hwnd;
			data.ps = ps;
			data.font = getFont();
			data.device = getDisplay();

			GC gc = new_GC(data);

			long hdc = gc.handle;


			Event event = new Event ();
			event.gc = gc;
			event.setBounds(new Rectangle( 0,0,size.x,size.y));
			sendEvent (SWT.Paint, event);
			// widget could be disposed at this point
			event.gc = null;


			OS.BitBlt(hdc, 0, 0, size.x, size.y, memHdc, 0, 0, OS.SRCCOPY);

			gc.dispose ();
//			OS.EndPaint(hwnd, ps);
		}

	}

	@Override
	public void redraw () {

		triggerRedraw = true;
		super.redraw();

	}

	private void closeDC() {

		if (surface != null && !surface.isClosed())
			surface.close();

		if (memHdc != 0)
			OS.DeleteDC(memHdc);
		if (hBitmap != 0)
			OS.DeleteObject(hBitmap);
	}

	private void setupMemoryDC() {

		System.out.println("SetupDC");

		Point s = getSize();

		int width = s.x;
		int height = s.y;

		/* Create resources */
		long hdc = OS.GetDC(hwnd);
		memHdc = OS.CreateCompatibleDC(hdc);
		BITMAPINFOHEADER bmiHeader = new BITMAPINFOHEADER();
		bmiHeader.biSize = BITMAPINFOHEADER.sizeof;
		bmiHeader.biWidth = width;
		bmiHeader.biHeight = -height;
		bmiHeader.biPlanes = 1;
		bmiHeader.biBitCount = 32;
		bmiHeader.biCompression = OS.BI_RGB;
		var bmi = new byte[BITMAPINFOHEADER.sizeof];
		OS.MoveMemory(bmi, bmiHeader, BITMAPINFOHEADER.sizeof);
		long[] pBits = new long[1];
		hBitmap = OS.CreateDIBSection(hdc, bmi, OS.DIB_RGB_COLORS, pBits, 0, 0);
		if (hBitmap == 0)
			SWT.error(SWT.ERROR_NO_HANDLES);

		OS.SelectObject(memHdc, hBitmap);

		surface = Surface.makeRasterDirect(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL), pBits[0],
				4 * width);

		// // Gib den HDC zurück
		OS.ReleaseDC(hwnd, hdc);

		return;
	}

	long startTime = System.currentTimeMillis();

	private void drawSurface() {

		surface.getCanvas().clear(0xFFFFFFFF);

		Point size = getSize();

		long currentPosTime = System.currentTimeMillis() - startTime;

		currentPosTime = currentPosTime % 10000;

		double position = (double) currentPosTime / (double) 10000;

		int colorAsRGB = 0xFF42FFF4;
		int colorRed = 0xFFFF0000;
		int colorGreen = 0xFF00FF00;
		int colorBlue = 0xFF0000FF;

		try (var paint = new Paint()) {
			paint.setColor(colorBlue);
			surface.getCanvas().drawCircle((int) (position * size.x), 100, 100, paint);
		}
	}

	@Override
	GC new_GC(GCData data) {
		return GC.skiagc_new(this, data);
	}

}
