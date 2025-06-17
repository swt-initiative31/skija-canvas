package org.eclipse.swt.opengl;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

public abstract class OpenGLCanvas extends GLCanvas implements IExternalCanvas {

	private boolean redrawTriggered;

	public OpenGLCanvas(Composite parent, int style, GLData data) {
		super(parent, style, data);
	}

	@Override
	public Object paint(PaintEventSender e, long wParam, long lParam) {

		if (isDisposed())
			return null;
		this.redrawTriggered = false;
		doPaint(e);
		swapBuffers();

		if(redrawTriggered) {
			super.redraw();
			return 0;
		}

		return null;

	}

	@Override
	public void redraw() {
		this.redrawTriggered = true;
		super.redraw();
	}

	abstract public void preResize(Event e);

	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender e);

}
