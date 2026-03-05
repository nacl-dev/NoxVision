## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2024-05-24 - [Avoid GC churn in Canvas loop]
**Learning:** High-frequency overlay drawing on top of a live video stream causes severe GC churn and stuttering if heavy graphics objects (`android.graphics.Paint`, `android.graphics.Rect`, `androidx.compose.ui.graphics.Path`) are allocated within the Jetpack Compose `Canvas` loop.
**Action:** Always hoist graphics objects outside the `Canvas` scope using `remember` blocks, and reuse them (e.g. `path.reset()`) instead of instantiating new ones.
