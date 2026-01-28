## 2024-05-30 - [Haptic Feedback for Primary Actions]
**Learning:** Users who perform primary actions (like "Record" or "Shutter") often do so without looking directly at the UI or while under time pressure. Visual feedback alone can be missed.
**Action:** Add `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (or `Medium`) to the `onClick` handler of the screen's primary action button to provide immediate physical confirmation of the state change.

## 2024-05-31 - [Visual Affordance for Draggable Overlays]
**Learning:** Floating overlay windows that lack standard OS chrome (title bars) can be ambiguous to interact with. Users may not realize they can reposition the window without explicit visual cues.
**Action:** Add a small pill-shaped drag handle (e.g., 32dp x 4dp, LightGray, 50% opacity) at the top center of draggable cards to provide a clear affordance for the drag gesture.

## 2026-01-14 - [Semantic Headings for Section Titles]
**Learning:** Visual text hierarchies (like "Interface" or "API Configuration") are invisible to screen readers without semantic markup, making navigation difficult for non-visual users.
**Action:** Always apply `Modifier.semantics { heading() }` to text elements that function as section headers, even if they are already styled typographically.

## 2024-06-01 - [Consistent Content Rendering]
**Learning:** When an application generates structured content (like Markdown), rendering it as plain text in secondary views (like History) creates a jarring disconnect and reduces readability compared to the primary view (Overlay).
**Action:** Use `Markwon` via `AndroidView` consistently across all surfaces where user-generated content is displayed, ensuring `textSize` and `textColor` match the surrounding theme context.

## 2024-06-02 - [Actionability in Non-Focusable Windows]
**Learning:** Overlay windows using `FLAG_NOT_FOCUSABLE` prevent native text selection, leaving users unable to copy content they can see.
**Action:** Explicitly implement a "Copy" button using `LocalClipboardManager` for any read-only text content in overlay windows, accompanied by visual (icon swap) and haptic feedback.
