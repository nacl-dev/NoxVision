## 2025-03-06 - [A11y] Screen Reader Semantics for Modifier.clickable
**Learning:** In Jetpack Compose, when building custom components (like Cards, Rows, or Boxes) that act as interactive buttons, simply adding `Modifier.clickable { ... }` is insufficient for accessibility. Screen readers (like TalkBack) will announce them as generic text or containers, not actionable buttons.
**Action:** Always include `role = Role.Button` when using `Modifier.clickable` on custom components to ensure proper semantic announcement by screen readers (e.g., `Modifier.clickable(role = Role.Button) { ... }`).
