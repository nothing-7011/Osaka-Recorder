## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2024-05-31 - [Visual Affordance for Draggable Overlays]
**Learning:** Floating overlay windows or sheets that lack a visual "handle" or grab bar can be ambiguous to users, who may not realize the entire surface is draggable.
**Action:** Add a small, centered, pill-shaped visual handle (e.g., 32dp x 4dp, semi-transparent gray) at the top of the draggable container to clearly indicate interactivity without needing text.
