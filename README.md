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
| Playlists | create/rename/delete, add from library, drag reorder, missing-file flagging |

**Audio** — library with Tracks / Albums / Artists / Folders / Playlists tabs, full-screen player
with seek, shuffle and repeat over ExoPlayer's native queue, a persistent mini-player above the
tabs, and playlists with drag reorder.

**Library shell** — video library, folder browser, search, history, settings hub, About.

**Not yet built** — libass (full ASS/SSA fidelity) · subtitle online search · network streaming
(SMB/FTP) · Chromecast · private folder · Android TV.

## Current grill

**Playlists — built, not yet device-verified.** Create, rename, delete, add tracks from any row's
overflow, drag to reorder, play all or shuffle. This also fills the fifth audio tab, which was
deliberately left out earlier because there was no data model behind it.

Drag reorder is hand-rolled — Compose has no built-in reorderable list and this is one screen, so
no dependency was added. Long-press lifts an item (leaving ordinary scrolling alone) and the new
order is persisted once on drop rather than on every swap.

Items whose file has disappeared are shown greyed and labelled rather than dropped, and are skipped
when playing. That's why each entry caches its title and artist: a row for a deleted file can't be
labelled otherwise.

> Seven features now await a device run. Drag reorder is the piece most likely to need tuning —
> thresholds and autoscroll are judged by feel.

## Next grill

Candidates, in rough priority order:

1. **Share the screenshot** — small follow-up: plumb the saved MediaStore uri into a share intent.
2. **Multi-select in the audio library** — long-press was deliberately left free for this.
3. **Pinch-to-zoom / gesture remap UI (Step 7)** — needs a persisted gesture-binding model.
