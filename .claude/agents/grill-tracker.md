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
| **Private folder** | app-private vault, PBKDF2 PIN, recovery code, hide-from-library; ✅ device-verified |
| **Folder + video listing** | folders A–Z + persisted sort, real compact list row, in-folder resume; ✅ device-verified |
| **Gesture remap + pinch-zoom** | remap 2 drags + 2 double-taps (DataStore), always-on pinch; ✅ editor + single-finger verified (pinch not injectable on emulator) |
| **Pan while zoomed** | one-finger pan once pinched in, clamped to bounds, resets on playNext; ✅ not-zoomed regression verified (pan needs a real pinch to reach) |
| **Network streaming — URL slice** | HTTP(S)/HLS/DASH/RTSP; media3 hls/dash/rtsp + INTERNET; NetworkScreen hub + NetworkStreamScreen; recents in Room v3→v4; network-aware Retry on ErrorOverlay; ✅ device-verified (HLS plays, error/retry) |
| **Theme & Skins (closes Step 5)** | ThemeMode {SYSTEM,LIGHT,DARK,AMOLED,DYNAMIC} + curated accent; live-applied via ThemeViewModel; orphaned useAmoledTheme removed; LibrarySkeleton de-leaked; ✅ device-verified |
| **Folder-first Video home + list default** | Video tab opens to FOLDER_BROWSER_ROUTE (flat VideoLibraryScreen dropped; Search+History moved into folder top bar); default view GRID→LIST; folder icon on folder rows; thumbnail frame@10% + video-glyph fallback; mini-player swipe-down dismiss; ✅ device-verified |
| **AdMob banner integration** | `MobileAds` init + UMP (EEA) consent gate before any ad loads; test-unit-in-debug / real-in-release via `BuildConfig`; anchored adaptive banner in app `bottomBar` (Video/Audio/Network — never Settings, never the player, via `isFullScreen`); `adsEnabled` seam reserved for a future Pro toggle; ✅ device-verified (banners render) |
| **Per-list ad placements** | Folder list gets an inline **Native** card; in-folder video list gets a top **Banner** + inline **Native** every ~8 rows; app-level Main banner suppressed on the folder browser to avoid stacking; `ads/AdUnits.kt` + `AdsController`/`AdsViewModel`/`AdBanner`/`AdNativeCard`; ⚠️ **native ads fail to load on the emulator** (internal error 0 — reproduced on 2 SDK versions; banners fine, degrades to rendering nothing) — needs a real-device check |
| **16 KB page-size support** | `libmmkv.so` was 4 KB-aligned; MMKV 1.3.9 → 2.4.1; all native libs now 16 KB-aligned; ✅ verified in the AAB |
| **SMB/FTP network shares** | `player/datasource/SmbDataSource.kt` + `FtpDataSource.kt`; `data/network/NetworkServerRepository.kt` + `NetworkShareBrowser.kt` + `CredentialStore.kt`; `ui/network/SmbBrowserScreen.kt` ("Network shares") + `NetworkSharesViewModel.kt`; `data/db/entity/NetworkServer.kt` + dao; smbj 0.14.0 + commons-net 3.11.1 + security-crypto; Room v4→v5; ✅ **FTP verified end-to-end against a local pyftpdlib server**; ⚠️ SMB not runtime-verified (no SMB server available in this environment) |
| **Distribution & Open-With (Growth 3.1 — A)** | `ACTION_VIEW`/`SEND`/`SEND_MULTIPLE` intent filters (MIME + extension) → play any handed-in URI, bypassing the splash permission gate (Back returns to caller); "Set as default player" Settings row deep-linking `ACTION_APP_OPEN_BY_DEFAULT_SETTINGS` (**no RoleManager media role exists — spec corrected**); Play in-app review after ≥3 completions, no error, once/screen (`review-ktx` 2.0.2); Android 14 partial-media (`READ_MEDIA_VISUAL_USER_SELECTED`) as a first-class state + folder-home "Manage" banner. `MainActivity`/`VixPlayNavGraph`/`Splash*`/`PlayerScreen`+VM/`SettingsScreen`/`FolderBrowser*`; ✅ **device-verified (emulator API 36):** external `VIEW` plays full-screen bypassing splash/permission, Back→caller, "Set as default player" opens the system Open-by-default screen, partial-media gate + "Manage" banner confirmed. Only the in-app review prompt is unverifiable off-Play |
| **New-folder creation at destination** | Move/copy picker gains a "New folder…" entry → name dialog (strict validation + collision reject, Create disabled until valid) → transfer into `Movies/<name>`; MediaStore can't make empty dirs, so the folder is created lazily when the files land (`RELATIVE_PATH` insert/update). Shared `runTransfer` core so a new-folder move gets the same-volume fast path for free. `FolderBrowserViewModel` (+`transferToNewFolder`/`runTransfer`), `FolderBrowserScreen` (+picker row + name dialog + `newFolderNameError`). ✅ **device-verified (emulator API 36):** created "Archive2026" via a move — write-consent, same MediaStore `_id`, dir created + file landed, source emptied, folder home lists the new folder. |
| **Same-volume move fast path** | Move within one volume is now an in-place `RELATIVE_PATH` update (API 30+) — instant, no byte copy, atomic per file — behind one `createWriteRequest` batch consent; path-prefix volume detection decides fast-vs-copy before prompting; cross-volume / API ≤29 / failures fall through to the existing copy-then-delete. `MediaTransfer` (+`writeRequest`/`applyMove`), `FolderBrowserViewModel` (+`pendingFastMove`/`sameVolume`, reuses the delete-consent launcher). ✅ **device-verified (emulator API 36):** "modify" write-consent shown (not copy/trash), same MediaStore `_id` with updated `relative_path`, source folder emptied, UI reloaded. ⚠️ cross-volume fallback not exercisable (single-volume emulator) |
| **Daily Briefing (P1 differentiator — Calendar slice)** | Net-new `briefing` package (kept in `:app`, single-module). Calendar-only via `CalendarContract` (`READ_CALENDAR`, no OAuth/backend) + local heuristics for the day-shape/conflicts (no LLM); exact-alarm→WorkManager scheduling with inexact fallback + boot/time/tz re-register; DataStore config with `sampleApprovedAt` go-live gate (FR-6); full conversational wizard (Intent→Sources→Schedule→Connect→Sample→Go-live→Confirm); own `briefing` notification channel + lock-screen redaction; no new tab (Settings + notification + before-noon home card). Email/Mentions/News deferred behind a `Connector` seam (OAuth+backends). ✅ **device-verified end-to-end (emulator API 36):** full wizard, calendar-permission grant, real sample + watermark, Go-live schedules the alarm (`dumpsys`), settings on-state, view brief, home card before/after noon. **Real crash found + fixed** — LazyColumn nested in a verticalScroll parent on the Sample step. ⚠️ actual scheduled morning delivery not waited out |
| **Crashlytics + crash reporting (Growth 3.1 — B, measurement half)** | Firebase Crashlytics + ANR + **NDK native-crash** capture wired onto the pre-existing (unused) `google-services.json`; `firebase-bom` 33.7.0 + `google-services`/`crashlytics` plugins; collection **on in release / off in debug** (`isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG`), independent of UMP ad-consent; global custom keys (`route` / `playback_kind` via `PlayerController.updateKind` / `external_open`) + one curated **codec non-fatal** at `onPlayerError` (the "plays everything" signal). `VixPlayApp`/`PlayerController`/`VixPlayNavGraph`/`MainActivity`/`PlayerScreen`. ✅ **device-verified init (emulator API 36):** Crashlytics 19.3.0 + native handler 3.2.0 initialize, app launches clean. ⚠️ actual dashboard upload is release-only by design (debug collection off); **Firebase Analytics funnel deferred** (locked "at beta") |
| **Library Pulse notifications (Growth 3.1 — C)** | All 3 triggers (resume nudge / new-in-library / weekly digest) via WorkManager periodic + `hilt-work` `HiltWorkerFactory` (`VixPlayApp` = `Configuration.Provider`, default `WorkManagerInitializer` removed); `PulseEvaluator` (one/run, precedence, 3-day min-gap cap, quiet hours 22–08, acknowledge-on-open), own `pulse` channel, dedicated Notifications settings screen + per-trigger toggles + "Turn off" action (`PulseActionReceiver`), resume tap deep-links into the player (auto-resume) with home beneath. `notifications/*`, `Media/SettingsRepository` (+`DATE_ADDED` queries, 9 DataStore keys), `MainActivity`/`VixPlayNavGraph`. ✅ **partially device-verified (emulator API 36):** settings screen + master-gates-subtoggles + persistence + worker scheduled (~24h/BATNOTLOW) and ran once returning SUCCESS. **Real crash found + fixed** — `LIMIT` in a `ContentResolver.query` sort order crashed startup. ⚠️ actual notification delivery / Turn-off / quiet-hours / deep-link taps still unrun (need a qualifying trigger over days) |

