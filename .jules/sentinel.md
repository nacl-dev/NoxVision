## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.

## 2024-03-24 - Plaintext Password Input in Compose
**Vulnerability:** The Wi-Fi password input field in Jetpack Compose was displaying user input in plaintext and allowing the keyboard to cache the password.
**Learning:** In Jetpack Compose, sensitive text fields must explicitly opt-in to secure text entry. Simply naming a variable `password` is not enough; the UI component must be configured to hide the text and hint to the keyboard not to learn the input.
**Prevention:** Always use `visualTransformation = PasswordVisualTransformation()` and `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)` on any `TextField` or `OutlinedTextField` that handles sensitive data like passwords or tokens.
