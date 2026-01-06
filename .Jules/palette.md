## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2026-01-06 - [Enforcing Validation on Save]
**Learning:** Visual validation errors in forms (like "1-300s") are insufficient if the underlying save logic blindly accepts invalid input. This disconnect erodes user trust and data integrity.
**Action:** Always clamp or validate form values in the final `save()` operation to match the constraints shown in the UI, ensuring that the saved state is always valid regardless of user input.
