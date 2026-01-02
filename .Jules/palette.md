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

## 2024-05-28 - [Real-time Data Consistency]
**Learning:** Users expect the "History" screen to reflect new recordings immediately without manual refresh or re-navigation, especially when background services are generating data.
**Action:** Use `SharedFlow` or similar reactive streams to broadcast data updates from background services and observe them in UI screens (`LaunchedEffect` with `.collect`) to ensure the view is always consistent with the latest state.

## 2024-06-03 - [Helper Text vs Placeholders]
**Learning:** Placeholder text disappears on input, making it poor for persistent guidance like default values or required field indicators.
**Action:** Use `supportingText` in Material 3 text fields to display critical context (like default values or "Required") that needs to remain visible during typing.
