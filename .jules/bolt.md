## 2025-02-24 - Thermal Loop Optimization
**Learning:** JVM floating-point division is significantly slower than array lookup for small integer ranges (0-255).
**Action:** When normalizing pixel data (byte -> float) in a hot loop, use a precomputed lookup table to save ~28% execution time.
## 2025-02-24 - Async PixelCopy Race Condition
**Learning:** `PixelCopy.request` is asynchronous and non-cancellable. If wrapped in a `suspendCancellableCoroutine` that gets cancelled, the `finally` block might recycle the bitmap while `PixelCopy` is still writing to it, causing a native crash.
**Action:** Always wrap `PixelCopy` calls (or similar non-cancellable async operations) in `withContext(NonCancellable)` to ensure they complete before resource cleanup (recycling) occurs.

## 2024-05-24 - [Avoid Object Allocation in Jetpack Compose Canvas Loops]
**Learning:** Jetpack Compose `Canvas` rendering executes very frequently on the UI thread, potentially leading to Garbage Collection (GC) churn and frame rate drops if objects are instantiated inside the `Canvas` scope or draw loops.
**Action:** When drawing dynamic content on a `Canvas`, hoist and pre-allocate drawing tools such as `android.graphics.Paint`, `android.graphics.Rect`, and `androidx.compose.ui.graphics.Path` out of the `Canvas` scope using `remember { ... }`. When re-using objects like `Path`, ensure you call `path.reset()` before re-constructing the shape data for each frame.

## 2025-03-11 - Avoid Array Allocation in SensorEvent Loops
**Learning:** High-frequency Android sensor event loops (`onSensorChanged`), especially those running at `SENSOR_DELAY_UI` (approx. 60Hz), will cause rapid garbage collection (GC) churn and subsequent UI micro-stutters if objects or arrays are instantiated inside the callback or if `event.values.clone()` is used when the data is only being read temporarily.
**Action:** Always pre-allocate arrays (e.g., `rotationMatrix`, `inclinationMatrix`, `orientation`) outside of the sensor event callback and reuse them. Eliminate `.clone()` calls on sensor event values if the data is just being passed to a local filter or processing function that reads it immediately.

## 2024-05-24 - [Avoid Recreating Stroke Objects inside Jetpack Compose Canvas Loops]
**Learning:** `Stroke` object instantiation inside `Canvas` scopes adds up when drawn repeatedly. Re-assigning variables (e.g. `strokeStyle = Stroke(width = strokeWidthPx)`) only when width changes prevents massive garbage creation per frame (60Hz) during draw loops.
**Action:** Extract `Stroke` and other style objects into `remember` blocks. If dynamic updates are needed, update the `remember`ed reference only when values change rather than re-creating them on each frame.

## 2024-05-24 - [Idiomatic Jetpack Compose Density-Aware Optimizations]
**Learning:** Working around Density conversions (`.toPx()`) outside of a `Canvas` by creating mutable variables and updating them later during the draw phase is an anti-pattern that defeats the purpose of the optimization by forcing a reference wrapper allocation.
**Action:** When extracting styling objects like `Stroke` out of a `Canvas` that depend on `dp` values, get `LocalDensity.current` and pass it to a `remember` block: `remember(density) { Stroke(width = with(density) { 2.dp.toPx() }) }`.

## 2025-03-22 - Filter High-Frequency Sensor Emissions
**Learning:** Emitting a new data object unconditionally on every high-frequency Android sensor event (e.g., `SENSOR_DELAY_UI` at 60Hz) creates severe GC churn from rapid object allocations and causes downstream Jetpack Compose UI loops to recompose 60 times a second.
**Action:** When working with continuous sensor flows, track the last emitted values and apply a change threshold (e.g., > 1 degree for compass data). Only instantiate and emit the new data object if the threshold is met. This drastically reduces object allocation and UI recompositions.
