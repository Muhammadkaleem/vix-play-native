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
| Multi-select | all three browse surfaces — audio: queue/playlist/share/trash · video: share/trash · folders also rename, move, copy |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Move / copy files — built, not yet device-verified.** Select videos in the folder browser and
copy or move them into another folder, with per-file progress and cancellation.

Copy is the primitive and move is copy-then-delete, with the source removed only after its copy is
published. That ordering is the whole safety argument: an interrupted move leaves you with two
copies, not zero. On API 29+ the destination stays `IS_PENDING` until it succeeds, so an abandoned
copy is never visible in the gallery.

The trade: moving within a single volume copies the whole file rather than doing an instant
rename. The fast path is deliberately deferred until the safe path has actually run on hardware.

Destinations are folders that already contain videos — one write path, and the result is
guaranteed to be somewhere the app can list afterwards.

> Fourteen features now await a device run. This one touches file contents.

## Next grill

Candidates, in rough priority order:

1. **Same-volume move fast path** — `RELATIVE_PATH` update instead of copying gigabytes.
2. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
3. **In-app Trash screen** — restore trashed items without leaving the app; today recovery lives in
   the system Files/Photos apps.
