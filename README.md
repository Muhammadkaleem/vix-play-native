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
repeat over ExoPlayer's native queue, and a persistent mini-player above the tabs.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass styled ASS/SSA · subtitle online search · Albums/Artists/Playlists
groupings · network streaming (SMB/FTP) · Chromecast · equalizer · playlists · private folder ·
Android TV.

## Current grill

**Audio mini-player — built, not yet device-verified.** A persistent bar above the tabs whenever
audio is loaded; tap or swipe up to expand into the full player.

Visibility keys off a `PlaybackKind` flag rather than "is something queued" — audio and video share
one player, and `stop()` keeps the playlist, so a queued-item check would advertise the video you
just exited. Keying off the flag rather than `isPlaying` also keeps the bar up while paused, so
there's still a way to resume.

The same pass moved track metadata onto the `MediaItem` itself. That deleted a whole library query
from the now-playing screen, and should fix the notification and lock screen, which were reading
metadata that was never attached.

> Three features now await a device run: background playback, the audio slice, and this. The
> notification-metadata fix in particular is only confirmable on hardware.

## Next grill

Candidates, in rough priority order:

1. **Remaining audio groupings** — Albums / Artists / Folders / Playlists tabs. Additive: different
   queries feeding the same row and player.
2. **Subtitle styling** — size/color/outline/position presets; blocked on libass for full ASS/SSA.
3. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