## Current grill

None in progress. The four most recent grills (Distribution & Open-With, Library Pulse,
Crashlytics, Daily Briefing) are built and **device-verified on emulator API 36** to the extent
each allows — Distribution fully; Pulse partially (settings/gating/persistence/worker; a real
startup crash found+fixed); Crashlytics init + native handler confirmed; **Daily Briefing fully
end-to-end** (wizard → Go-live → alarm scheduled → settings/brief/home-card; a real Sample-step
crash found+fixed). What still needs a real device/track: Pulse's + Briefing's actual scheduled
notification *delivery* (a qualifying fire over time), the Play-only in-app review prompt, and
Crashlytics dashboard **upload** (release-only by design). See **Next grill** below.

## Prior device-verification pass (historical log)

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

### Playlists — verified, one real bug fixed
Create (inline, from a track's overflow), add tracks, Room persistence, detail screen with
Play all / Shuffle / per-row delete, **cached titles surviving as designed**, and reorder
**persisting** across leaving and re-entering the screen — all confirmed.

**Bug: a drag of any length advanced exactly one position.** Root cause was *not* the swap
detection — it was `Modifier.pointerInput(index)`. `pointerInput` restarts when its key
changes, and a swap changes the dragged row's index, so the block was torn down and the
gesture cancelled mid-drag. **I rewrote the detection twice at the wrong layer** (offset
rebasing, then pointer hit-testing) before spotting it; both failed identically, which was
the clue. Now keyed on `Unit` with the live index read via `rememberUpdatedState`, and
detection is deterministic distance accumulation that derives nothing from `layoutInfo`
mid-gesture. Verified: one drag now moves an item two positions.

*Note:* the Playlists **tab** shows no track count, while the separate `PlaylistsScreen`
route does — two playlist lists of differing fidelity. The PRD asks for name + item count
+ art collage.

### Folder browser — MAJOR finding: most of its UI was never wired
Rename, Copy, Move **and** the sub-API-30 delete confirmation were all **missing entirely**.
The state (`renaming`, `choosingDestination`, `confirmDelete`), the ViewModel wiring,
`MediaRenamer` and `MediaTransfer` all existed and compiled — but the dialogs that render
them were never inserted. Tapping the menu items did nothing at all.

**Cause: three of my own string-anchored edits silently no-op'd.** Each anchored on text a
previous failed edit was supposed to have created, so the failures cascaded. Nothing
errored, the code compiled, and `assembleDebug` passed throughout. **Lesson: use an edit
tool that fails loudly on a missed anchor; a silent no-op in a build that still compiles
is invisible.**

After wiring them, verified on device:
- **Rename** — dialog seeds the name *without* the extension; `createWriteRequest` shows
  the OS "Allow … to modify this video?" prompt; `long_clip.mp4` → **`holiday.mp4`**, with
  the extension reapplied though only "holiday" was typed. Correct on disk and in MediaStore.
- **Copy** — destination picker lists other folders and **excludes the current one**;
  the copy landed in `DCIM/Camera` with the **source intact**, **identical MD5**
  (byte-for-byte), and correctly indexed at the destination path.

- **Move — bug found and fixed.** `MediaTransfer` removed the source with a raw
  `contentResolver.delete()`, which throws `RecoverableSecurityException` on API 30+ for
  files the app doesn't own. `runCatching` swallowed it, so **move silently degraded into
  copy** while reporting a failure. I had built the whole consent flow in `MediaDeleter`
  and then bypassed it. Source removal now routes through that path — **one consent
  prompt for the whole batch**, after all copies succeed. Verified: `clip_c.mp4` moved
  folders, and the original became `.trashed-…-clip_c.mp4` — i.e. **a move leaves the
  original recoverable**, which is safer than a conventional move.
  *The safety design held throughout: the failed move left two copies, never zero.*

Still untested here: cancel-mid-copy cleanup and multi-file batches. The legacy
(API ≤28) rename and copy branches **cannot** run on this API-35 image at all.

### Private folder (grill) — built and verified on device
Vault in app-private storage, PIN-gated. The PRD's "hide in place via .nomedia + MediaStore
exclusion" is **not buildable** under scoped storage without MANAGE_EXTERNAL_STORAGE
(power-mode, Play-gated) — so "private" means moving the bytes into `filesDir/private`,
which MediaStore genuinely cannot see, and trashing the public original.

- **PIN/recovery:** PBKDF2 salted hashes (no dependency), verified never stored in clear.
  Recovery code shown once at setup (DEK2-LLDX), grouped, unambiguous alphabet.
- **Hide:** "Move to private" (lock icon) in the video-library multi-select → copies to
  vault → consent to remove the public original (goes to trash, recoverable) → scrubs the
  file's resume position.
- **Verified on device, full chain:** set PIN → recovery screen → empty vault; hide clip_c
  → it leaves the library query and the original becomes `/sdcard/Movies/.trashed-…-clip_c`;
  vault **re-locks on exit**; wrong-empty PIN stays locked, correct PIN (2468) unlocks; the
  hidden item shows in the vault with a thumbnail decoded from the app-private file.

**Deferred (logged, not silent):** biometric unlock (needs androidx.biometric + an enrolled
fingerprint to test — acceptance criterion), folder-browser "move to private" entry,
per-item encryption (PRD P2), unhide/delete not yet exercised on device.

### Folder + video listing (grill) — done, verified on device
Three PRD gaps fixed:
1. **Folders were sorted by video count**, so any file operation reshuffled the whole
   list — it even made my own testing tap the wrong folder. Now **Name A–Z by default**
   (stable: position changes only on rename), with a persisted sort control
   (Name / Recent / Item count). `FolderEntry` gained `dateModified` for Recent.
2. **`ViewMode.LIST` was a 1-column grid of full-size cards** — fewer items than the grid,
   the opposite of a list. New shared `VideoListRow` (96×54, reusing the `HistoryRow`
   vocabulary) is a genuine compact row. `ViewMode` is now persisted in `SettingsRepository`
   and **shared** by the library and folder browser.
3. **In-folder videos had no resume bar** (`FolderBrowserViewModel` never had
   `PlaybackRepository`). Now wired: verified `holiday.mp4` shows a resume fill in-folder.

Verified on device: folders list Camera/Movies A–Z; library and folder browser both render
the compact row in LIST; view mode persisted across navigation; in-folder resume bar shows.

### Subtitle styling — verified, one wart fixed
Live preview, four presets, sliders, swatches, edge and font chips all work. Selecting
**Cinema** propagated correctly (yellow serif, larger size, yellow swatch selected), and
the choice **survived an app restart** — DataStore persistence confirmed.

*Wart fixed:* the Background swatches are black at 0/50/80/100% alpha, painted straight
onto a near-black surface, so three of the four were **visually identical**. They now sit
on a light underlay, so the alpha progression reads.

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

### Gesture remap + pinch-to-zoom (grill) — built, device-verified except the pinch itself
Requested as "Gesture remap" **+ "add pinch to zoom in out, as well"**, so pinch became a
real always-on gesture, not a deferral. `GestureRemapScreen` was a dead `LibraryEmptyState`
stub since the Settings pass.

- **Design:** the modifier stays binding-blind (emits `VerticalDrag(side)` / `Zoom(scale)`),
  `PlayerScreen` resolves meaning through the user's `GestureBindings`. Pinch is a strictly
  ≥2-pointer detector placed *first* in the chain that consumes nothing at one finger, so the
  verified single-finger loop is never starved. Double-tap doubles as zoom-reset when zoomed.
  Bindings persist in DataStore (defaults = old hardcoded behaviour); zoom scale is per-video,
  not persisted.
- **Verified on device (emulator API 35):** editor renders with defaults; edits (Left-drag→None,
  Right-tap→Skip 30s) **persisted across navigate-away-and-back**; **Reset** restored defaults.
  In-player **double-tap-right fired "+10s"** and **vertical-drag-right raised the Volume HUD** —
  proving the new first-position pinch detector doesn't starve single-finger gestures and the
  binding path works end-to-end.
- **Not verifiable here (stated honestly):** a genuine two-finger pinch can't be injected on this
  emulator — no `adb root` (production build), SELinux blocks `sendevent` to `/dev/input` (shell
  is in the `input` group but MAC denies it), and `input motionevent` is single-pointer. The pinch
  code is straightforward + the `graphicsLayer` transform is deterministic; the detector gating is
  what needed proving and was proven indirectly. **Needs a real finger test.**
- **Deferred:** per-file zoom persistence, pan-while-zoomed (zoom is centre-anchored today),
  sensitivity/haptics sliders, remapping the skip amount beyond the two presets, live editor preview.

### Pan while zoomed (grill) — built, not-zoomed regression device-verified
Finishes the pinch-zoom feature — zoom was centre-anchored, so you couldn't inspect a corner.

- **Design:** while zoomed, a one-finger drag pans (free 2D `Pan(dx, dy)`); brightness/volume/
  seek/speed-hold suspend until 1× (double-tap resets zoom = the escape hatch). The modifier
  gained a `zoomed` flag (read live via `rememberUpdatedState`, so crossing 1× never restarts
  the detectors) and bails the moment a 2nd finger lands so the pinch detector owns it. Pan is
  clamped to `±(scale−1)·size/2` (content always covers the viewport), 1:1, and the `Zoom`
  handler re-clamps on pinch-out. Zoom+pan reset on `playNext`. Long-press speed-hold is guarded
  off while zoomed (else it strands the speed-restore path).
- **Verified on device (emulator API 35):** with the pan branch added, in-player double-tap seek
  and vertical-drag volume still fired in the un-zoomed state — the normal single-finger path is
  unbroken by the new `zoomed` param/branch. **Pan itself couldn't be exercised** — reaching the
  zoomed state needs a genuine two-finger pinch, un-injectable here (same limit as the pinch pass).
  The pan/clamp math is deterministic given the zoomed state; needs a real finger test.
- **Deferred:** elastic overscroll on the bounds, edge-exclusion for pan starts near the system
  back-gesture gutter (pan can start anywhere today — watch for collision), per-file zoom persistence.

## Release stage

The app is now at **Play-release stage**: signed release `signingConfig` (git-ignored
`keystore.properties`, `vixplay-release-key`), app name **VixPlay**, **versionCode 58 /
3.0.0**, AdMob is now **live** (`play-services-ads` 25.4.0 + `user-messaging-platform`
3.1.0, UMP consent gate, real ad units in release), 16 KB page-size compliant. Signed AAB
builds and verifies (`jar verified`).

**Monetization gap, stated plainly: ads now show to every user, with no way to remove
them.** The `adsEnabled` seam exists in `AdsController` specifically so a future Pro
entitlement can flip it off, but nothing flips it yet — there is no billing integration
and no purchase surface. **The user has explicitly decided Pro/Billing is built dead
last** (see Next grill below) — this gap stays open on purpose until every other
buildable-now item is done.

## Grilled — design locked, HELD on external blockers

- **Subtitle online search** (OpenSubtitles .com REST) — full design locked (BuildConfig
  API key, moviehash+filename match, `PlayerSheet.SubtitleSearch`, login-gated download to
  app-cache, Retrofit+OkHttp+kotlinx-serialization). **HELD: needs the user's registered
  OpenSubtitles API key.**
- **Chromecast / Cast** — full design locked (media3-cast `CastPlayer` dual-player swap in
  `PlayerController`, NanoHTTPD token-scoped local-file server, framework `MediaRouteButton`,
  Default Media Receiver, Play-Services-gated). **HELD: needs a real Chromecast to verify.**
- **Pro entitlement / Google Play Billing v6** — grilled through Q1–Q4 (locked: single
  "Remove ads" product, local Play verification per the CLAUDE.md decision, no backend),
  paused at Q5. **HELD, not on a blocker — the user has explicitly decided this is built
  dead last**, after every other buildable-now item. See Next grill ranking below.

## Next grill

New PRD material landed under `vix-play-docs/media-player-docs/` from a growth-foundation
PRD split: `features/distribution.md`, `features/analytics.md`, `features/pro-billing.md`,
`features/notifications.md`, `features/daily-briefing.md`, `product/05-growth-foundation.md`,
and screens `16-daily-briefing` / `17-briefing-setup` / `18-briefing-settings`. These are
buildable feature specs, not aspirational — folded into the ranking below.

Distribution & Open-With, Notifications (Library Pulse), Crashlytics, and Daily Briefing are all
**built and shipped** — see the Shipped table. The **Firebase Analytics funnel** was deliberately
NOT built (Q1 of the Crashlytics grill): CLAUDE.md locks it to "add at beta when real users
exist," so it stays gated until there are installs to measure (the `completed_playbacks` counter
is its seed). The remaining queue is smaller storage/verification items before the billing cluster.

