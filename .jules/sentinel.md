## 2024-05-24 - [Insecure Password Input in SettingsScreen]
**Vulnerability:** The WiFi password input field (`OutlinedTextField`) in `SettingsScreen.kt` was missing `PasswordVisualTransformation` and `KeyboardOptions(keyboardType = KeyboardType.Password)`, causing the password to be displayed in plaintext.
**Learning:** This oversight allowed shoulder-surfing and potentially caused the password to be cached in the device's keyboard dictionary, which is a known security vulnerability for sensitive data entry in Jetpack Compose.
**Prevention:** Always apply `visualTransformation = PasswordVisualTransformation()` and configure `keyboardOptions` with `KeyboardType.Password` when implementing text fields that collect sensitive information like passwords or API keys in Jetpack Compose.
