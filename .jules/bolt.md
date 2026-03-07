## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.
## 2026-03-07 - High-Frequency Android Sensor Loop Allocation Optimization
**Learning:** Object allocation inside a high-frequency sensor event loop (e.g., `onSensorChanged` for `SensorEventListener`) like those in Android can lead to significant GC churn and frame drops. Calls like `event.values.clone()` or re-initializing arrays like `FloatArray(9)` inside this callback should be avoided.
**Action:** Move all necessary array allocations outside the event loop and reuse them (pre-allocation) for read operations. Avoid `.clone()` unless mutation on the shared input data explicitly requires it.