1. **In-app Trash screen — RECOMMENDED NEXT.** Restore trashed items (`QUERY_ARG_MATCH_TRASHED`);
   weigh whether to reintroduce a permanent-delete action. The last self-contained storage item
   before the queue turns to external-blocked / deferred work.
2. **Daily Briefing — account sources (Email/Mentions/News)** — the deferred `Connector`
   implementations. **HELD** on external services: OAuth apps (Google/MS/Slack), a summarization
   backend, and a news API — none configured. Grill/design when those exist (the seam is ready).
3. **Remaining device-verification (Pulse + Briefing delivery, review, Crashlytics upload, SMB)** —
   most of Distribution + Pulse + Crashlytics + Briefing is verified on emulator API 36 (see
   Shipped). Still unrun: **actual scheduled notification delivery** for Pulse (idle trigger over
   days) and Briefing (a real morning fire), the **in-app review** flow (Play-track only),
   **Crashlytics dashboard upload** (release build), and **SMB** (no server was available). Not a
   grill — a verification task.

### Gated (not a grill until its condition is met)

- **Firebase Analytics funnel** — `features/analytics.md`. Locked to "at beta / real users
   exist" per CLAUDE.md. Build it when there are installs whose activation/playback/revenue
   funnel is worth measuring; Crashlytics (its Firebase sibling) is already wired, so this is a
   dependency-light follow-on when the gate opens.

