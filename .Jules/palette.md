## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2024-05-31 - [Visual Affordance for Draggable Overlays]
**Learning:** Floating overlay windows that lack standard OS chrome (title bars) can be ambiguous to interact with. Users may not realize they can reposition the window without explicit visual cues.
**Action:** Add a small pill-shaped drag handle (e.g., 32dp x 4dp, LightGray, 50% opacity) at the top center of draggable cards to provide a clear affordance for the drag gesture.
