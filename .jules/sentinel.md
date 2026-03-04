## Sentinel Journal

## 2024-03-24 - Unsafe Media URL Construction
**Vulnerability:** URLs for media files were constructed by concatenating unsanitized filenames (e.g., "$baseUrl/$filename"). This could lead to malformed URLs if filenames contained spaces or special characters, or potential server-side interpretation issues if path traversal sequences were present.
**Learning:** Developers often assume filenames are safe because they are "from the camera", but relying on external input without sanitization is risky. Manual string concatenation for URLs is error-prone.
**Prevention:** Always use `URLEncoder` (for path segments) or `Uri.Builder` (on Android) to construct URLs programmatically. Use helper functions to centralize URL logic.
## 2024-03-24 - Plaintext Passwords
**Vulnerability:** WiFi password input field used plain `OutlinedTextField` without visual transformation, displaying the password in plaintext and allowing keyboard caching.
**Learning:** In Compose, explicit `PasswordVisualTransformation` and `KeyboardType.Password` are required for sensitive fields.
**Prevention:** Always use `visualTransformation` and `keyboardOptions` on sensitive input fields.
