package org.eclipse.swt.tests.skia;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

class TextAreaPosition {
	private ImageData data;
	Rectangle backgroundArea;
	Rectangle foregroundArea;
	int bgColor = -1;
	int fgColor = -1;
	int canvasBackground;

	public TextAreaPosition(ImageData data1) {
		this.data = data1;
		calculate();
	}

	private void calculate() {

		canvasBackground = data.getPixel(0, 0);

		int leftColorStarts = -1;
		int rightColorEnds = -1;
		int topColorStarts = -1;
		int bottomColorEnds = -1;

		int firstNotCanvasBackgroundTop = -1;
		int firstNotCanvasBackgroundLeft = -1;
		int lastNotCanvasBackgroundRight = -1;
		int lastNotCanvasBackgroundBottom = -1;

		for (int x = 0; x < Math.min(data.width, 500); x++) {
			for (int y = 0; y < Math.min(data.height, 500); y++) {
				var pix1 = data.getPixel(x, y);

				if (pix1 != canvasBackground) {
					if (firstNotCanvasBackgroundTop == -1) {
						firstNotCanvasBackgroundTop = y;
					}
					if (firstNotCanvasBackgroundLeft == -1) {
						firstNotCanvasBackgroundLeft = x;
					}
					lastNotCanvasBackgroundRight = x;
					lastNotCanvasBackgroundBottom = y;
				}

				if (x == firstNotCanvasBackgroundLeft && y == firstNotCanvasBackgroundTop) {
					// this is the background color of the text
					bgColor = pix1;
				}

			}
		}

		backgroundArea = new Rectangle(firstNotCanvasBackgroundLeft, firstNotCanvasBackgroundTop,
				lastNotCanvasBackgroundRight - firstNotCanvasBackgroundLeft,
				lastNotCanvasBackgroundBottom - firstNotCanvasBackgroundTop);

		String state1 = "CHECK_FOR_FOREGROUND_START";
		String state2 = "CHECK_FOR_FOREGROOUND_END";
		String currentState = state1;

		for (int y = firstNotCanvasBackgroundTop; y <= lastNotCanvasBackgroundBottom; y++) {
			boolean lineIsCompleteBackground = true;
			for (int x = firstNotCanvasBackgroundLeft; x <= lastNotCanvasBackgroundRight; x++) {

				var pix1 = data.getPixel(x, y);
				if (pix1 != bgColor) {
					lineIsCompleteBackground = false;
					break;
				}

			}

			if (currentState.equals(state1)) {
				if (lineIsCompleteBackground)
					topColorStarts = y + 1;
				else
					currentState = state2;
			} else {
				if (lineIsCompleteBackground) {
					bottomColorEnds = y - 1;
					break;
				} else {
					continue;
				}
			}

		}

		if (topColorStarts == -1) {
			topColorStarts = firstNotCanvasBackgroundTop;
		}
		if (bottomColorEnds == -1) {
			bottomColorEnds = lastNotCanvasBackgroundBottom;
		}

		currentState = state1;
		for (int x = firstNotCanvasBackgroundLeft; x <= lastNotCanvasBackgroundRight; x++) {
			boolean lineIsCompleteBackground = true;
			for (int y = firstNotCanvasBackgroundTop; y <= lastNotCanvasBackgroundBottom; y++) {

				var pix1 = data.getPixel(x, y);
				if (pix1 != bgColor) {
					lineIsCompleteBackground = false;
					break;
				}

			}

			if (currentState.equals(state1)) {
				if (lineIsCompleteBackground)
					leftColorStarts = x + 1;
				else
					currentState = state2;
			} else {
				if (lineIsCompleteBackground) {
					rightColorEnds = x - 1;
					break;
				} else {
					continue;
				}
			}

		}

		if (leftColorStarts == -1) {
			leftColorStarts = firstNotCanvasBackgroundLeft;
		}
		if (rightColorEnds == -1) {
			rightColorEnds = lastNotCanvasBackgroundRight;
		}

		foregroundArea = new Rectangle(leftColorStarts, topColorStarts, rightColorEnds - leftColorStarts,
				bottomColorEnds - topColorStarts);

		if (leftColorStarts > 0 && topColorStarts > 0 && leftColorStarts < data.width && topColorStarts < data.height )
			fgColor = data.getPixel(leftColorStarts, topColorStarts);

	}
}