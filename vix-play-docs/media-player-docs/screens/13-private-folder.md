# Private Folder — PRD

**Route:** `/private` (PIN-gated) · **Priority:** P1
**Entry:** Settings > Private Folder; long-press "hide/move to private".

## Purpose
Hide sensitive media behind a PIN/biometric lock, excluded from library, history, and thumbnails.

## Layout
- PIN/biometric gate → private library grid.
- Move in/out actions; change PIN; recovery.

## Capabilities
- App-lock (PIN/pattern/biometric) for the private area (and optionally whole app).
- Excluded from media scan, history, recents, and system MediaStore where possible.
- Optional per-item encryption (P2).

## States
Locked · Unlocked · No items · PIN setup · PIN recovery.

## Edge cases
- Forgotten PIN → recovery flow (email/security answer) without exposing content.
- Biometric unavailable → PIN fallback.
- Ensure private files don't leak into system galleries (`.nomedia`, MediaStore exclusion).

## Acceptance criteria
- [ ] Private items never appear in library, history, or system gallery.
- [ ] Biometric + PIN gate works; recovery path exists.
