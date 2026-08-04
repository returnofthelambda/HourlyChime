## 2024-05-25 - Instantiating SimpleDateFormat in Compose Render Loop
**Learning:** Instantiating `Calendar` and `SimpleDateFormat` inside a frequently called function (`formatHour`) used in a Compose `Picker` can cause unnecessary allocations and GC pressure, leading to UI jank.
**Action:** Memoize the formatted strings or reuse the formatter instance when formatting a finite set of values (like hours 0-23) in UI components.
## 2024-05-18 - Avoid SimpleDateFormat, Use Modern DateTime API
**Learning:** Legacy `SimpleDateFormat` coupled with `Calendar` creates excessive objects and is heavily unoptimized compared to the modern `java.time` API.
**Action:** Use `DateTimeFormatter` and modern classes like `LocalTime` on minSdk >= 26 where available.
## 2024-06-22 - Optimizing Wear OS Pickers by eliminating object allocations
**Learning:** Wear OS devices have limited processing capability. Rapidly recomposing components like `Picker` that call functions repeatedly (e.g., formatting hours 0-23) can cause significant UI jank. Even using modern APIs like `java.time`, calculating and instantiating a String representation dynamically or instantiating a `List` of indices repeatedly is too costly for smooth scrolling.
**Action:** Pre-calculate and cache strings (using an Array for rapid access) and avoid redundant `List` allocations by utilizing exact index lookups when generating simple numeric `Picker` options.
## 2024-07-06 - Memoize Exact String Results for Zero Allocation Scroll
**Learning:** Instantiating `LocalTime` and formatting it within a Composable like `TimePickerDialog` (even when `DateTimeFormatter` is cached) causes unnecessary allocations that lead to GC pressure and jitter during fast scrolling for finite sets.
**Action:** When a UI component scrolls through a tiny finite set of options (e.g., hours 0-23), compute and cache the exact string results in a simple array to achieve zero allocation per render loop, making scrolling buttery smooth.
## 2024-07-28 - Offload MediaPlayer preparation to avoid BroadcastReceiver Main Thread Blocking
**Learning:** Using synchronous `MediaPlayer.prepare()` in a `BroadcastReceiver` causes Main Thread blocking, which can result in ANRs (Application Not Responding) if I/O or decoding takes too long.
**Action:** When handling media in `BroadcastReceiver`, use `goAsync()` to obtain a `PendingResult`, call `MediaPlayer.prepareAsync()`, and signal `pendingResult.finish()` in the completion/error callbacks.
