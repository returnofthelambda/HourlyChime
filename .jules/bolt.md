## 2024-05-25 - Instantiating SimpleDateFormat in Compose Render Loop
**Learning:** Instantiating `Calendar` and `SimpleDateFormat` inside a frequently called function (`formatHour`) used in a Compose `Picker` can cause unnecessary allocations and GC pressure, leading to UI jank.
**Action:** Memoize the formatted strings or reuse the formatter instance when formatting a finite set of values (like hours 0-23) in UI components.
## 2024-05-18 - Avoid SimpleDateFormat, Use Modern DateTime API
**Learning:** Legacy `SimpleDateFormat` coupled with `Calendar` creates excessive objects and is heavily unoptimized compared to the modern `java.time` API.
**Action:** Use `DateTimeFormatter` and modern classes like `LocalTime` on minSdk >= 26 where available.
## 2026-06-29 - Pre-computing formatted strings for Compose UI components
**Learning:** Even when the underlying `DateTimeFormatter` is cached, repeated calls to `LocalTime.of().format()` within a Compose `Picker` component's render loop allocate new objects and increase GC pressure on constrained Wear OS devices. Caching just the formatter isn't enough for bounded, frequently rendered values.
**Action:** When a UI component like a scrollable Picker needs to display a finite, small set of localized string values (like hours 0-23), fully memoize the resulting strings and only recalculate them on Locale changes, thereby eliminating allocation overhead in the render loop.
