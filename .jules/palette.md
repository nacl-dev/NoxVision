## 2026-03-04 - Add Role.Button to clickable Cards and Rows
**Learning:** In Jetpack Compose, wrapping content in custom containers like `Card` or `Row` and adding `.clickable` does not automatically announce the element as a button to screen readers like TalkBack. They announce it just as "clickable".
**Action:** Always include `role = Role.Button` inside the `.clickable()` modifier for custom interactive components that behave like buttons (e.g. `SettingsCategoryCard`, `HuntingFeatureCard`) so visually impaired users know they can activate them.
