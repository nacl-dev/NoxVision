## 2024-XX-XX - Added Button Roles to Clickable Modifiers
**Learning:** In Jetpack Compose, custom components (like Cards, Rows) acting as buttons using `Modifier.clickable` are not announced as buttons by screen readers (TalkBack) unless explicitly given a semantic role. This is an accessibility issue specific to compose applications where traditional buttons are not used.
**Action:** Always include `role = Role.Button` when adding `Modifier.clickable` to a composable that acts as a button or interactive element to ensure proper screen reader support.
