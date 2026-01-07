## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2025-05-24 - [Avoid Fixed Height for Multiline Inputs]
**Learning:** Using `Modifier.height()` on `OutlinedTextField` for multiline input forces a fixed container size, which breaks when users increase their system font size (Dynamic Type).
**Action:** Use `minLines` and `maxLines` instead. This allows the text field to grow naturally with the content and font size while maintaining a usable default size.
