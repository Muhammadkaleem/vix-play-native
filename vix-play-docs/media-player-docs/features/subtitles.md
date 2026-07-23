# Subtitles

## Capability
Embedded + external subtitles across text and image formats, online search, sync, and full styling. See screens/07-subtitle-manager.md for UI.

## Rendering
- Text (SRT/VTT/SSA basic): Media3 text renderer.
- **ASS/SSA styled:** integrate **libass** (native, JNI) for accurate positioning, karaoke, fonts, effects — Media3's built-in SSA support is partial.
- Image subs (PGS/VobSub): bitmap subtitle decoder → overlay.

## Sources
- Embedded tracks via `Tracks`.
- External sidecar: auto-detect same-basename files; manual load via SAF.
- Online: OpenSubtitles API by movie hash + title; language filter; download to sidecar and select.

## Sync & style
- Offset in 50ms steps, persisted per file.
- FPS resync (23.976↔25) P2.
- Style presets (Netflix/Cinema/Minimal/Classic) + font/size/color/outline/box/position with live preview. Respect or override embedded ASS styles.

## AI subtitles (P2)
- On-device Whisper (e.g., whisper.cpp via JNI) to generate subtitles for files without them; run in background, cache as sidecar.

## Edge cases
- Encoding detection + manual override (UTF-8/Windows-125x/Big5).
- Malformed cues skipped gracefully.
- RTL languages, multi-line, overlapping cues.

## Acceptance criteria
- [ ] SRT + styled ASS render accurately with sync + styling.
- [ ] Online download applies end-to-end.
- [ ] Encoding override fixes mojibake.
