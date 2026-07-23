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
| Subtitles | track select, external SAF load, **sync offset (±50 ms, per file)** |
| Audio | track switch (multi-language / commentary) |
| View | aspect Fit/Crop/Stretch, manual orientation override, immersive full-screen |
| Extras | playback speed (persisted), Picture-in-Picture, sleep timer, screenshot |
| Background | `MediaSessionService` + notification/lock-screen controls, audio focus (opt-in) |

**Audio** — tracks library (MediaStore, album art), full-screen player with seek, shuffle and
repeat over ExoPlayer's native queue.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass styled ASS/SSA · subtitle online search · audio mini-player ·
Albums/Artists/Playlists groupings · network streaming (SMB/FTP) · Chromecast · equalizer ·
playlists · private folder · Android TV.

## Current grill

**Audio library + player (Tracks slice) — built, not yet device-verified.** A `MediaStore.Audio`
query feeds a tracks list; tapping one queues the whole visible list from that point and opens a
full-screen player with album art, seek, shuffle and repeat.

The queue *is* ExoPlayer's own playlist rather than a list maintained beside it, so shuffle order
and repeat modes come from the engine and the UI can't drift out of sync with what's playing.
Album art goes through Coil here — many distinct items, one image each, the exact inverse of the
scrub-preview case that needed a dedicated provider.

> Two features now await a device run: this one, and background playback before it. Neither the
> audio query, album-art resolution, queue transitions, nor the foreground service is exercised by
> a compile.

## Next grill

Candidates, in rough priority order:

1. **Audio mini-player** — persistent bar above the bottom nav so playback stays reachable after
   leaving the player. Cross-cutting nav surface; has a PRD acceptance criterion.
2. **Subtitle styling** — size/color/outline/position presets; blocked on libass for full ASS/SSA.
3. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
