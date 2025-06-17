package org.eclipse.swt.opengl;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

public abstract class OpenGLCanvas extends GLCanvas implements IExternalCanvas{


	public OpenGLCanvas(Composite parent, int style, GLData data) {
		super(parent, style, data);
	}

	@Override
	public void paint (PaintEventSender e  ,long wParam, long lParam) {

		if (isDisposed())
			return ;
			doPaint(e);
			swapBuffers();
	}

	@Override
	public void redraw() {
		super.redraw();
	}

	abstract public void preResize(Event e) ;

	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	public abstract void doPaint(PaintEventSender e);


}
