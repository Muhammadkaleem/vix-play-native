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

**Audio** — library with Tracks / Albums / Artists / Folders tabs, full-screen player with seek,
shuffle and repeat over ExoPlayer's native queue, and a persistent mini-player above the tabs.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · equalizer · playlists · private folder · Android TV.

## Current grill

**Subtitle styling — built, not yet device-verified.** Four presets (Netflix / Cinema / Minimal /
Classic) plus size, position, colour, background, edge and font. One editor component hosted in two
places — full-screen in Settings, and as a sheet over the player — both writing the same persisted
style, so they can't disagree.

This was previously listed as blocked on libass. That was wrong: Media3's `SubtitleView` exposes
every property the PRD asks for, including the embedded-ASS override. libass only affects fidelity
of embedded ASS rendering, which remains genuinely unbuilt.

Embedded styles are respected by default, so deliberately-styled releases render as authored. Text
size is the one exception — it always applies, because "subtitles are too small" is the most common
complaint and it would otherwise do nothing on the very files people complain about.

> Five features now await a device run: background playback, the audio slice, the mini-player,
> groupings, and this.

## Next grill

Candidates, in rough priority order:

1. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
2. **Playlists (Step 7)** — needs a Room data model; would add the fifth audio tab.
3. **Equalizer (Step 6)** — `EqualizerScreen` is still a stub; needs Android's `Equalizer` audio
   effect bound to the player session.
