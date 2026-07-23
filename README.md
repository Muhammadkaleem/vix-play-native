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
| Multi-select | long-press to select; add to queue, add to playlist, share, delete |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Bulk delete — built, not yet device-verified. Destructive.** Deleting selected tracks, across the
three flows Android requires: the OS confirmation dialog on API 30+, the recoverable-permission
prompt on 29, and an app-provided dialog on 24–28 where the platform offers no confirmation at all.

The rule enforced throughout is that **no path deletes without at least one confirmation**. On API
29 that can mean two prompts for one action; that redundancy is the right trade for something
irreversible.

After a delete the app re-queries MediaStore rather than trusting what it asked to remove — the
user can deselect items inside the system dialog, so the request isn't the outcome. Deleted tracks
are also dropped from the active queue, so deleting what you're listening to skips onward instead
of erroring.

> Ten features now await a device run, and this is the one where being wrong isn't recoverable.
> Verify on files you can afford to lose.

## Next grill

Candidates, in rough priority order:

1. **Lift multi-select to the video library / folder browser** — the selection primitive is ready.
2. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
3. **Trash instead of delete** — `createTrashRequest` (API 30+) is a recoverable soft-delete and a
   better default than permanent removal.
