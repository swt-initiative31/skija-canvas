package org.eclipse.swt.internal.skia.cache;

import java.util.Objects;

import org.eclipse.swt.internal.canvasext.FontProperties;

public class ImageTextKey {
	public final String text;
	public final FontProperties fontProperties;
	public final boolean transparent;
	public final int background;
	public final int foreground;

	public ImageTextKey(String text, FontProperties fontProperties, boolean transparent, int background, int foreground) {
		this.text = text;
		this.fontProperties = fontProperties;
		this.transparent = transparent;
		this.background = background;
		this.foreground = foreground;
	}

	@Override
	public int hashCode() {

		if(transparent) {
			return Objects.hash(fontProperties, foreground, text, transparent);
		}
		return Objects.hash(background, fontProperties, foreground, text);
	}

	@Override
	public String toString() {
		return "ImageTextKey [text=" + text + ", fontProperties=" + fontProperties + ", transparent=" + transparent
				+ ", background=" + background + ", foreground=" + foreground + "]";
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
		final ImageTextKey other = (ImageTextKey) obj;

		if(transparent ) {
			return  Objects.equals(fontProperties, other.fontProperties)
					&& foreground == other.foreground && Objects.equals(text, other.text) &&
					other.transparent;
		}

		return background == other.background && Objects.equals(fontProperties, other.fontProperties)
				&& foreground == other.foreground && Objects.equals(text, other.text);
	}



}