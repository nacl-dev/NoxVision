## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2024-05-28 - [Compose Canvas Scope Allocation]
**Learning:** Instantiating objects like `android.graphics.Paint`, `android.graphics.Rect`, or `Path` directly inside the `Canvas` scope of a Composable creates significant Garbage Collection (GC) churn and frame drops, especially for high-frequency operations like video rendering and object detection overlays.
**Action:** Always hoist object instantiations outside of the `Canvas` scope and wrap them in `remember` blocks. When reusing stateful drawing objects like `Path`, ensure you call `.reset()` before reusing them inside the drawing logic.
