## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.
## 2025-02-24 - Compose Canvas Allocation Optimization
**Learning:** Object allocation (e.g., `android.graphics.Paint`, `android.graphics.Rect`, `Path`) directly inside Jetpack Compose `Canvas` loops (like detected objects `forEach` and crosshairs `CrosshairStyle`) leads to high garbage collection (GC) churn and frame rate drops because the block is executed frequently (every frame or recomposition).
**Action:** Always hoist object allocations out of the `Canvas` scope and wrap them in a `remember` block. For reusable path drawing objects like `Path()`, invoke `path.reset()` before re-using the path context in every frame.
