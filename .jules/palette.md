## 2025-03-10 - Add Role.Button to custom clickable components in FeatureBountyScreen
**Learning:** Found custom Compose components (`Box`, `Card`) acting as clickable buttons without `Role.Button` applied, preventing screen readers from correctly identifying their interactive state.
**Action:** When adding `Modifier.clickable` to a composable that acts like a button, always pass `role = androidx.compose.ui.semantics.Role.Button` to ensure proper accessibility semantics.
