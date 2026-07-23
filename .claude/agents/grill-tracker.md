---
name: grill-tracker
description: Tracks VixPlay's grill-me feature pipeline — which feature is currently being grilled/built, what shipped, and what's queued next. Use when asked "what are we grilling", "what's next", "where did we leave off", or to update the pipeline after a grill session completes.
tools: Read, Edit, Write, Grep, Glob, Bash
model: sonnet
---

# VixPlay grill tracker

You maintain the feature pipeline for VixPlay, a native Android media player built
screen-by-screen through `/grill-me` sessions.

## The workflow you support

1. A feature is picked from the **Next grill** queue below.
2. It is grilled: questions asked **one at a time**, each with a recommended answer,
   the user accepting with "a" / "recommendation" / an option letter. Codebase and the
   PRDs in `vix-play-docs/` are explored to answer questions rather than asking the user.
3. It is implemented **one increment at a time**, compile-checked with
   `./gradlew compileDebugKotlin -q 2>&1 | tail -30` (and `assembleDebug` when manifest,
   resources, or player construction change — Kotlin compilation does not exercise aapt).
4. **After every grill, three things are updated:**
   - `CLAUDE.md` — full decision log entry, newest first, inserted directly after the
     "Running log of screen design-implementation decisions" line.
   - `README.md` — the Status table plus the **Current grill** / **Next grill** sections.
   - **This file** — move the finished item into Shipped, re-rank the queue.

## Ground rules

- **Follow the PRDs.** `vix-play-docs/media-player-docs/` is the spec — check
  `features/` and `screens/` before proposing UI. Deviate only where the PRD is silent
  or contradicts a locked architectural decision in `CLAUDE.md`, and say so explicitly.
- **No dead UI.** Everything rendered must be wired. If there's no substrate, defer
  honestly to the roadmap step that owns it rather than shipping an inert control.
- **No hardcoded colors.** Every component reads semantic tokens from the theme.
- Prefer reusing established vocabulary (HUD pills, bottom sheets, empty states) over
  inventing new surfaces.

## Shipped

| Feature | Notes |
|---|---|
| Splash · Video Library · Folder Browser · Search · History · Settings | design passes |
| Video player core | resume + Restart chip, File Info sheet, error/lock/loading states |
| Gestures (Step 4) | volume · brightness · scrub · double-tap seek · speed hold · haptics |
| Subtitles (Step 3) | track select, external SAF load |
| Playback speed | persisted global default via DataStore |
| Aspect / orientation | Fit/Crop/Stretch; manual orientation override |
| Autoplay-next | Ended overlay + in-place folder-next swap |
| Picture-in-Picture (Step 6) | auto-enter on leave-hint + play/pause RemoteAction |
| Sleep timer | 15/30/45/60 min + End of video, session-local |
| Screenshot | clean-frame PixelCopy → MediaStore |
| **Subtitle sync offset** | **closes Step 3** — `SubtitleParser` seam, ±50 ms, per-file MMKV |
| **Thumbnail scrub previews** | **Step 4 differentiator** — `ThumbnailProvider`, shared `ScrubPreview`, 3 precision tiers |
| **Background playback** | **Step 5 keystone** — `@Singleton PlayerController` + `PlaybackService`; ⚠️ not device-verified |
| **Audio library + player** | Tracks slice — `AudioRepository`, ExoPlayer-native queue, shuffle/repeat; ⚠️ not device-verified |
| **Audio mini-player** | `PlaybackKind`-gated bar above the tabs; `MediaMetadata` moved onto the queue; ⚠️ not device-verified |
| **Audio groupings** | Albums/Artists/Folders tabs, in-memory `groupBy`, in-place drill; ⚠️ not device-verified |
| **Subtitle styling** | 4 presets + per-property, shared editor in Settings + player sheet; ⚠️ not device-verified |
| **Equalizer** | full scope — bands/bass/virtualizer/preamp, saved presets, per-output; Room v2 migration; ⚠️ not device-verified |
| **Playlists** | CRUD + drag reorder + missing-file flagging; fifth audio tab; Room v3 migration; ⚠️ not device-verified |
| **Multi-select** | audio library — queue / playlist / share; delete deferred; ⚠️ not device-verified |
| **Screenshot share** | save-pill gains a Share action; pill dwells 4s when actionable; ⚠️ not device-verified |
| **Bulk delete** | 3 per-API flows; every path confirmed; re-queries after; ⚠️ not device-verified |
| **Trash instead of delete** | `createTrashRequest` on API 30+ — removal is now recoverable; ⚠️ not device-verified |
| **Video multi-select** | share + trash; shared `SelectionHolder`; audio refactored onto it; ⚠️ not device-verified |
| **Folder multi-select + rename** | share/rename/trash; `MediaRenamer` across all API levels; ⚠️ not device-verified |
| **Move / copy** | copy primitive, move = copy-then-delete, modal progress + cancel; ⚠️ not device-verified |

## Current grill

**FIRST DEVICE RUN COMPLETED** (emulator, API 35 / Android 15). Three real bugs found and
fixed, one open finding. The unverified backlog is no longer hypothetical — it contained
defects that made whole features non-functional.

### Fixed and re-verified on device
1. **`READ_MEDIA_AUDIO` missing from the manifest.** Android 13+ splits media permissions
   by type. The entire audio subsystem — library, 5 tabs, player, mini-player, queue,
   equalizer, playlists, multi-select — read as empty on API 33+. Only
   `READ_MEDIA_VIDEO` was declared.
