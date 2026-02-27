package org.eclipse.swt.graphics;

public interface IExternalFontMetrics {

	public int getAscent();

	public double getAverageCharacterWidth();

	@Deprecated
	public int getAverageCharWidth();

	public int getDescent();

	public int getHeight();

	public int getLeading();

}