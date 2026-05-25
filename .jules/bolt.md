## 2024-05-25 - Instantiating SimpleDateFormat in Compose Render Loop
**Learning:** Instantiating `Calendar` and `SimpleDateFormat` inside a frequently called function (`formatHour`) used in a Compose `Picker` can cause unnecessary allocations and GC pressure, leading to UI jank.
**Action:** Memoize the formatted strings or reuse the formatter instance when formatting a finite set of values (like hours 0-23) in UI components.
