package org.eclipse.swt.tests.skia;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.internal.graphics.SkiaGC;
import org.eclipse.swt.internal.graphics.SkiaTextDrawing;
import org.eclipse.swt.internal.skia.DpiScalerUtil;
import org.eclipse.swt.internal.skia.ISkiaCanvasExtension;
import org.eclipse.swt.internal.skia.SkiaResources;
import org.eclipse.swt.tests.skia.skijamocks.DrawCommand;
import org.eclipse.swt.tests.skia.skijamocks.DrawStringCommand;
import org.eclipse.swt.tests.skia.skijamocks.TestSurface;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;

public class Test_org_eclipse_swt_skia_SkiaTextDrawing {

	@Test
	public void testDrawText_NoCache_100x() {

		SkiaTextDrawing.USE_TEXT_CACHE = false;

		final Display display = Display.getDefault();

		final Shell s = new Shell(display);

		final Canvas c = new Canvas(s, 0);

		try (Surface surface = Surface.makeRaster(
				new ImageInfo(100, 100, io.github.humbleui.skija.ColorType.RGBA_8888, ColorAlphaType.PREMUL))) {

			final TestSurface testSurface = new TestSurface(surface);

			final AtomicReference<SkiaResources> resourcesRef = new AtomicReference<>();

			final var ext = new ISkiaCanvasExtension() {

				@Override
				public SkiaResources getResources() {
					return resourcesRef.get();
				}

				@Override
				public Surface getSurface() {
					return testSurface;
				}

				@Override
				public Surface createSupportSurface(int width, int height) {
					return surface.makeSurface(width, height);
				}

				@Override
				public DpiScalerUtil getScaler() {
					return new DpiScalerUtil(() -> 100);
				}
			};
			final SkiaResources resources = new SkiaResources(c, ext);
			resourcesRef.set(resources);

			final SkiaGC gc = new SkiaGC(c, ext, 0);

			SkiaTextDrawing.drawText(gc, new String[] { "Test1", "Test2" }, 0, 0, 0);

			s.dispose();

			final var executed = testSurface.getTestCanvas().executedCommands;

			assertEquals(2, executed.size());
			final var command1 = executed.get(0);
			final var command2 = executed.get(1);

			assertDrawCommand(command1, "Test1", 0, 13);
			assertDrawCommand(command2, "Test2", 0, 30);

		}

	}

	private static void assertDrawCommand(DrawCommand command1, String string, float x, float y) {

		if (!(command1 instanceof final DrawStringCommand dsc)) {
			throw new AssertionError("Expected DrawStringCommand but got " + command1.getClass().getName());
		}
		assertEquals(string, dsc.stringToDraw);
		assertEquals(x, dsc.x);
		assertEquals(y, dsc.y);

	}

}
