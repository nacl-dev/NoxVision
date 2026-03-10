## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2024-05-24 - [Avoid Object Allocation in Jetpack Compose Canvas Loops]
**Learning:** Jetpack Compose `Canvas` rendering executes very frequently on the UI thread, potentially leading to Garbage Collection (GC) churn and frame rate drops if objects are instantiated inside the `Canvas` scope or draw loops.
**Action:** When drawing dynamic content on a `Canvas`, hoist and pre-allocate drawing tools such as `android.graphics.Paint`, `android.graphics.Rect`, and `androidx.compose.ui.graphics.Path` out of the `Canvas` scope using `remember { ... }`. When re-using objects like `Path`, ensure you call `path.reset()` before re-constructing the shape data for each frame.
