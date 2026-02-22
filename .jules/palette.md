# Palette's Journal

This file contains critical UX and accessibility learnings for the NoxVision project.

## 2025-05-18 - [DarkButton Loading State]
**Learning:** Adding a loading state to buttons (specifically for connection) allows users to cancel the action while receiving visual feedback that the action is in progress. The "Cancel" button should remain enabled during this state.
**Action:** When implementing async actions that can be cancelled, ensure the button shows a spinner but remains clickable.
