## 2024-05-25 - Instantiating SimpleDateFormat in Compose Render Loop
**Learning:** Instantiating `Calendar` and `SimpleDateFormat` inside a frequently called function (`formatHour`) used in a Compose `Picker` can cause unnecessary allocations and GC pressure, leading to UI jank.
**Action:** Memoize the formatted strings or reuse the formatter instance when formatting a finite set of values (like hours 0-23) in UI components.
## 2024-05-18 - Avoid SimpleDateFormat, Use Modern DateTime API
**Learning:** Legacy `SimpleDateFormat` coupled with `Calendar` creates excessive objects and is heavily unoptimized compared to the modern `java.time` API.
**Action:** Use `DateTimeFormatter` and modern classes like `LocalTime` on minSdk >= 26 where available.