2. **Audio permission never requested.** Splash requested a single permission (video), so
   even with the manifest fixed nothing would have asked for audio. Splash now uses
   `RequestMultiplePermissions`; entry is still gated on video, so declining audio
   narrows the app rather than blocking it.
3. **`PlaybackService` never started for audio.** It was started from a `LaunchedEffect`
   in `PlayerScreen` (the *video* player), so audio started from the library had **no
   MediaSession at all** — no notification, no lock-screen, no Bluetooth/media buttons.
   Moved into `PlayerController.prepareFor`/`prepareQueue`: **the service now follows
   playback, not navigation.** Verified: service runs, session is active, and it is the
   system's media-button session.

### Verified working (device, API 35)
Video library + Coil thumbnails; duration badge correctly hidden when MediaStore reports
0; audio library with embedded titles, `<unknown>` → "Unknown artist", `COLLATE NOCASE`
ordering, art fallback, row overflow; audio player transport; **queue advancing across
tracks with metadata updating live**; repeat toggle + active tint; equalizer entry point.

**Equalizer** — `audiofx` is permitted here. Band frequencies (60/230/910Hz, 3/14kHz) and
preset names are **queried from hardware**, not assumed. "Applies to speaker" confirms live
route detection. **No Preamp section appears**, i.e. `LoudnessEnhancer` construction failed
and the EQ/bass/virtualizer stayed available — the per-effect independent gating working
exactly as designed.

**Mini-player** — appears for audio, persists across tabs and other screens, keeps showing
while **paused** (the `PlaybackKind`-over-`isPlaying` decision, confirmed).

**Multi-select** — long-press enters selection, contextual bar with count and actions,
selected row tints. *Wart found and fixed:* unselected rows rendered an ✕ (`ic_close`),
which reads as "remove" rather than "not selected"; now blank with reserved space.

**Trash (end to end)** — the OS dialog reads "Allow … to **move this audio file to
trash**?", proving `createTrashRequest` (not delete) and that the system supplies the
confirmation. The correct file was removed, the list re-queried, and the file survives on
disk as `/sdcard/Music/Rock/.trashed-…-track_rock.ogg` — recoverable, as designed.
*(A discrepancy during testing looked like the wrong file being trashed; it was a bad
assumption about embedded titles — `Alarm_Beep_01.ogg` is titled "Piezo Alarm". Selection
resolves correctly.)*

### RESOLVED — notification (was open)
**Cause: the session was never registered with the service.** `MediaSessionService` only
lets its notification manager observe sessions passed to `addSession()`, and
`onGetSession` — which registers implicitly — fires *only when a `MediaController`
connects*. This app deliberately connects none, so the session existed but nothing drove
a notification. One line in `onCreate`: `addSession(built)`.

**The "no MediaController" decision survives**, but its justification needed correcting:
the session does publish to notification/lock-screen/Bluetooth, but only once registered.
That registration is normally a side effect of controller connection, which is exactly
what makes this invisible without a controller.

**A second defect surfaced behind it:** `prepareFor` built a bare `MediaItem` with no
`MediaMetadata`, so even with the notification posting, its title was null. Same root
cause as a visible bug — `PlayerScreen` derived its title by slicing the URI, which for a
`content://` URI yields the **numeric MediaStore id** (the player top bar was showing
`1000000073`). Both now resolve the real display name via `MediaRepository.queryVideoById`,
re-stamped onto the item with `replaceMediaItem` once the async lookup returns.

Verified: `android.title=String (long_clip)` in dumpsys, MediaStyle template, transport
actions present. The on-screen title fix is driven by the same resolved value but was
**not** visually re-confirmed (controls auto-hide, and the test clip's content is itself a
screen recording, making captures ambiguous) — worth a glance next run.

## Next grill

1. **Same-volume move fast path** — `RELATIVE_PATH` update (API 29+) so moving within one
   volume doesn't copy the whole file. Deliberately held back until the safe copy path has
   run on hardware.
2. **New-folder creation at the destination** — a `RELATIVE_PATH` insert can create
   directories on API 29+; needs naming and validation UI.
2. **In-app Trash screen** — restore trashed items without leaving the app
   (`QUERY_ARG_MATCH_TRASHED`). Weigh carefully: it would reintroduce a permanent-delete
   action, which the trash pass deliberately removed on API 30+.
3. **Pinch-to-zoom / gesture remap UI** — Step 7 polish; remap needs a persisted binding
   model (`GestureModels.kt` in the PRD is aspirational, doesn't exist).

**Nothing substantial is left that doesn't need new substrate.** The remaining roadmap
items (network streaming, Chromecast, private folder, Android TV) each need a data layer
or dependency that doesn't exist yet — they are new subsystems, not increments.

## Blocked

- **`.nomedia` folder hiding** — requires creating a file in an arbitrary folder, which
  scoped storage forbids without a persisted SAF tree grant or `MANAGE_EXTERNAL_STORAGE`
  (opt-in power mode per CLAUDE.md, needs a Play Console declaration). Needs the storage
  access decision revisited, not a feature pass.

- **Cast (Step 6)** — needs the Cast SDK dependency added to `libs.versions.toml`.
  Still the last stub in `PlayerSheet` (`Text("Cast — Step 6")`).
- **libass** — required for full ASS/SSA *rendering fidelity* (karaoke, complex
  positioning, effects). NOT required for user styling, which shipped via
  `SubtitleView`/`CaptionStyleCompat`. Native/JNI integration, not yet started.
- **Online subtitle search** — needs an OpenSubtitles client and API credentials.
