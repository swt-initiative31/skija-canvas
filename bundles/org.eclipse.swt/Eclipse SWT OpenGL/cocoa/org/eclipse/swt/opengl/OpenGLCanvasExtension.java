package org.eclipse.swt.opengl;

import java.util.function.*;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class OpenGLCanvasExtension {

	public OpenGLCanvasExtension(Canvas canvas, GLData data) {
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void redrawTriggered() {
	}

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	abstract public void preResize(Event e);

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract void createSurface(long pointer, Point size, RasterImageInfo info);

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract void doPaint(Consumer<Event> paintEventSender);

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public Object paint(Consumer<Event> consumer, long wParam, long lParam) {
		return null;
	}

	public void setCurrent() {

	}

}
