## 2025-03-10 - Add Role.Button to custom clickable components in FeatureBountyScreen
**Learning:** Found custom Compose components (`Box`, `Card`) acting as clickable buttons without `Role.Button` applied, preventing screen readers from correctly identifying their interactive state.
**Action:** When adding `Modifier.clickable` to a composable that acts like a button, always pass `role = androidx.compose.ui.semantics.Role.Button` to ensure proper accessibility semantics.

## 2026-03-12 - Add Number Keyboard to numeric inputs
**Learning:** Found a numeric input field (`OutlinedTextField` for donation amount in `FeatureBountyScreen`) that correctly filtered characters (`onValueChange = { if (it.all { char -> char.isDigit() }) ... }`), but failed to show the numeric keyboard to users, leading to a frustrating UX where users had to manually switch the keyboard to numbers.
**Action:** When adding numeric `OutlinedTextField`s, always pair the value filtering logic with `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)` to display the correct keyboard layout automatically.
