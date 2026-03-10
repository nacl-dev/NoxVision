## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.

## 2024-03-24 - Plaintext WiFi Password Exposure in Settings UI
**Vulnerability:** The WiFi password field in `SettingsScreen.kt` was displayed as plain text (`OutlinedTextField` without `PasswordVisualTransformation`). This allows shoulder-surfing and potential exposure of the network password while the user is configuring or viewing the camera settings.
**Learning:** In Jetpack Compose, sensitive text fields do not automatically obscure input. It must be explicitly configured using `visualTransformation` and `keyboardOptions` to prevent visibility and keyboard caching.
**Prevention:** Always use `PasswordVisualTransformation()` and `KeyboardOptions(keyboardType = KeyboardType.Password)` for any `TextField` or `OutlinedTextField` that handles passwords, API keys, or other sensitive secrets.
