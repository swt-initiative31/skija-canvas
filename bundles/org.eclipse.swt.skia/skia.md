# Architecture Description: Skia Plugin and CanvasExtension

## Skia Plugin Architecture (`org.eclipse.swt.skia`)

The Skia plugin org.elcipse.swt.skia extends SWT with hardware-accelerated 2D graphics using the Skia library. The central class is `SkiaGlCanvasExtension`, which inherits from `OpenGLCanvasExtension` and implements the interfaces `ISkiaCanvasExtension` and `IExternalCanvasHandler`. It encapsulates the integration of Skia with OpenGL and SWT Canvas.

**Main Components:**
- `SkiaGlCanvasExtension`: Bridge between SWT Canvas and Skia, manages rendering, redraw logic, and resource management.
- `ISkiaCanvasExtension`: Interface for Skia-specific canvas extensions (e.g., access to Surface, transformations, resources).
- `SkiaResources`: Encapsulates font, image, and color management for Skia, including caching and DPI handling.
- `SkiaCaretHandler`: Separate class for drawing the caret (cursor) on Skia surfaces.

**Integration:**
- The Skia extension is used as an implementation of `OpenGLCanvasExtension` and can be passed as a `Consumer<Event>` to paint methods.
- Redraw and paint events are managed via functional interfaces (`Consumer<Event>`), enabling flexible and modern Java integration.

---

## CanvasExtension Architecture (`org.eclipse.swt`)

CanvasExtension is an abstract extension model for SWT Canvas, supporting various backends (OpenGL, Skia, Raster). The central class is `OpenGLCanvasExtension`, which inherits from `GLCanvasExtension`.

**Main Components:**
- `OpenGLCanvasExtension`: Abstract base class for OpenGL-based canvas extensions, defines methods like paint, doPaint, preResize, createSurface.
- `GLCanvasExtension`: Base for OpenGL canvas, encapsulates context and pixel format management.
- Functional interfaces: The paint/doPaint methods accept `Consumer<Event>` instead of non-API interfaces, ensuring API compliance and flexibility.

**Interaction with Skia:**
- `SkiaGlCanvasExtension` inherits from `OpenGLCanvasExtension` and implements Skia-specific logic.
- Paint events are passed as `Consumer<Event>`, allowing easy integration of Skia and other backends.

---

## Design Decisions
- **Functional Interfaces:** Using `Consumer<Event>` instead of custom interfaces (like PaintEventSender) ensures API compliance and modern Java integration (lambdas, method references).
- **Resource Management:** `SkiaResources` and `DpiScaler` provide efficient and DPI-independent handling of fonts, images, and colors.
- **Redraw Management:** `RedrawCommand` and flexible paint event logic enable targeted and performant redrawing.
- **Modularity:** The architecture separates backend-specific logic (Skia, OpenGL, Raster) and allows easy extensibility.

---

## Summary
The Skia plugin and CanvasExtension form a flexible, modern, and API-compliant architecture for hardware-accelerated and DPI-independent canvas rendering in SWT. Integration is achieved via abstract and functional interfaces, allowing various backends to be easily integrated and extended.
