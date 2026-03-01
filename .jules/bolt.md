## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.
## 2025-02-24 - Sensor Event Loop Allocations
**Learning:** Android `SensorEventListener.onSensorChanged` is called very frequently (hot loop). Array allocations like `FloatArray(9)` and cloning arrays using `event.values.clone()` inside this callback create severe memory churn and trigger frequent Garbage Collection, which introduces micro-stutters and degrades performance in high-frequency applications.
**Action:** Move all object and array allocations out of the `onSensorChanged` callback loop (pre-allocate them at the scope level) and read directly from `event.values` without cloning unless strictly necessary.