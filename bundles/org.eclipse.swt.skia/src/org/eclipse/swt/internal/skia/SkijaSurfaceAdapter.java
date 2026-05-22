package org.eclipse.swt.internal.skia;

import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.IRect;

/**
 * Adapter to wrap Skija's Surface as ISkSurface.
 */
class SkijaSurfaceAdapter implements ISkSurface {
	private final Surface delegate;

	public SkijaSurfaceAdapter(Surface surface) {
		this.delegate = surface;
	}

	@Override
	public int getWidth() {
		return delegate.getWidth();
	}

	@Override
	public int getHeight() {
		return delegate.getHeight();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public ISkCanvas getCanvas() {
		// For now, return the raw Skija Canvas. Later, wrap in ISkCanvas if needed.
		return new SkijaCanvasAdapter(delegate.getCanvas());
	}

	@Override
	public Image makeImageSnapshot() {
		return delegate.makeImageSnapshot();
	}

	@Override
	public Image makeImageSnapshot(IRect rect) {
		return delegate.makeImageSnapshot(rect);
	}

	@Override
	public ISkSurface makeSurface(int width, int height) {
		final Surface s = delegate.makeSurface(width, height);
		return s != null ? new SkijaSurfaceAdapter(s) : null;
	}

	@Override
	public void close() {
		delegate.close();
	}

}