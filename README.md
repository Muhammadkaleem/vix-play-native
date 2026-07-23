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
| Multi-select | long-press to select — audio: queue/playlist/share/trash · video: share/trash |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Multi-select lifted to the video library — built, not yet device-verified.** Long-press a video
to select; the contextual bar offers Share and Move to trash.

Only two of the PRD's six listed actions had anything behind them — playlists are audio-only, and
rename/properties/hide don't exist anywhere in the app — so two real actions shipped rather than
six buttons that can't work.

The selection logic moved into a shared `SelectionHolder` and the audio library was refactored onto
it in the same pass, so there's one implementation rather than two copies waiting to diverge.

The Continue Watching rail hides while selecting: its cards aren't selection-aware, so leaving it
visible would mean a tap there opens the player while a tap below ticks a checkbox.

> Twelve features now await a device run.

## Next grill

Candidates, in rough priority order:

1. **Folder-browser multi-select** — needs its own file-system actions (move, copy, rename,
   `.nomedia`), which is separate work from selection.
2. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
3. **In-app Trash screen** — restore trashed items without leaving the app; today recovery lives in
   the system Files/Photos apps.
