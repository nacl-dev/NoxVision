## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.
## 2025-02-24 - Compose Canvas Allocation Churn
**Learning:** Re-instantiating objects like `android.graphics.Paint`, `android.graphics.Rect`, `Path`, and static lists directly inside Jetpack Compose's `Canvas` (which is recomposed often, like a drawing loop) or inside a Composable functions causes unnecessary GC churn which can degrade frame rates.
**Action:** Always hoist object allocations outside of the `Canvas` `DrawScope` and wrap them in a `remember` block so they are instantiated only once and reused across recompositions. For `Path` objects, use `remember` and then call `path.reset()` before redrawing.
