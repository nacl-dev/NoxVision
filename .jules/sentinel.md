## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.

## 2024-03-24 - Plaintext Sensitive Input Fields
**Vulnerability:** The WiFi password input field in the settings screen did not mask user input, leaving sensitive data vulnerable to shoulder-surfing and potentially exposing credentials to third parties or screen recording software. It also did not hint to the OS keyboard that it was entering a password.
**Learning:** Even internal or local network passwords should be treated securely in UI components. In Jetpack Compose, the `visualTransformation` and `keyboardOptions` properties are necessary for sensitive inputs.
**Prevention:** Always apply `visualTransformation = PasswordVisualTransformation()` and `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)` to `OutlinedTextField` or `TextField` composables that handle passwords or sensitive tokens.