### Deferred to last, by explicit user decision

4. **Pro entitlement / Google Play Billing v6.** Ads now show to every user with no
   relief, and the `adsEnabled` seam in `AdsController` exists for exactly this. It would
   otherwise be a strong "next" candidate (small, closes the AdMob grill's deliberate
   gap, unblocks the two ad formats below) — **but the user has explicitly chosen to
   build this dead last**, after every other buildable-now feature. Design is already
   locked through Q4 (see "Grilled — HELD" above); only Q5 and implementation remain.
5. **Interstitial ads (player-exit, capped)** — parked pending Q1: does purchasing Pro
   suppress interstitials as part of "remove ads," or is Pro scoped to banners/native
   only? Sequenced after Pro/Billing by construction, and therefore also last.
6. **Rewarded ads** — needs the Pro entitlement to exist first (e.g. "watch an ad to
   unlock a Pro feature for 24h"), so it is sequenced after item 4, and therefore also
   last.

## Blocked

- **`.nomedia` folder hiding** — requires creating a file in an arbitrary folder, which
  scoped storage forbids without a persisted SAF tree grant or `MANAGE_EXTERNAL_STORAGE`
  (opt-in power mode per CLAUDE.md, needs a Play Console declaration). Needs the storage
  access decision revisited, not a feature pass.

- **Cast** and **Online subtitle search** — no longer "blocked", they are **grilled and
  design-locked**; see the "Grilled — HELD" section above (waiting on a Chromecast and an
  OpenSubtitles API key respectively, not on design or dependencies).
- **libass** — required for full ASS/SSA *rendering fidelity* (karaoke, complex
  positioning, effects). NOT required for user styling, which shipped via
  `SubtitleView`/`CaptionStyleCompat`. Native/JNI integration, not yet started.
- **Android TV (Compose for TV)** — new UI paradigm; large, lower priority.
