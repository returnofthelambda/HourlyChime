## 2024-05-25 - Instantiating SimpleDateFormat in Compose Render Loop
**Learning:** Instantiating `Calendar` and `SimpleDateFormat` inside a frequently called function (`formatHour`) used in a Compose `Picker` can cause unnecessary allocations and GC pressure, leading to UI jank.
**Action:** Memoize the formatted strings or reuse the formatter instance when formatting a finite set of values (like hours 0-23) in UI components.
## 2024-05-18 - Avoid SimpleDateFormat, Use Modern DateTime API
**Learning:** Legacy `SimpleDateFormat` coupled with `Calendar` creates excessive objects and is heavily unoptimized compared to the modern `java.time` API.
**Action:** Use `DateTimeFormatter` and modern classes like `LocalTime` on minSdk >= 26 where available.
## 2024-07-06 - Memoize Exact String Results for Zero Allocation Scroll
**Learning:** Instantiating `LocalTime` and formatting it within a Composable like `TimePickerDialog` (even when `DateTimeFormatter` is cached) causes unnecessary allocations that lead to GC pressure and jitter during fast scrolling for finite sets.
**Action:** When a UI component scrolls through a tiny finite set of options (e.g., hours 0-23), compute and cache the exact string results in a simple array to achieve zero allocation per render loop, making scrolling buttery smooth.
## 2024-05-19 - MediaPlayer Synchronous I/O on Main Thread
**Learning:** Using `MediaPlayer.prepare()` inside a BroadcastReceiver's `onReceive` blocks the main thread, causing ANRs or dropped frames. While changing to `prepareAsync()` avoids blocking, it requires proper `goAsync()` lifecycle management to keep the background process alive long enough to finish playback.
**Action:** When handling media or I/O in short-lived components like `BroadcastReceiver`, always use async methods paired with `goAsync()` and `PendingResult` to ensure safe, non-blocking execution.
