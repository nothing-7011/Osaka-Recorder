## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2024-06-01 - [Visual Affordance for Draggable Overlays]
**Learning:** Floating windows (OverlayService) implemented as Cards lack inherent visual cues for drag interactions, leading to low discoverability of the move gesture.
**Action:** Add a standardized "drag handle" (32dp x 4dp rounded pill) at the top center of floating containers to explicitly signal interactivity.
