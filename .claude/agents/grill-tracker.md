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

## Current grill

None in progress. **Six consecutive features are now built but unrun on hardware.** This is the largest untested surface the project has carried
and it keeps growing; clear it before adding more.

Device checklist:
- **Background playback:** notification appears and its controls work; audio survives
  app-swipe with the toggle on; playback pauses on background with it off; PiP still
  keeps playing; audio focus ducks/resumes around a call.
- **Audio slice:** the MediaStore query returns tracks on a real library; album art
  resolves (and the placeholder shows where it doesn't); tapping a track queues from
  that point; next/prev/shuffle/repeat behave.
- **Mini-player:** appears only for audio (never after exiting a video); stays up while
  paused; tap and swipe-up both expand; next works; progress advances.
- **Notification metadata:** title/artist/artwork now populate. This was very likely
  blank before `MediaMetadata` was attached to the queue — confirm the fix landed.
- **Groupings:** Albums/Artists/Folders populate and their counts match the Tracks tab;
  drilling in and pressing system back returns to the grouping list, not out of the
  screen; playing from inside a group queues that group only.
- **Subtitle styling:** presets visibly change the captions; the size slider works on an
  ASS file even with the override toggle off (the deliberate asymmetry); Settings and the
  player sheet show the same values.
- **Equalizer (needs hardware most):** effects are permitted at all on this device — if
  not, the screen must show the explanation and NO controls; bands audibly change output;
  settings survive a track change and an app restart; plugging in headphones swaps to that
  route's profile; saving and re-applying a preset round-trips.
- **Room migration 1 → 2:** *verified without hardware* — the hand-written DDL matches
  Room's exported `schemas/…/2.json` exactly, so schema validation passes, and the
  migration only adds tables (it never touches `playback_positions`), so resume history
  is preserved by construction. Still worth a glance at Continue Watching after the
  first upgrade, but this is no longer an open risk.

## Next grill

1. **Share the screenshot** — small: plumb the saved MediaStore uri through and add a
   share intent, likely as an action on the confirmation pill.
2. **Playlists (Step 7)** — needs Room entities (playlist + playlist_track), CRUD UI, and
   the `/playlists/{id}` route. Would add the deferred fifth audio tab.
3. **Pinch-to-zoom / gesture remap UI** — Step 7 polish; remap needs a persisted binding
   model (`GestureModels.kt` in the PRD is aspirational, doesn't exist).

## Blocked

- **Cast (Step 6)** — needs the Cast SDK dependency added to `libs.versions.toml`.
  Still the last stub in `PlayerSheet` (`Text("Cast — Step 6")`).
- **libass** — required for full ASS/SSA *rendering fidelity* (karaoke, complex
  positioning, effects). NOT required for user styling, which shipped via
  `SubtitleView`/`CaptionStyleCompat`. Native/JNI integration, not yet started.
- **Online subtitle search** — needs an OpenSubtitles client and API credentials.
