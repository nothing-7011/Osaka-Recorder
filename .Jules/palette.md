## 2024-05-22 - Dynamic Type Support for Text Fields
**Learning:** Using fixed height modifiers (e.g., `.height(120.dp)`) on text fields breaks accessibility when font scaling is increased, as text may be clipped or scroll unnecessarily.
**Action:** Always use `minLines` and `maxLines` for multi-line inputs to allow the container to scale naturally with the text size.
