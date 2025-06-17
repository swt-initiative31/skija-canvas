package org.eclipse.swt.widgets;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.widgets.RasterImageInfo.*;

abstract class RasterCanvas extends Canvas implements Listener, IExternalCanvas {

	final static int OVERLAY_PIXEL = 20;
	final static boolean DYNAMIC_FPS = true;
	int fps = 1000;  // value between 1 and 1000 possible

	private long hwnd;
	private long memHdc;
	private long hBitmap;
	private long memoryPointer;
	private boolean triggerRedraw;

	private long lastRedraw;

	private Point currentAreaSize = null;
	private boolean surfaceIsEmpty = true;

	public RasterCanvas(Composite parent, int style) {
		super(parent, style);
		addListener(SWT.Resize, this);
	}

	@Override
	public void handleEvent(Event e) {
		if (e.type == SWT.Resize) {

			var s = getSize();
			if (currentAreaSize != null && currentAreaSize.x > s.x && currentAreaSize.y > s.y)
				return;

			this.hwnd = handle;
			surfaceIsEmpty = true;
			preResize(e);
			setupSurface();
			createSurface(this.memoryPointer, currentAreaSize, new RasterImageInfo(true, RasterColorType.ARBG32));
			redraw();
		}
	}

	@Override
	public void paint(PaintEventSender s,long wParam, long lParam) {
		// This is a complete override of the paint logic

		if (isDisposed())
			return;

		if ((System.currentTimeMillis() - lastRedraw > (1000 / fps) || surfaceIsEmpty) && false) {
			long paintStartTime = System.currentTimeMillis();
			doPaint(s);
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
			pushImageToCanvas();
			lastRedraw = System.currentTimeMillis();
			super.redraw();
		} else {
			doPaint(s);
			pushImageToCanvas();
		}
		if (triggerRedraw) {
			triggerRedraw = false;
			super.redraw();
		}

		super.redraw();

	}

	@Override
	public void redraw() {
		triggerRedraw = true;
		super.redraw();
	}

	void setupSurface() {
		closeDC();
		setupMemoryDC();
	}

	void closeDC() {
		if (memHdc != 0)
			OS.DeleteDC(memHdc);
		if (hBitmap != 0)
			OS.DeleteObject(hBitmap);
	}

	void setupMemoryDC() {

		System.out.println("SetupDC");

		Point s = getSize();

		int width = s.x + OVERLAY_PIXEL;
		int height = s.y + OVERLAY_PIXEL;

		currentAreaSize = new Point(width, height);

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
		this.memoryPointer = pBits[0];

	}

	public void pushImageToCanvas() {
		Point size = currentAreaSize;
		PAINTSTRUCT ps = new PAINTSTRUCT();
		var hDC = OS.BeginPaint(hwnd, ps);
		OS.BitBlt(hDC, 0, 0, size.x, size.y, memHdc, 0, 0, OS.SRCCOPY);
		OS.EndPaint(hwnd, ps);
	}

	abstract void preResize(Event e);

	abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender s);

}
