## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2025-02-24 - Compose Canvas Garbage Collection (GC) Avoidance
**Learning:** Re-instantiating `android.graphics.Paint`, `android.graphics.Rect`, and `androidx.compose.ui.graphics.Path` objects within Compose `Canvas` loops causes severe GC churn during fast re-renders (like stream overlays). This directly results in dropped frames.
**Action:** When working in `Canvas` modifiers, explicitly hoist object instantiations outside of the rendering scope using `remember`, and clear their state across rendering passes (e.g. `path.reset()`).
