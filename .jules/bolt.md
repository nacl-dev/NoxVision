## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2025-02-13 - Avoid object allocation inside Compose Canvas drawing loops
**Learning:** Instantiating objects like `android.graphics.Paint`, `android.graphics.Rect`, or `androidx.compose.ui.graphics.Path` directly inside the drawing scope of a Jetpack Compose `Canvas` (especially in high-frequency rendering like AI detection overlays and crosshairs over a video stream) leads to massive GC churn and frame drops.
**Action:** Always hoist high-frequency drawing objects outside of the `Canvas` scope using `remember` blocks, and reuse them (e.g., using `Path.reset()`) instead of recreating them on every frame.
