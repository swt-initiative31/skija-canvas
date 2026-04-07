# Architecture: Skia Canvas Feature (`org.eclipse.swt.skia`)

## Overview

The `org.eclipse.swt.skia` plugin extends SWT with hardware-accelerated 2D rendering using the
[Skija](https://github.com/HumbleUI/Skija) Java bindings for the [Skia](https://skia.org/) graphics
library. It integrates transparently into the existing SWT drawing lifecycle and provides a switch between the native rendering backend for canvases and the Skia drawing framework. 

This feature is currently only supported for windows and linux and it requires OpenGL support.

The plugin is structured into three Java packages:

| Package | Purpose |
|---|---|
| `org.eclipse.swt.internal.skia` | Core extension classes: rendering pipeline, resource management, caret handling |
| `org.eclipse.swt.internal.graphics` | GC implementation, image/color conversion, region calculation, text layout |
| `org.eclipse.swt.internal.canvasext` | Factory class that plugs the Skia backend into the SWT canvas extension framework |

---

## Modification in the existing binary plugins of SWT

To support Skia drawing, only minimal changes were made to the SWT binary fragments. Importantly, these fragments do not depend directly on any Skia resources. Instead, they introduce a lightweight, independent extension mechanism: painting commands are delegated to external handlers (such as the Skia fragment) using the Java ServiceLoader (see `ExternalCanvasHandler`).

If the Skia fragment is not present, SWT automatically falls back to the classic rendering path. When a `Canvas` is instantiated with the `SWT.SKIA` style, the `externalCanvasHandler` field is initialized (if available), and all paint method calls are routed through this handler.

To enable a GC API for Skia, the `final` modifier on `GC` was replaced with `sealed`, allowing only the new `GCExtension` subclass (added in the SWT fragments). `GCExtension` simply delegates all `GC` method calls to an internal delegate. The same approach is used for `FontMetrics` and the new `FontMetricsExtension`.

There are also other small modifications to enable Skia drawing. But there are no modifications to the SWT API except the field SWT.SKIA.

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

The handler instance (e.g., `SkiaGlCanvasExtension`) is responsible for:
- Creating and managing the base Skia resources (contexts, surfaces) and connecting these to the gl render target.
- Ensuring these resources are released when the canvas is disposed, using explicit cleanup and Java try-with-resources for all `AutoCloseable` Skija objects.
- Providing the paint and redraw logic for the canvas, including incremental redraw, snapshotting, and caret rendering.

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
  └── OpenGLCanvasExtension
        └── SkiaGlCanvasExtension  (implements ISkiaCanvasExtension, IExternalCanvasHandler)
```

**Responsibilities:**
- Creates and manages the Skia `DirectContext` (OpenGL-backed Skia GPU context).
- Creates and re-creates the `BackendRenderTarget` and `Surface` on canvas resize.
- Implements an incremental redraw strategy: only the union of all pending redraw areas is
  re-rendered per paint cycle.
- Maintains a `lastImage` snapshot after each frame so that unchanged areas can be restored
  cheaply without re-executing paint listeners.
- Drives the paint event loop by constructing a `SkiaGC`, wrapping it in a `GCExtension`, and
  sending a paint `Event` to all registered SWT paint listeners.
- Delegates caret rendering to `SkiaCaretHandler` after the paint event, so the caret is drawn
  on top of the painted content and is not part of the cached frame.

**Key fields:**
- `skijaContext` — Skia `DirectContext` for OpenGL, created once at construction time.
- `renderTarget` / `surface` — recreated on each canvas resize; `surface` is the drawable Skia canvas.
- `lastImage` — snapshot of the last fully rendered frame, used to restore unchanged areas on partial redraws.
- `redrawCommands` — list of pending redraw areas accumulated between paint cycles; `null` area means full repaint.
- `resources` — shared `SkiaResources` instance for fonts, images, and colors.
- `scaler` — `DpiScaler` for automatic HiDPI coordinate scaling.

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
- `getTransformation()` — optional `Matrix33` transform applied to region coordinates; used on
  macOS for the flipped coordinate system. Returns `null` by default.
- `getScaler()` — returns the `DpiScaler` for logical-to-physical coordinate conversion.

---

### `SkiaGC`
**Package:** `org.eclipse.swt.internal.graphics`

Implements `IExternalGC` — the SWT-internal interface that maps all GC drawing calls to a
non-native backend. `SkiaGC` translates every SWT drawing operation into a Skija canvas call.

**Key design principles:**
- All drawing operations are routed through two central methods:
  - `performDraw(Consumer<Paint>)` — for stroke operations (lines, arcs, borders, text).
    Sets up a `Paint` with foreground color, blend mode, line style, line width, stroke cap, and
    optionally a foreground pattern shader. The caller lambda receives the configured `Paint` and
    issues the actual Skija draw call.
  - `performDrawFilled(Consumer<Paint>)` — for fill operations (filled rectangles, ovals, paths).
    Sets up a `Paint` with the background color. If a background pattern is set, the color is
    overridden by the pattern shader. The caller lambda receives the configured `Paint`.

Both methods use try-with-resources to ensure that `Paint` and `Shader` objects are always
closed after use, preventing native memory leaks.

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
  `convertSWTImageToSkijaImage()`.
- Converted images are cached in `SkiaResources` by `(SWT Image identity, version, zoom)` key.
  The version field invalidates the cache entry when a GC is created on the SWT image, then we expect an image modification..
- The pixel format of the SWT image is detected and mapped to the corresponding Skija `ColorType`.
  Unsupported or ambiguous formats fall back to a full RGBA conversion via `RGBAEncoder`.

**Pattern handling:**
- `convertSWTPatternToSkijaShader(Pattern)` converts an SWT `Pattern` to a Skija `Shader`:
  - Gradient patterns become `Shader.makeLinearGradient(...)`.
  - Image patterns become `image.makeShader(FilterTileMode.REPEAT)`.
- The temporary Skija `Image` used to create an image shader is closed immediately after
  `makeShader()` returns, inside a try-with-resources block.
- The returned `Shader` is owned by the caller and must be closed after use, which
  `performDraw` and `performDrawFilled` handle via try-with-resources.

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
- **Image cache:** Caches Skija `Image` objects by `(SWT Image identity, version, zoom level)`.
  The version key ensures cache invalidation when the SWT image content changes.
- **Text image cache:** Caches pre-rendered text images (used by `drawTextBlobWithCache`) keyed
  by `(text, font properties, transparency flag, background color, foreground color, antialias)`.
- **Text split cache:** Caches pre-processed text split arrays (tab expansion, delimiter
  splitting, mnemonic stripping) to avoid repeated string processing.
- **Color management:** Stores the current foreground and background `Color` for the GC.
  `resetBaseColors()` is called on `SkiaGC.dispose()` to clear the references and avoid
  retaining disposed colors.
- **Cleanup:** All cached Skija resources are explicitly closed in `resetResources()`, which
  is registered as an SWT `SWT.Dispose` listener on the canvas widget.

**Font size conversion:**
- Font sizes are converted from SWT logical point units to physical pixels:
  `fontSize = (fontSize * display.getDPI().y) / 72`
- On Win32, the zoom-adjusted font size from `DpiScaler.getZoomedFontSize()` is applied first.

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

### `SkiaRegionCalculator`
**Package:** `org.eclipse.swt.internal.graphics`

Converts an SWT `Region` into a Skija `Region` by replaying the region's operation log.

SWT `Region` objects record their construction history as a sequence of operations (ADD, SUBTRACT,
INTERSECT, TRANSLATE) in a `RegionLog`. `SkiaRegionCalculator` reads this log via
`RegionLogProvider` and applies the equivalent Skija `RegionOp` operations. Polygon-based region
operations (including rectangles, which are converted to polygons internally) are handled by
`PathBuilder` and set on the Skija region via `setPath()`. The calculated region is cached so it
is only computed once per `SkiaRegionCalculator` instance.

---

### `SkiaFontMetrics`
**Package:** `org.eclipse.swt.internal.graphics`

Wraps a Skija `FontMetrics` object and implements `IExternalFontMetrics`. Provides ascent, descent,
height, leading, and average character width values to the SWT `FontMetrics` API via
`FontMetricsExtension`.

---

## Rendering Pipeline

```
SWT widget calls canvas.redraw()
         │
         ▼
SkiaGlCanvasExtension.redrawTriggered()
  └── adds RedrawCommand to queue, schedules paint
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
  │                 └── SkiaGC.performDraw() / performDrawFilled()
  │                       ├── configure Paint (color, style, shader)
  │                       └── Skija Canvas.drawXxx() calls
  ├── SkiaGC.dispose() → canvas.restoreToCount(initialSaveCount)
  ├── createLastImageSnapshot() — cache frame for next partial repaint
  ├── SkiaCaretHandler.handleCaret() — draw caret on top of painted content
  └── skijaContext.flush() — submit all buffered GPU commands to OpenGL
```

---

## Canvas Save/Restore Strategy

Skija's `Canvas` uses a state stack similar to the HTML5 Canvas or PDF graphics state. Each
`save()` pushes the current clip region and transformation matrix; `restore()` pops it.

`SkiaGC` uses this stack as follows:

| Call site | Operation | Purpose |
|---|---|---|
| `SkiaGC()` constructor | `save()` | Captures the baseline canvas state for this GC instance |
| `SkiaGC.dispose()` | `restoreToCount(initialSaveCount)` | Removes all state layers pushed during this GC's lifetime, including clips and transforms |
| `setTransform(...)` | `save()` | Pushes a new layer so clips applied after the transform can be independently undone |
| `setClipping(Path)` | `restore()` + `save()` | Pops the previous clip layer, then pushes a new layer for the path clip |
| `setClipping(Rectangle)` | `save()` only | Rectangle clips do not need to pop a previous layer because the clip is combined by intersection |
| `setClipping(Region)` | `restore()` + `save()` | Pops the previous clip layer, then pushes a new layer for the region clip |
| `copyArea(..., paint=true)` | `save()` + `restore()` | Temporarily clips to the source area to clear it, then immediately restores |

---

## Resource Management

All Skija objects that implement `AutoCloseable` (e.g. `Paint`, `Shader`, `Path`, `PathBuilder`,
`Surface`, `Image`) are managed with Java try-with-resources to ensure native memory is released
promptly:

| Resource | Lifecycle |
|---|---|
| `Paint` | Created per `performDraw()` / `performDrawFilled()` call; always closed on exit |
| `Shader` | Created on demand when a pattern is active; closed inside the same try block as `Paint` |
| `Path` / `PathBuilder` | Created per draw/fill path or polygon call; always closed |
| `Surface` (support surface) | Created for each cached text rendering call; always closed |
| `Image` (snapshots in `copyArea`) | Always closed with try-with-resources |
| Cached `Image` (in `SkiaResources`) | Closed explicitly in `resetResources()` on canvas dispose |
| Cached `Font` (in `SkiaResources`) | Closed explicitly in `resetResources()` on canvas dispose |

---

## DPI and Coordinate Scaling

All SWT APIs operate in logical (device-independent) pixels. Skija requires physical pixels.
The conversion is handled transparently by `DpiScaler` and helper methods in `SkiaGC`:

- `DpiScaler.autoScaleUp(int)` — converts a logical pixel value to physical pixels.
- `DpiScaler.autoScaleDown(int)` — converts a physical pixel value back to logical pixels.

---

## Architecture Decision: `Canvas`, `GCExtension` and `FontMetricsExtension`

In order to minimize API modifications, the only new API is the field in SWT, SKIA. So the classical canvas can be used, just the style SWT.SKIA must be set. This minimizes the modifications, which are necessary for application developers to use the Skia feature.

Only small modifications to the canvas class were necessary.

SWT's `GC` and `FontMetrics` classes are effectively sealed types — they are tightly coupled to
the native platform handles and are not designed for subclassing by external rendering backends.

Rather than refactoring `GC` into an wrapper class with a delegate (which would require
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
usage, which bypasses the canvas extension mechanism.

---

## Known Limitations and Open TODOs

- **Mnemonic underlining:** `replaceMnemonics()` strips `&` prefix characters from text but does
  not yet draw an underline beneath the mnemonic character.
- **`USE_TEXT_CACHE`:** The text image cache (`drawTextBlobWithCache`) is currently **enabled**
  (`USE_TEXT_CACHE = true`). Set to `false` to disable caching and use direct string rendering,
  which can be useful for debugging rendering artefacts.
- **`logImageNullError`:** The first null-image error per JVM process is logged via
  `Logger.logException()`. Subsequent occurrences are suppressed (`logImageNullError = false`)
  to avoid log flooding.

---