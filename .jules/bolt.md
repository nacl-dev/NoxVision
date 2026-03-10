## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.
## 2025-03-10 - Jetpack Compose Canvas Object Allocation
**Learning:** Instantiating objects like `Paint` or `Rect` within a Jetpack Compose `Canvas` loop (e.g., inside a `forEach` loop on a frequently updated list) causes severe garbage collection (GC) churn and UI jank, as the `Canvas` is redrawn on every frame during high-frequency updates like live video stream overlays.
**Action:** Always hoist object allocations outside of the `Canvas` block and wrap them in `remember { ... }` so they are instantiated only once and reused across recompositions and draw frames.
