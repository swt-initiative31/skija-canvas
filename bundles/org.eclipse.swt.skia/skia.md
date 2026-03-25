# Architecture: Skia Canvas Plugin (`org.eclipse.swt.skia`)

## Overview

The `org.eclipse.swt.skia` plugin extends SWT with hardware-accelerated 2D rendering using the
[Skija](https://github.com/HumbleUI/Skija) Java bindings for the [Skia](https://skia.org/) graphics
library. It integrates transparently into the existing SWT canvas extension framework and replaces
the native GDI/Cairo/Quartz rendering backend for canvases that are created with the `SWT.SKIA`
style bit.

The plugin is structured into three Java packages:

| Package | Purpose |
|---|---|
| `org.eclipse.swt.internal.skia` | Core extension classes: rendering pipeline, resource management, caret handling |
| `org.eclipse.swt.internal.graphics` | GC implementation, image/color conversion, region calculation, text layout |
| `org.eclipse.swt.internal.canvasext` | Factory class that plugs the Skia backend into the SWT canvas extension framework |

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
  The version field invalidates the cache entry when the SWT image data changes.
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

### `SkiaCaretHandler`
**Package:** `org.eclipse.swt.internal.skia`

A stateless utility class that draws the SWT caret onto the Skia surface after each paint event.
The caret bounds are retrieved from `Canvas.getCaret()` and drawn as a filled rectangle using
`BlendMode.DIFFERENCE`, which inverts the underlying colors to make the caret visible on any
background color.

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

### `RGBAEncoder`
**Package:** `org.eclipse.swt.internal.graphics`

A utility class that converts an SWT `ImageData` object into a raw RGBA byte array suitable for
use with `io.github.humbleui.skija.Image.makeRasterFromBytes()`. It handles all SWT transparency
types (`TRANSPARENCY_ALPHA`, `TRANSPARENCY_PIXEL`, `TRANSPARENCY_MASK`) for both direct-color and
indexed-color (palette-based) image formats.

---

### `SkiaFontMetrics`
**Package:** `org.eclipse.swt.internal.graphics`

Wraps a Skija `FontMetrics` object and implements `IExternalFontMetrics`. Provides ascent, descent,
height, leading, and average character width values to the SWT `FontMetrics` API via
`FontMetricsExtension`.

---

### `SkiaTextLayout`
**Package:** `org.eclipse.swt.internal.graphics`

An experimental text layout implementation based on the Skija paragraph API (`ParagraphBuilder`,
`Paragraph`). It is not yet active in the main rendering path — `SkiaGC` uses direct Skija font
and string drawing instead. `SkiaTextLayout` provides a foundation for future support of fully
styled text rendering with correct line breaking, tab stops, bidirectional text, and selection
highlighting.

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
- `createScaledRectangle(x, y, w, h)` — scales a bounding rectangle and converts it to
  `io.github.humbleui.types.Rect` for Skija.
- `getScaledOffsetValue()` — returns a 0.5-pixel sub-pixel offset applied to stroke positions
  to avoid anti-aliasing artifacts when drawing with odd-width strokes on integer pixel
  boundaries. Returns `0` for even-width strokes and `0.5` otherwise.

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
- **`SkiaTextLayout`:** Not used in the main rendering path. The primary blocker is an unsolved
  font size mapping between SWT `FontData` and Skija `Font` sizes.
- **`createSkiaFont(Font)` in `SkiaResources`:** Returns `null` and is not yet implemented.
  It is called only from `SkiaTextLayout`, which is itself inactive.
- **Image zoom factor in `drawImage`:** The zoom factor is computed as
  `Math.round(Math.max(destWidth / srcWidth, destHeight / srcHeight))`. This can produce
  imprecise results for non-integer scale factors.
- **`USE_TEXT_CACHE`:** The text image cache (`drawTextBlobWithCache`) is currently **enabled**
  (`USE_TEXT_CACHE = true`). Set to `false` to disable caching and use direct string rendering,
  which can be useful for debugging rendering artefacts.
- **`logImageNullError`:** The first null-image error per JVM process is logged via
  `Logger.logException()`. Subsequent occurrences are suppressed (`logImageNullError = false`)
  to avoid log flooding.

---

## Architecture Analysis: Weaknesses and Improvement Proposals

The following section documents architectural weaknesses identified by code review and proposes
concrete countermeasures. Items are grouped by concern.

---

### 1. Thread Safety

**Weakness — mixed use of `ConcurrentHashMap` and `HashMap` without a consistent threading model**

`SkiaResources` uses `ConcurrentHashMap` for `fontCache` and `fontNameMapping` but plain
`HashMap` for `imageCache`, `textImageCache`, and `cachedTextSplits`. All caches are accessed
from the SWT UI thread only, so the `ConcurrentHashMap` instances add unnecessary overhead
without providing real protection. Conversely, if a background thread ever touches these maps
(e.g. via an off-thread redraw trigger), the plain `HashMap` instances are unsafe.

**Proposal:** Decide on a clear threading contract. If all access is guaranteed to be on the SWT
UI thread (which is the natural SWT contract), replace `ConcurrentHashMap` with `HashMap`
throughout and document the constraint. If off-thread access must be supported in the future,
protect all caches uniformly.

---

### 2. Cache Invalidation and Unbounded Growth

**Weakness — text image cache (`textImageCache`) grows without a bound**

The text image cache in `SkiaResources` caches one `io.github.humbleui.skija.Image` per unique
`(text, font, transparent, background, foreground, antiAlias)` combination. For applications
that render dynamically generated or user-entered text (labels with changing values, live search
results, log viewers), the number of distinct cache keys is unbounded and the cached native
images accumulate until the canvas is disposed.

**Proposal:** Introduce an LRU eviction policy with a configurable maximum entry count (e.g.
512 or 1024 entries). Java's `LinkedHashMap` with `accessOrder = true` and an overridden
`removeEldestEntry()` is a straightforward, zero-dependency solution. Alternatively, expose the
cache limit as an OSGi system property.

**Related weakness — the `SplitsTextCache` key uses the processed string as key after mutation**

In `getTextSplits()` the `inputText` variable is mutated (tab expansion, mnemonic stripping)
*before* it is used as the key in `cachedTextSplits.put(...)`. This means a lookup with the
original unprocessed string can never hit the entry that was stored under the mutated string.
The cache effectively never yields a hit for most inputs, making it dead weight.

**Proposal:** Use the *original* unprocessed `inputText` as the key, or cache the result under
a key computed before mutation begins.

---

### 3. Platform Coupling inside Domain Objects

**Weakness — hard-coded `"win32"` / `"linux/gtk"` platform checks inside `SkiaResources`**

`createSkijaFont()` contains an `if (SWT.getPlatform().equals("win32"))` branch for font size
conversion and an Arabic-font fallback hard-coded to `"Arial"`. Platform-specific font handling
is valuable, but embedding it inside the general font creation method makes the code hard to
test in isolation and will silently produce wrong results on any platform that is neither
`"win32"` nor GTK (e.g. macOS, future platforms).

**Proposal:** Extract all platform-specific font size calculations into `DpiScaler` (as already
noted by the inline TODO comment `// TODO: move platform-specific font size calculation into the
scaler`). Font family fallback rules should be read from a configuration table, not hardcoded.

---

### 4. Clipping State Inconsistency

**Weakness — `setClipping(Rectangle)` does not call `canvas.restore()` when replacing an existing clip**

The three `setClipping(...)` overloads behave inconsistently with respect to the canvas state
stack:

| Method | When `isClipSet == true` | Expected |
|---|---|---|
| `setClipping(Path)` | calls `restore()` then `save()` + clip | ✓ correct |
| `setClipping(Region)` | calls `restore()` then `save()` + clip | ✓ correct |
| `setClipping(Rectangle)` | **skips `restore()`**, just does `save()` + clip | ✗ leaks a save level |

When `setClipping(Rectangle)` is called repeatedly, each call pushes an additional save level
without ever popping the previous clip. Over the lifetime of a GC, this accumulates extra save
levels that are only recovered by `restoreToCount(initialSaveCount)` on `dispose()`.
The clip is effectively correct by accident (Skija intersects clips), but the stack depth grows
unnecessarily and is inconsistent with the documented strategy.

**Proposal:** Apply the same `restore()` + `save()` pattern to `setClipping(Rectangle)` that
is already used for `setClipping(Path)` and `setClipping(Region)`.

---

### 5. Duplicated Code: `replaceMnemonics` and `textExtent`

**Weakness — `replaceMnemonics()` is independently duplicated in `SkiaGC` and `SkiaResources`**

Both `SkiaGC.replaceMnemonics(String)` and `SkiaResources.replaceMnemonics(String)` (called
from `expandTabs`) are identical private static helpers. Any future fix (e.g. the incomplete
mnemonic underline implementation) would have to be applied in two places.

Similarly, `textExtent(String, int)` exists in both `SkiaGC` (as a public `@Override`) and as
a private method in `SkiaResources` (called from `expandTabs`).

**Proposal:** Move the canonical implementation into a shared utility class (e.g.
`SkiaTextUtils`) or into `SkiaResources` as the single source of truth, and have `SkiaGC`
delegate to it.

---

### 6. `textLayoutDraw` Falls Back to Native GC

**Weakness — `SkiaGC.textLayoutDraw()` renders into a temporary SWT `Image` via a native GC**

`textLayoutDraw()` creates a native `Image`, opens a native `GC` on it, asks `TextLayout` to
render natively into it, and then blits the result back onto the Skia surface via
`drawImage(...)`. This defeats the purpose of GPU-accelerated rendering for any caller that uses
`TextLayout` (e.g. `StyledText`). The round-trip also incurs a CPU-side pixel readback
(`makeImageSnapshot` → `convertSkijaImageToImageData`) for every text layout draw call.

**Proposal:** Once `SkiaTextLayout` is complete, route `textLayoutDraw()` through it. As an
intermediate step, document the performance cost explicitly and consider caching the blit result
with the same key as the text image cache.

---

### 7. `drawImage` Integer Zoom Factor

**Weakness — zoom factor calculation uses integer rounding, losing precision**

In `SkiaGC.drawImage(Image, int, int, int, int, int, int, int, int)`:

```java
int factor = Math.round(Math.max(destWidth / srcWidth, destHeight / srcHeight));
```

Both `destWidth / srcWidth` and `destHeight / srcHeight` are integer divisions, so fractional
scale factors (e.g. 1.5×) are truncated to 1 before `Math.round()` is called. The result is
that scaled images are fetched at 1× zoom even when the display DPI requires 2× or higher,
causing blurry rendering.

**Proposal:** Cast operands to `float` before division:

```java
float factor = Math.max((float) destWidth / srcWidth, (float) destHeight / srcHeight);
int zoom = Math.max(1, Math.round(factor * 100)); // zoom as percentage, like the rest of the API
```

---

### 8. `SkiaCaretHandler` Code Duplication

**Weakness — `SkiaCaretHandler` re-implements `createScaledRectangle` and `offsetRectangle`**

`SkiaCaretHandler` contains private copies of `createScaledRectangle()` and
`offsetRectangle()` that are already present in `SkiaGC`. If the coordinate scaling logic
changes (e.g. for a new platform), it must be updated in two places.

**Proposal:** Move the scaling helpers into `DpiScaler` or a shared `SkiaCoordinateUtils`
class so all callers share a single implementation.

---

### 9. `SkiaRegionCalculator` is not `AutoCloseable`

**Weakness — `SkiaRegionCalculator.getSkiaRegion()` returns a `Region` that is never closed**

`io.github.humbleui.skija.Region` is a native Skija object. `SkiaRegionCalculator` caches the
calculated region in the `calculatedRegion` field but never closes it — there is no `dispose()`
method and `SkiaRegionCalculator` is not `AutoCloseable`. The `setClipping(Region)` call in
`SkiaGC` creates a new `SkiaRegionCalculator` on every call, so the native region leaks every
time clipping is changed.

**Proposal:** Make `SkiaRegionCalculator` implement `AutoCloseable`, close the `calculatedRegion`
in `close()`, and use it with try-with-resources in `SkiaGC.setClipping(Region)`.

---

### 10. `expandTabs` Performance

**Weakness — `expandTabs()` calls `textExtent(" ")` in a tight loop**

The tab expansion loop in `SkiaResources.expandTabs()` calls `textExtent(" ")` once per
non-tab character and `textExtent(" ")` repeatedly inside the tab-filling inner loop. Each call
to `textExtent` invokes `getSkiaFont().measureTextWidth(...)`, which incurs a Skija JNI round-
trip. For a string with many characters, this means hundreds of JNI calls per string.

**Proposal:** Call `textExtent(" ")` exactly once before the loop and reuse the result. Since
the font does not change during the expansion, the space width is constant.

---

### 11. `getLineAttributesInPixels` Returns Hardcoded Values

**Weakness — `getLineAttributesInPixels()` ignores the actual GC line state**

`SkiaGC.getLineAttributesInPixels()` always returns:

```java
new LineAttributes(lineWidth, SWT.CAP_FLAT, SWT.JOIN_MITER, SWT.LINE_SOLID, null, 0, 10);
```

The `cap`, `join`, `style`, `dash`, and `miterLimit` values are hardcoded regardless of what
was previously set via `setLineCap()`, `setLineJoin()`, `setLineStyle()`, etc. Any caller that
reads back line attributes after setting them (e.g. code that saves and restores GC state) will
see wrong values.

**Proposal:** Return the actual field values:

```java
return new LineAttributes(lineWidth, lineCap, lineJoin, lineStyle, lineDashes, dashOffset, miterLimit);
```

---

### 12. `USE_TEXT_CACHE` is a Public Static Mutable Field

**Weakness — `SkiaGC.USE_TEXT_CACHE` is `public static final` but semantically acts as a toggle**

The field is declared `public final static boolean USE_TEXT_CACHE = true`. Because it is
`final`, the compiler inlines the value at all call sites, so changing it requires a recompile.
The documentation says "Set to `false` to disable caching", implying it is intended as a
runtime toggle. Its `public` visibility also means any code in the same bundle can read (and, if
it were non-final, write) it directly.

**Proposal:** Introduce it as an OSGi bundle configuration property or a system property
(`System.getProperty("org.eclipse.swt.skia.useTextCache", "true")`), read once at class
initialization. This allows toggling without recompilation and makes the intent explicit.

---

### Summary Table

| # | Area | Severity | Effort |
|---|---|---|---|
| 1 | Thread safety / cache map types | Low | Low |
| 2 | Unbounded text image cache growth | **High** | Medium |
| 2b | `SplitsTextCache` key mutation bug | **High** | Low |
| 3 | Platform coupling in `SkiaResources` | Medium | Medium |
| 4 | Clip state stack leak in `setClipping(Rectangle)` | Medium | Low |
| 5 | Duplicated `replaceMnemonics` / `textExtent` | Low | Low |
| 6 | `textLayoutDraw` native GC fallback | **High** | High |
| 7 | Integer division in zoom factor | Medium | Low |
| 8 | `SkiaCaretHandler` coordinate helper duplication | Low | Low |
| 9 | `SkiaRegionCalculator` native region leak | **High** | Low |
| 10 | `expandTabs` JNI call per character | Medium | Low |
| 11 | `getLineAttributesInPixels` ignores actual state | Medium | Low |
| 12 | `USE_TEXT_CACHE` compile-time constant vs runtime toggle | Low | Low |