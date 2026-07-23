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

**Audio** — library with Tracks / Albums / Artists / Folders tabs, full-screen player with seek,
shuffle and repeat over ExoPlayer's native queue, and a persistent mini-player above the tabs.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass styled ASS/SSA · subtitle online search · network streaming (SMB/FTP) ·
Chromecast · equalizer · playlists · private folder · Android TV.

## Current grill

**Audio groupings — built, not yet device-verified.** Albums, Artists and Folders join Tracks as
tabs. All three are derived in memory from the same MediaStore query, so they can't disagree with
each other or with the `IS_MUSIC` filter — MediaStore's own Albums/Artists tables don't know about
that filter and would report track counts that differ from what's listed.

Drilling into a group happens in place, matching the video-side folder browser, so no new routes.
Playing from inside a group queues *that group* rather than the whole library.

Playlists is deliberately not a fifth tab: it's P1 with its own route and no data model yet, so the
tab would be selectable and permanently empty.

> Four features now await a device run: background playback, the audio slice, the mini-player, and
> this.

## Next grill

Candidates, in rough priority order:

1. **Subtitle styling** — size/color/outline/position presets; blocked on libass for full ASS/SSA.
2. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
3. **Playlists (Step 7)** — needs a Room data model; would add the fifth audio tab.
