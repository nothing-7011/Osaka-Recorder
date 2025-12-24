## 2024-05-22 - [Empty States Matter]
**Learning:** Users react positively to empty states that guide them rather than just informing them of emptiness. A centered icon and clear instruction is better than a list item saying "No items".
**Action:** When creating list views, always design a dedicated full-screen empty state with an icon and call to action/explanation, separate from the list container.

## 2024-05-24 - [Keyboard Navigation in Forms]
**Learning:** Users expect the "Next" button on the soft keyboard to advance focus automatically.
**Action:** Always configure `KeyboardOptions` with `ImeAction.Next` for intermediate form fields and `ImeAction.Done` (or default) for the last field.

## 2024-05-25 - [Text Selection in Compose]
**Learning:** By default, Text composables in Jetpack Compose are not selectable. This frustrates users who expect to be able to copy content like logs or history details.
**Action:** Wrap read-only text content that a user might want to copy (like error logs, history details, or API keys) in a `SelectionContainer`.

## 2024-05-25 - [Dynamic Accessibility Descriptions]
**Learning:** Static content descriptions for buttons that change function (like a "Back" button becoming a "Close" button in a detail view) are confusing for screen reader users.
**Action:** Use conditional logic to update `contentDescription` dynamically when the UI state changes the button's purpose, ensuring the description matches the current action.

## 2024-05-27 - [Live Regions for Status Updates]
**Learning:** Visual text updates (like "Recording..." appearing) are invisible to screen readers unless explicitly announced. Users relying on TalkBack may miss critical state changes.
**Action:** Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on Text composables that display dynamic status updates to ensure they are announced automatically.
