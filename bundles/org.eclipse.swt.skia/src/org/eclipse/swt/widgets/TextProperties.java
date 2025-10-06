package org.eclipse.swt.widgets;

import java.util.Objects;

// TODO if transparent, then the background color does not matter...

class TextProperties {

	String text;
	float fontSize;
	String fontName;
	boolean transparent;
	int background;
	int foreground;

	long lastCall;

	public TextProperties(String text, float fontSize, String fontName, boolean isTransparent, int background,
			int foreground) {
		super();
		this.text = text;
		this.fontSize = fontSize;
		this.fontName = fontName;
		this.transparent = isTransparent;
		this.background = background;
		this.foreground = foreground;
	}

	@Override
	public int hashCode() {
		return Objects.hash(background, fontName, fontSize, foreground, transparent, text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TextProperties other = (TextProperties) obj;
		return background == other.background && Objects.equals(fontName, other.fontName)
				&& Float.floatToIntBits(fontSize) == Float.floatToIntBits(other.fontSize)
				&& foreground == other.foreground && transparent == other.transparent
				&& Objects.equals(text, other.text);
	}


}
