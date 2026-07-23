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
| Background | `MediaSessionService` + notification/lock-screen controls, audio focus (opt-in) |
| Equalizer | multiband EQ, bass boost, virtualizer, preamp, saved presets, per-output profiles |
| Playlists | create/rename/delete, add from library, drag reorder, missing-file flagging |
| Multi-select | all three browse surfaces — audio: queue/playlist/share/trash · video & folders: share/trash · folders also rename |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Folder-browser multi-select + rename — built, not yet device-verified.** Long-press to select;
share, rename and move to trash. Multi-select now covers all three browse surfaces.

Rename works across the whole supported range: `DISPLAY_NAME` with a consent prompt on modern
Android, and a real file rename via the legacy path on API 24–28, where updating the name alone
doesn't move the file. The original extension is reapplied on save, so renaming `clip.mp4` to
`holiday` can't produce a file the system stops recognising. It's offered only when exactly one
item is selected.

**`.nomedia` hiding is blocked, not deferred.** It needs to create a file inside an arbitrary
folder, which scoped storage forbids without a SAF tree grant or `MANAGE_EXTERNAL_STORAGE` — a
storage-architecture decision, not something a feature pass can resolve.

Move and copy are deferred: both need progress reporting, cancellation and cross-volume fallbacks,
and neither degrades gracefully if interrupted.

> Thirteen features now await a device run.

## Next grill

Candidates, in rough priority order:

1. **Move / copy files** — the remaining folder-browser actions; need progress UI, cancellation
   and a cross-volume copy+delete fallback.
2. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
3. **In-app Trash screen** — restore trashed items without leaving the app; today recovery lives in
   the system Files/Photos apps.
