package org.eclipse.swt.tests.doubles;

import org.eclipse.swt.internal.skia.ISkCanvas;
import org.eclipse.swt.internal.skia.ISkImage;
import org.eclipse.swt.internal.skia.ISkSurface;

import io.github.humbleui.types.IRect;

public class SkSurfaceDouble implements ISkSurface {

	int width;
	int height;
	boolean closed;
	ISkCanvas canvas;

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public ISkCanvas getCanvas() {
		return canvas;
	}

	@Override
	public void close() {
		closed = true;
	}

	@Override
	public ISkImage makeImageSnapshot() {
		// Dummy implementation: return null or a mock if needed
		return null;
	}

	@Override
	public ISkImage makeImageSnapshot(IRect rect) {
		// Dummy implementation: return null or a mock if needed
		return null;
	}

	@Override
	public ISkSurface makeSurface(int width, int height) {
		SkSurfaceDouble surface = new SkSurfaceDouble();
		surface.width = width;
		surface.height = height;
		return surface;
	}

}