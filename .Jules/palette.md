## 2024-05-23 - Overlay Service Copy UX
**Learning:** `OverlayService` using `ComposeView` (WindowManager) lacks standard text selection/copy behavior found in `Scaffold` screens. Users can't natively select text in `FLAG_NOT_FOCUSABLE` windows.
**Action:** Always provide explicit action buttons (Copy, Share) for content displayed in floating overlays or non-focusable windows.
