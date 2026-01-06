## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2025-01-26 - [Dynamic Text Scaling in Inputs]
**Learning:** Using fixed `height` (e.g., `120.dp`) for multiline text fields breaks accessibility for users with large font sizes, as the container doesn't expand.
**Action:** Always use `minLines` and `maxLines` in `OutlinedTextField` to ensure the input area scales proportionally with the user's text settings while maintaining a "textarea" appearance.
