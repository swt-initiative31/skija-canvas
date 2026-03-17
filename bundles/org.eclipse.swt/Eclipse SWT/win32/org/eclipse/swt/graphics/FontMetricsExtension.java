package org.eclipse.swt.graphics;

import org.eclipse.swt.internal.canvasext.*;
import org.eclipse.swt.internal.win32.*;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
public final class FontMetricsExtension extends FontMetrics {


	/**
	 * On Windows, handle is a Win32 TEXTMETRIC struct
	 * (Warning: This field is platform dependent)
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public TEXTMETRIC handle;

	private IExternalFontMetrics externalMetrics;

	/**
	 * @noreference This constructor is not intended to be referenced by clients.
	 * @param extMetrics
	 */
	public FontMetricsExtension(IExternalFontMetrics extMetrics) {
		this.externalMetrics = extMetrics;
	}

	@Override
	public int getAscent() {
		return externalMetrics.getAscent();
	}

	@Override
	public double getAverageCharacterWidth() {
		return externalMetrics.getAverageCharacterWidth();
	}

	@Override
	@Deprecated
	public int getAverageCharWidth() {
		return externalMetrics.getAverageCharWidth();
	}

	@Override
	public int getDescent() {
		return externalMetrics.getDescent();
	}

	@Override
	public int getHeight() {
		return externalMetrics.getHeight();
	}

	@Override
	public int getLeading() {
		return externalMetrics.getLeading();
	}

	@Override
	public int hashCode() {
		return externalMetrics.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return externalMetrics.equals(object);
	}

}