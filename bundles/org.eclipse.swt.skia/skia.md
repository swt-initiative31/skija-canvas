# Architecture: Skia Canvas Feature (`org.eclipse.swt.skia`)

## Overview

The `org.eclipse.swt.skia` plugin extends SWT with hardware-accelerated 2D rendering using the
[Skija](https://github.com/HumbleUI/Skija) Java bindings for the [Skia](https://skia.org/) graphics
library. It integrates transparently into the existing SWT drawing lifecycle and provides with SWT.SKIA a switch between the native rendering backend for canvases and the Skia drawing framework. 

This feature is currently only supported for windows and linux and it requires OpenGL support.

The plugin org.eclipse.swt.skia is structured into four Java packages:

| Package | Purpose |
|---|---|
| `org.eclipse.swt.internal.skia` | Core extension classes: rendering pipeline, resource management, caret handling |
| `org.eclipse.swt.internal.skia.cache` | Cache key records for image, text-image, and text-split caches (`ImageKey`, `ImageTextKey`, `SplitsTextCache`) |
| `org.eclipse.swt.internal.graphics` | GC implementation (`SkiaGC`), paint management (`SkiaPaintManager`), text drawing (`SkiaTextDrawing`), image conversion (`SwtToSkiaImageConverter`, `SkijaToSwtImageConverter`, `RGBAEncoder`), color conversion (`SkiaColorConverter`), path conversion (`SkiaPathConverter`), transform conversion (`SkiaTransformConverter`), region calculation (`SkiaRegionCalculator`), font metrics (`SkiaFontMetrics`) |
| `org.eclipse.swt.internal.canvasext` | Factory class (`SkiaCanvasFactory`) that plugs the Skia backend into the SWT canvas extension framework |

---

## Modification in the existing binary plugins of SWT

To support Skia drawing, only minimal changes were made to the SWT binary fragments. Importantly, these fragments do not depend directly on any Skia resources. Instead, they introduce a lightweight, independent extension mechanism: painting commands are delegated to external handlers (such as the Skia fragment) using the Java ServiceLoader (see `ExternalCanvasHandler`).

If the Skia fragment is not present, SWT automatically falls back to the classic rendering path. When a `Canvas` is instantiated with the `SWT.SKIA` style, the `externalCanvasHandler` field is initialized (if available), and all paint method calls are routed through this handler.

To enable a GC API for Skia, the `final` modifier on `GC` was replaced with `sealed`, allowing only the new `GCExtension` subclass (added in the SWT fragments). `GCExtension` simply delegates all `GC` method calls to an internal delegate. The same approach is used for `FontMetrics` with `FontMetricsExtension`.

In order to use Skia, a connection with OpenGL must be established. For this the `GLCanvasExtension` was created, which is a duplication of the `GLCanvas`, just without extending the class hierarchy of `Canvas`. In order to send a Paint event with OpenGL, the `GLPaintEventInvoker` provides the necessary delegation mechanism. 

There are also other small modifications to enable Skia drawing. But these are no modifications to the SWT API.

The following packages were added or modified in the SWT binary fragments:

| Package | Purpose |
|---|---|
| `org.eclipse.swt.internal.canvasext` | Extension framework: `ExternalCanvasHandler`, `IExternalCanvasFactory`, `IExternalCanvasHandler`, `IExternalGC`, `IExternalFontMetrics`, `DpiScaler`, `FontProperties` |
| `org.eclipse.swt.opengl` | OpenGL integration: `GLCanvasExtension`, `GLPaintEventInvoker` |
| `org.eclipse.swt.graphics` | Extension subclasses: `GCExtension`, `FontMetricsExtension` |

---

### Skia Handler Implementation

The Skia backend provides its handler via the `SkiaCanvasFactory` class, which implements `IExternalCanvasFactory` and is registered as a service in the OSGi bundle via:

```
resources/META-INF/services/org.eclipse.swt.internal.canvasext.IExternalCanvasFactory
```

with the content:

```
org.eclipse.swt.internal.canvasext.SkiaCanvasFactory
```

This enables the `ServiceLoader` to discover the Skia factory at runtime.

### Handler Lifecycle and Resource Management

The handler instance (e.g., `SkiaGlCanvasExtension`) is responsible for creating and managing the base Skia resources (contexts, surfaces) and connecting these to the GL render target.
The handler is attached to the canvas for its lifetime. On canvas disposal, all resources are released via the handler's cleanup logic.

### Error Handling and Fallback

If handler creation fails (e.g., due to missing OpenGL support or Skia initialization errors), the error is logged and the external handler mechanism is permanently disabled for the process. All subsequent canvases fall back to the default SWT rendering path.

---

## Class Overview

### `SkiaGlCanvasExtension`
**Package:** `org.eclipse.swt.internal.skia`

The central class of the plugin. It connects an SWT `Canvas` widget to a Skia rendering surface
backed by an OpenGL framebuffer.

**Inheritance:**
```
GLCanvasExtension
  └── GLPaintEventInvoker
        └── SkiaGlCanvasExtension  (implements ISkiaCanvasExtension, IExternalCanvasHandler)
```

**Responsibilities:**
- Creates and manages the Skia `DirectContext` (OpenGL-backed Skia GPU context).
- Creates and re-creates the `BackendRenderTarget` and `Surface` on canvas resize.
- Maintains a `lastImage` snapshot after each frame so that unchanged areas can be restored
  cheaply without re-executing paint listeners.
- Drives the paint event loop by constructing a `SkiaGC`, wrapping it in a `GCExtension`, and
  sending a paint `Event` to all registered SWT paint listeners.

**Key fields:**
- `skijaContext` — Skia `DirectContext` for OpenGL, created once at construction time.
- `renderTarget` / `surface` — recreated on each canvas resize; `surface` is the drawable Skia canvas.
- `lastImage` — snapshot of the last fully rendered frame, used to restore unchanged areas on partial redraws.
- `redrawCommands` — list of pending redraw areas accumulated between paint cycles; `null` area means full repaint.
- `resources` — shared `SkiaResources` instance for fonts, images, and colors.
- `scaler` — `DpiScaler` for automatic HiDPI coordinate scaling.

---

### `GLCanvasExtension`
**Package:** `org.eclipse.swt.opengl`

Duplicates the functionality of `GLCanvas` but without extending the `Canvas` widget hierarchy.
It manages the OpenGL context, pixel format setup, and provides methods like `setCurrent()`,
`swapBuffers()`, `isCurrent()`, and `getGLData()`. Disposal of the OpenGL context is handled
via an `SWT.Dispose` listener on the canvas.

---

### `GLPaintEventInvoker`
**Package:** `org.eclipse.swt.opengl`

Extends `GLCanvasExtension` and provides the mechanism to invoke paint events within an OpenGL
context. It receives a `Consumer<Event>` (the paint event sender), calls the abstract `doPaint()`
method (implemented by subclasses like `SkiaGlCanvasExtension`), and then calls `swapBuffers()`.

It also manages a `redrawTriggered` flag to schedule additional redraws when needed.

---

### `ISkiaCanvasExtension`
**Package:** `org.eclipse.swt.internal.skia`

Interface implemented by `SkiaGlCanvasExtension` and used by `SkiaGC`. It provides the GC with
access to Skia-specific services without exposing the full extension class.

**Methods:**
- `getSurface()` — returns the current Skia `Surface` for drawing.
- `getResources()` — returns the shared `SkiaResources` instance.
- `createSupportSurface(int w, int h)` — creates a temporary off-screen GPU surface (used for
  cached text rendering).
- `getScaler()` — returns the `DpiScaler` for logical-to-physical coordinate conversion.

---

### `SkiaGC`
**Package:** `org.eclipse.swt.internal.graphics`

Implements `IExternalGC` — the SWT-internal interface that maps all GC drawing calls to a
non-native backend. `SkiaGC` translates every SWT drawing operation into Skija canvas calls.
Drawing operations (stroke and fill) are delegated to `SkiaPaintManager`, which configures
the Skija `Paint` object with color, alpha, line style, pattern shaders, and XOR blend mode.

**Canvas state management (save/restore stack):**

Skija's `Canvas` uses a state stack. Each `save()` pushes the current clip and transform state;
`restore()` pops it. `SkiaGC` manages this stack as follows:
- On construction: `canvas.save()` captures the baseline state (`initialSaveCount`).
- On `dispose()`: `canvas.restoreToCount(initialSaveCount)` undoes all state changes made during
  the lifetime of this GC, including all clipping and transform layers.
- Each `setClipping(...)` call first calls `canvas.restore()` to remove any previous clip layer,
  then calls `canvas.save()` followed by the new clip call to push a fresh clip layer.
- `setTransform(...)` calls `canvas.save()` after applying the matrix so that subsequent clip
  operations are stacked on top of the transform independently.

**SWT-to-Skija coordinate mapping:**
- All SWT coordinates are in logical (device-independent) pixels.
- `DpiScaler.autoScaleUp()` converts logical pixel values to physical pixels before every Skija
  draw call.
- `createScaledRectangle(x, y, w, h)` scales a bounding rectangle and converts it to
  `io.github.humbleui.types.Rect`.
- `getScaledOffsetValue()` computes a 0.5-pixel sub-pixel offset applied to stroke operations
  to prevent anti-aliasing blur when drawing on integer pixel boundaries with odd-width strokes.

**Image handling:**
- SWT `Image` objects are converted to Skija `Image` objects on demand via
  `SwtToSkiaImageConverter.convertSWTImageToSkijaImage()`.
- Converted images are cached in `SkiaResources` by `(SWT Image identity, version, zoom)` key.
  The version field invalidates the cache entry when a GC is created on the SWT image, then we expect an image modification.
- The pixel format of the SWT image is detected and mapped to the corresponding Skija `ColorType`.
  Unsupported or ambiguous formats fall back to a full RGBA conversion via `RGBAEncoder`.

---

### `SkiaPaintManager`
**Package:** `org.eclipse.swt.internal.graphics`

Manages the creation and configuration of Skija `Paint` objects for all drawing operations in
`SkiaGC`. Provides two main entry points:

- `performDraw(Consumer<Paint>)` — configures a `Paint` for stroke operations (line width, line
  cap, line style / dash pattern, foreground color, foreground pattern shader, XOR blend mode).
- `performDrawFilled(Consumer<Paint>)` — configures a `Paint` for fill operations (background
  color, background pattern shader, XOR blend mode).

**Pattern handling:**
- `convertSWTPatternToSkijaShader(Pattern)` converts an SWT `Pattern` to a Skija `Shader`:
  - Gradient patterns become `Shader.makeLinearGradient(...)`.
  - Image patterns are converted via `SwtToSkiaImageConverter` and become `image.makeShader(FilterTileMode.REPEAT)`.

---

### `SkiaTextDrawing`
**Package:** `org.eclipse.swt.internal.graphics`

Static utility class responsible for drawing text strings onto the Skia surface. Supports two
modes controlled by the `USE_TEXT_CACHE` flag:
- `drawTextBlobWithCache` — renders text into a cached off-screen image and reuses it for
  identical text/font/color combinations (default, enabled).
- `drawTextBlobNoCache` — renders text directly onto the surface without caching (debug mode).

Handles multi-line text (split by delimiters), transparent backgrounds, and anti-aliasing settings.

---

### `SwtToSkiaImageConverter`
**Package:** `org.eclipse.swt.internal.graphics`

Converts SWT `Image` objects to Skija `Image` objects. Uses `SkiaResources` for caching: if a
cached Skija image already exists for the given SWT image identity, version, and zoom level, it
is returned directly. Otherwise, the SWT `ImageData` pixel buffer is extracted and mapped to the
appropriate Skija `ColorType`.

---

### `SkijaToSwtImageConverter`
**Package:** `org.eclipse.swt.internal.graphics`

Converts Skija `Image` objects back to SWT `Image` objects (used when SWT code needs an SWT
image from Skia-rendered content).

---

### `SkiaColorConverter`
**Package:** `org.eclipse.swt.internal.graphics`

Utility class for converting between SWT `Color` values and Skija integer color representations.
Provides methods for standard conversion, conversion with alpha, and color inversion.

---

### `SkiaPathConverter`
**Package:** `org.eclipse.swt.internal.graphics`

Converts SWT `Path` objects to Skija `Path` objects, applying DPI scaling to all coordinates.

---

### `SkiaTransformConverter`
**Package:** `org.eclipse.swt.internal.graphics`

Converts SWT `Transform` objects to Skija `Matrix33` representations for canvas transformations.

---

### `SkiaRegionCalculator`
**Package:** `org.eclipse.swt.internal.graphics`

Implements `AutoCloseable`. Calculates and manages Skia clip regions from SWT `Region` objects.

---

### `RGBAEncoder`
**Package:** `org.eclipse.swt.internal.graphics`

Fallback pixel format converter. When the SWT image pixel format cannot be directly mapped to a
Skija `ColorType`, this class performs a full RGBA conversion of the pixel data.

---

### `SkiaResources`
**Package:** `org.eclipse.swt.internal.skia`

Centralizes all shared Skia resources for a single canvas widget. One instance is created per
`SkiaGlCanvasExtension` and is shared with the `SkiaGC` created for each paint event.

**Responsibilities:**
- **Font management:** Converts SWT `Font` objects to Skija `Font` objects and caches them by
  `FontProperties` (family name, weight, height, italic flag). Font family resolution uses a
  best-fit scoring algorithm against all font families registered in the system `FontMgr`.
  If no match is found, the system default font is used as a fallback. Unresolvable names are
  added to the `unknownFonts` set to avoid repeated lookup overhead.
- **Image cache:** LRU cache (`LruImageCache`) of Skija `Image` objects keyed by
  `ImageKey(SWT Image identity, version, zoom level)`. Maximum capacity: 256 entries.
  Evicted images are automatically closed.
- **Text image cache:** LRU cache of pre-rendered text images keyed by
  `ImageTextKey(text, font properties, transparency flag, background color, foreground color, antialias)`.
  Maximum capacity: 512 entries. Used by `SkiaTextDrawing.drawTextBlobWithCache`.
- **Text split cache:** Caches pre-processed text split arrays (`SplitsTextCache` key) covering
  tab expansion, delimiter splitting, and mnemonic stripping to avoid repeated string processing.
- **Color management:** Stores the current foreground and background `Color` for the GC.
  `resetBaseColors()` is called on `SkiaGC.dispose()` to clear the references and avoid
  retaining disposed colors.
- **Cleanup:** All cached Skija resources are explicitly closed in `resetResources()`, which
  is registered as both an `SWT.Dispose` and `SWT.ZoomChanged` listener on the canvas widget.

---

### `SkiaCaretHandler`
**Package:** `org.eclipse.swt.internal.skia`

Static utility class for drawing the text caret on top of the Skia-rendered content. Uses
`BlendMode.DIFFERENCE` to ensure visibility against any background color.

**Note:** Caret rendering is currently **disabled** in `SkiaGlCanvasExtension.doPaint()` (the
call to `SkiaCaretHandler.handleCaret()` is commented out). Full caret support requires
additional modifications to `Canvas` and `Caret`.

---

### `SkiaCanvasFactory`
**Package:** `org.eclipse.swt.internal.canvasext`

Implements `IExternalCanvasFactory`. This is the OSGi service registered via the service loader
to plug the Skia backend into the SWT canvas extension framework.

When an SWT `Canvas` with the `SWT.SKIA` style is created, the framework calls
`createCanvasExtension(Canvas)` on this factory. If Skia initialization fails (for example,
because the platform does not support OpenGL), the error is logged via `Logger.logException()`
and Skia is permanently disabled for the current process (`skiaFailedWithErrors = true`), so
that subsequent canvas creations do not repeatedly attempt and fail to initialize Skia.

---

### `SkiaFontMetrics`
**Package:** `org.eclipse.swt.internal.graphics`

Wraps a Skija `FontMetrics` object and implements `IExternalFontMetrics`. Provides ascent, descent,
height, leading, and average character width values to the SWT `FontMetrics` API via
`FontMetricsExtension`. All values are scaled down from physical to logical pixels via `DpiScaler`.

---

## Rendering Pipeline

```
SWT widget calls canvas.redraw()
         │
         ▼
SkiaGlCanvasExtension.redrawTriggered()
  └── adds RedrawCommand to queue, delegates to GLPaintEventInvoker
         │
         ▼
GLPaintEventInvoker.paint(consumer, wParam, lParam)
  ├── calls doPaint(consumer) → dispatched to SkiaGlCanvasExtension
  ├── calls swapBuffers()
  └── if redrawTriggered flag set → canvas.redraw() (schedule next frame)
         │
         ▼
SkiaGlCanvasExtension.doPaint(paintEventSender)
  ├── Determine dirty area (union of all pending RedrawCommands)
  │     └── null area → full repaint
  ├── If partial repaint: draw lastImage onto surface (restore unchanged content)
  ├── surface.getCanvas().save() + clipRect(dirtyArea)
  ├── surface.getCanvas().clear(backgroundColor)
  ├── executePaintEvents(paintEventSender, dirtyArea)
  │     ├── create SkiaGC (saves initial canvas state)
  │     ├── wrap in GCExtension, attach to Event
  │     └── invoke paintEventSender → SWT paint listeners fire
  │           └── listeners call gc.drawXxx() / gc.fillXxx()
  │                 └── SkiaPaintManager.performDraw() / performDrawFilled()
  │                       ├── configure Paint (color, style, shader)
  │                       └── Skija Canvas.drawXxx() calls
  ├── SkiaGC.dispose() → canvas.restoreToCount(initialSaveCount)
  ├── createLastImageSnapshot() — cache frame for next partial repaint
  └── skijaContext.flush() — submit all buffered GPU commands to OpenGL
```

---

## DPI and Coordinate Scaling

Since Skia requires physical pixels, the conversion from SWT logical pixels is handled
transparently by `DpiScaler` and helper methods in `SkiaGC`:

- `DpiScaler.autoScaleUp(int)` / `DpiScaler.autoScaleUp(float)` — converts a logical pixel value to physical pixels.
- `DpiScaler.autoScaleDown(float)` / `DpiScaler.autoScaleDown(float[])` — converts physical pixel values back to logical pixels.
- `DpiScaler.getZoomedFontSize(int)` — converts a font size from points to zoomed physical pixels.
- `DpiScaler.scaleUpRectangle(Rectangle)` — scales a full rectangle to physical coordinates.

---

## Architecture Decision: `Canvas`, `GCExtension` and `FontMetricsExtension`

In order to minimize API modifications, the only new API is the field in SWT, SKIA. So the classical canvas can be used, just the style SWT.SKIA must be set. This minimizes the modifications, which are necessary for application developers to use the Skia feature.

Only small modifications to the canvas class were necessary.

SWT's `GC` and `FontMetrics` classes are effectively sealed types — they are tightly coupled to
the native platform handles and are not designed for subclassing by external rendering backends.

Rather than refactoring `GC` into a wrapper class with a delegate (which would require
changes across the entire SWT repository), the Skia integration introduces two minimal subclasses:

- **`GCExtension extends GC`**: Overrides all drawing and state methods of `GC` to delegate them
  to an `IExternalGC` implementation (i.e. `SkiaGC`). Native handle access methods throw
  `IllegalStateException` to prevent misuse. `GCExtension` is annotated `@noreference` so that
  it remains an internal implementation detail.

- **`FontMetricsExtension extends FontMetrics`**: Overrides all measurement methods to delegate
  to an `IExternalFontMetrics` implementation (i.e. `SkiaFontMetrics`).

**Rationale:** This approach minimizes the impact on the SWT codebase. No modifications to the
existing SWT widget hierarchy, paint dispatch logic, or public API are required. The Skia backend
plugs in entirely through the `SkiaCanvasFactory` service registration and the two extension
classes. This makes the feature self-contained and easy to enable or disable at the OSGi bundle
level. Further architectural refinements (e.g. a proper wrapper `GC` delegate model) can follow
in a later iteration.

---

## Widgets with Limited or No Support

The Skia backend operates at the `Canvas` level. Higher-level SWT widgets that manage their own
native painting (such as `StyledText`, `Table`, and `Tree`) do not currently use the Skia
rendering path. These widgets paint through native OS controls or through their own internal GC
usage, which bypasses the canvas extension mechanism. `ExternalCanvasHandler` explicitly excludes
`StyledText` and `Decorations` subclasses.

---

## Known Limitations and Open TODOs

- **Mnemonic underlining:** `replaceMnemonics()` strips `&` prefix characters from text but does
  not yet draw an underline beneath the mnemonic character.
- **No Caret support currently** (SkiaCaretHandler exists but is not called)
- **No Skia TextLayout**

---