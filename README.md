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
| Multi-select | long-press to select; add to queue, add to playlist, share, move to trash |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Trash instead of delete — built, not yet device-verified.** Removing tracks now goes to the
system trash on API 30+, recoverable for around 30 days from the Files or Photos apps, rather than
deleting permanently. Same OS confirmation dialog as before.

This was taken ahead of its queue position because it directly de-risks bulk delete, which shipped
irreversible and unverified. The riskiest operation in the app is now forgiving.

Devices below API 30 have no trash concept, so they keep permanent delete with the app's own
confirmation. Labels follow the device: "Move to trash" where it's recoverable, "Delete" where it
isn't, so the UI promises what will actually happen.

The trade: on API 30+ you can no longer permanently delete from inside VixPlay. That decision moves
to the system trash UI, which already exists and is where people look for it.

> Eleven features now await a device run.

## Next grill

Candidates, in rough priority order:

1. **Lift multi-select to the video library / folder browser** — the selection primitive is ready.
2. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
3. **In-app Trash screen** — restore trashed items without leaving the app; today recovery lives in
   the system Files/Photos apps.
