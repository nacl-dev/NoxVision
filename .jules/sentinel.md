## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.

## 2024-03-24 - Plaintext Password Input Field
**Vulnerability:** The WiFi password input field in `SettingsScreen.kt` was displayed as plain text without any visual transformation, making it susceptible to "shoulder surfing" and potentially leaking the password to anyone viewing the screen.
**Learning:** In Jetpack Compose, sensitive text fields must explicitly use `PasswordVisualTransformation` and an appropriate `KeyboardOptions` (with `KeyboardType.Password`) to properly mask the input and configure the keyboard securely.
**Prevention:** Always apply `visualTransformation = PasswordVisualTransformation()` and `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)` to any `TextField` or `OutlinedTextField` that handles sensitive information like passwords.
