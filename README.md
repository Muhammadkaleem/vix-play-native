# vix-play-native

VixPlay — a native Android media player. Kotlin + Jetpack Compose, Media3/ExoPlayer, indigo→violet dark-first design.

## Build

```bash
./gradlew assembleDebug     # debug APK
./gradlew installDebug      # install on a connected device
./gradlew test              # unit tests
./gradlew lint              # lint
```

## Stack

Kotlin · Jetpack Compose · Media3 (ExoPlayer) · Hilt · Room · DataStore · MMKV · Coil

## Docs

- `CLAUDE.md` — architecture decisions, build order, and the design-implementation log
- `vix-play-docs/media-player-docs/` — product docs
- `config-vix-play/player-design-assets/` — color tokens, typography, icons, launcher icon
- `.claude/agents/grill-tracker.md` — which feature is being grilled now, and what's queued next

## Status

Built via `/grill-me` sessions — each feature is stress-tested question-by-question against the
PRDs in `vix-play-docs/`, then implemented and logged in `CLAUDE.md`.

**Player surface (Steps 2–6)**

| Shipped | |
|---|---|
| Playback core | resume position, folder-next autoplay, Ended overlay, error/lock states |
| Gestures | volume · brightness · double-tap seek · drag-to-vary speed hold · haptics |
| Precision seek | thumbnail scrub previews on both seek paths, 3 vertical precision tiers |
| Subtitles | track select, external SAF load, sync offset (±50 ms, per file), **styling + presets** |
| Audio | track switch (multi-language / commentary) |
| View | aspect Fit/Crop/Stretch, manual orientation override, immersive full-screen |
| Extras | playback speed (persisted), Picture-in-Picture, sleep timer, screenshot + share |
| Gesture remap | remap the two vertical drags + two double-taps (persisted); pinch-to-zoom + pan always on |
| Background | `MediaSessionService` + notification/lock-screen controls, audio focus (opt-in) |
| Equalizer | multiband EQ, bass boost, virtualizer, preamp, saved presets, per-output profiles |
| Playlists | create/rename/delete, add from library, drag reorder, missing-file flagging |
| Multi-select | all three browse surfaces — audio: queue/playlist/share/trash · video: share/trash · folders also rename, move, copy |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About, and a
PIN-locked private folder (app-private vault, PBKDF2-hashed PIN, recovery code).

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · Android TV.

## Current grill

**Pan while zoomed (finishes pinch-zoom) — built, not-zoomed regression device-verified; pan
itself needs a real pinch to reach.** Once pinched in, a one-finger drag moves the viewport, so you
can inspect any corner instead of only the centre.

While zoomed, the drag is repurposed for panning — brightness / volume / seek / speed-hold suspend
until you zoom back out (double-tap resets zoom, so there's always a one-gesture way back). The pan
is clamped so the zoomed content always covers the viewport: you reach any edge but never reveal a
black bar, and pinching out re-clamps so the image can't stay stranded off-centre. Zoom and pan
reset on the next video.

On device: with the pan branch added, double-tap seek and vertical-drag volume still fired in the
un-zoomed state — the normal single-finger gestures are unbroken. Reaching the zoomed state to test
pan itself needs a genuine two-finger pinch, which the emulator can't inject (same limit as the
pinch pass), so the pan feel needs a hardware check.

> Sixteen features now await (or have partially had) a hardware run.

## Next grill

Candidates, in rough priority order:

1. **Same-volume move fast path** — `RELATIVE_PATH` update instead of copying gigabytes.
2. **In-app Trash screen** — restore trashed items without leaving the app; today recovery lives in
   the system Files/Photos apps.
3. **New-folder creation at a move/copy destination** — `RELATIVE_PATH` insert can make directories
   on API 29+; needs naming and validation UI.
