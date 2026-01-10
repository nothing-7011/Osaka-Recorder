## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2024-06-03 - [Visual Drag Affordance]
**Learning:** Floating windows (OverlayService) that support drag gestures often lack visual cues, forcing users to "guess" the interactivity. A simple drag handle clarifies the behavior without clutter.
**Action:** Add a small, centered, semi-transparent pill-shaped Box (32dp x 4dp) at the top of the draggable surface.
