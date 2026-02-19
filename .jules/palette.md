## 2024-05-22 - [Async Button Feedback]
**Learning:** Async operations on custom toggle buttons (like PaletteButton) lack visual feedback, making the UI feel unresponsive and potentially leading to double-clicks.
**Action:** Implement an `isLoading` state in reusable button components that replaces the icon/image with a small `CircularProgressIndicator` and disables interaction during the operation.
