## 2024-05-22 - [Empty States Matter]
**Learning:** Users react positively to empty states that guide them rather than just informing them of emptiness. A centered icon and clear instruction is better than a list item saying "No items".
**Action:** When creating list views, always design a dedicated full-screen empty state with an icon and call to action/explanation, separate from the list container.

## 2024-05-24 - [Keyboard Navigation in Forms]
**Learning:** On mobile devices, manually tapping each field in a form is tedious. Users expect the "Next" button on the soft keyboard to advance focus automatically.
**Action:** Always configure `KeyboardOptions` with `ImeAction.Next` for intermediate form fields and `ImeAction.Done` (or default) for the last field. Use `KeyboardType` (e.g., Uri, Number) to show the most relevant keyboard layout.

## 2025-05-18 - [Touch Targets in Overlays]
**Learning:** Floating overlay windows often compete with other apps for screen space and user attention. Small touch targets (like 24dp buttons) are extremely difficult to hit reliably in this context, leading to frustration.
**Action:** Never constrain `IconButton` size with `Modifier.size()`. Allow it to occupy the standard 48dp touch target, even if the icon inside is smaller. Adjust surrounding layout (e.g., Row height) to accommodate the proper touch size.
