# LQ Ban Assistant — Kotlin Native

Tool gợi ý cấm tướng Liên Quân Mobile — **1 APK Kotlin duy nhất**, không server, không phụ thuộc app ngoài.

## Kiến trúc
- MediaProjection: chụp màn hình game (OS-level, game không detect)
- TYPE_APPLICATION_OVERLAY: overlay gợi ý đè lên game
- BanScorer: thuật toán chấm điểm tướng nên cấm (port từ JS)
- champions.json: bundled asset

## Build
Xem `docs/BUILD.md` — 3 cách: PC, GitHub Actions, Termux SDK.

## Phase
- [x] Phase 1: Skeleton + engine + data
- [ ] Phase 2: MediaProjection + ScreenCapture + template matching
- [ ] Phase 3: Overlay UI + BanPhase detector + end-to-end
