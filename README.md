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

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass styled ASS/SSA · subtitle online search · audio library + player ·
network streaming (SMB/FTP) · Chromecast · equalizer · playlists · private folder · Android TV.

## Current grill

**Background playback / `MediaSessionService` — built, not yet device-verified.** The `ExoPlayer`
moved out of the composable into a Hilt `@Singleton` `PlayerController` so playback can outlive the
Activity; `PlaybackService` hosts the `MediaSession` for notification, lock-screen and Bluetooth
controls. Deliberately no `MediaController` — the app is single-process, and the session alone
provides every PRD benefit without an async-null connection window.

Off by default: a video player that silently keeps playing after you background it is a battery
complaint, not a feature. The `Background playback` switch now gives `PlaybackDefaultsScreen` its
first real content.

> Needs a device run to confirm the notification, foreground-service promotion, audio focus and
> task-removal behaviour — none of that is exercised by a compile.

## Next grill

Candidates, in rough priority order:

1. **Audio library + audio player** — both screens are placeholders, now blocked on UI work rather
   than a missing engine. Needs MediaStore *audio* queries and a second player surface.
2. **Subtitle styling** — size/color/outline/position presets; blocked on libass for full ASS/SSA.
3. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
