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
| Extras | playback speed (persisted), Picture-in-Picture, sleep timer, screenshot |
| Background | `MediaSessionService` + notification/lock-screen controls, audio focus (opt-in) |
| Equalizer | multiband EQ, bass boost, virtualizer, preamp, saved presets, per-output profiles |

**Audio** — library with Tracks / Albums / Artists / Folders tabs, full-screen player with seek,
shuffle and repeat over ExoPlayer's native queue, and a persistent mini-player above the tabs.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · playlists · private folder · Android TV.

## Current grill

**Equalizer — built, not yet device-verified.** Multiband EQ with bass boost, virtualizer and
preamp, saved custom presets, and a separate profile per output route so headphones and speaker can
hold different curves.

Band count, frequencies, level range and preset names are all read from the hardware rather than
assumed — they vary by device. The app generates and owns its audio session id, which means effects
bind once and can never be orphaned by a track change; the PRD's rebind edge case stops existing
rather than being handled.

This also fixed a reachability bug: `EqualizerScreen` had a route and a callback but nothing ever
invoked it, so the screen was unreachable from anywhere in the app.

> Six features now await a device run. The equalizer is the one that most needs it — audio effects
> are restricted on some ROMs, and the graceful-disable path can only be confirmed on hardware.

## Next grill

Candidates, in rough priority order:

1. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
2. **Playlists (Step 7)** — needs a Room data model; would add the fifth audio tab.
3. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
