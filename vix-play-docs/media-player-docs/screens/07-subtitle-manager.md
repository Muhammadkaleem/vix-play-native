# Subtitle Manager — PRD

**Route:** player sheet `/player/subtitles` · **Priority:** P0
**Entry:** Video Player > subtitle button.

## Purpose
Select, load, search, sync, and style subtitles.

## Layout (bottom sheet)
- Tracks list (embedded + external), enable/disable, "off".
- Actions: Load from file · Search online · Sync offset (±) · Style · Add secondary (P2).

## Capabilities
- Formats: SRT, VTT, ASS/SSA (libass styled), PGS/VobSub (image, P1), SMI.
- External load via SAF; auto-detect same-basename sidecar files.
- Online search (OpenSubtitles) by hash + title; language filter; download to sidecar.
- Timing offset in 50ms steps; per-file persistence.
- Styling: preset (Netflix/Cinema/Minimal/Classic) + font, size, color, outline, box opacity, position. Live preview.

## States
No subtitles · Embedded only · External loaded · Searching (spinner) · Search results · Download progress · Error (network/format).

## Edge cases
- Encoding detection (UTF-8, Windows-125x, Big5) — allow manual override to fix mojibake.
- ASS embedded styling vs user override — respect or force.
- Malformed subtitle file → skip bad cues, don't crash.
- Frame-rate mismatch (23.976 vs 25) → offer FPS-based resync (P2).

## Acceptance criteria
- [ ] SRT + ASS render correctly with styling and sync.
- [ ] Online search returns and applies a subtitle end-to-end.
- [ ] Encoding override fixes garbled text.
