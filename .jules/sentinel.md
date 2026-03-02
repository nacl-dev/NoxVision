## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.
## 2024-03-24 - Unprotected Password Field in Settings
**Vulnerability:** The WiFi password field in `SettingsScreen.kt` was an `OutlinedTextField` without a visual transformation, meaning the password was displayed in plaintext. It also didn't specify `keyboardType = KeyboardType.Password`, which means the password could be cached by the soft keyboard.
**Learning:** Always use `PasswordVisualTransformation` and `KeyboardOptions(keyboardType = KeyboardType.Password)` for sensitive text fields in Jetpack Compose to prevent shoulder-surfing and keyboard dictionary caching.
**Prevention:** Review all `OutlinedTextField` and `TextField` components handling credentials or sensitive tokens to ensure they have these properties set.
