# VixPlay — App Screen Audit (ASO)

_Generated for Google Play promotional-asset work. Source of truth: the running
code in this repo + the design-implementation log in `CLAUDE.md`._

## Project facts (verified from the codebase)

| Concern | Finding |
|---|---|
| Package | `com.devbytes.vixplayer.app` |
| Platform | Native Android (Kotlin) |
| Build | Gradle (`app/build.gradle.kts`), `versionName 3.0.8` / `versionCode 68` |
| UI framework | **Jetpack Compose only** (no XML layouts, no Fragments; one `MainActivity` + `VixPlayNavGraph` with `@Composable` destinations). Confirmed: no `res/layout/*`, UI is `ui/**/*Screen.kt`. |
| Player | **Media3 / ExoPlayer** (`media3-exoplayer` + `-ui` + `-session` + `-hls/-dash/-rtsp`). App-scoped `PlayerController` (`@Singleton`) + `PlaybackService` (`MediaSessionService`). FFmpeg extension is planned, **not yet in the build** (so AC3/DTS/exotic codecs are not guaranteed — see "formats" caveat). |
| Media index | MediaStore (video + audio) via `MediaRepository` / `AudioRepository`; resume positions in MMKV (fast path) + Room; favorites/history/EQ/playlists/network-recents in Room. |
| Images | Coil + `VideoFrameDecoder` (thumbnails decoded from video frames). |
| Nav | Navigation-Compose, bottom tabs: **Home · Video · Audio · Network · Settings**; full-screen routes for player / audio player / shorts / splash. |
| Theme | Dark-first, indigo→violet brand (`#6D5BFF → #B24BFF`), bg `#121316`, AMOLED `#000000`, content `#F2F3F5`. Light / AMOLED / Dynamic (Material You) all supported & verified. Fonts: **Inter** (UI) + **JetBrains Mono** (timecodes), bundled in `res/font/`. |
| Screenshot tests | **None present** (`app/src/androidTest` has only the default `ExampleInstrumentedTest`). No Paparazzi/Shot/screenshot-testing tooling configured. ASO capture tooling is net-new (this workstream). |

## Feature / screen classification

> **ASO rule applied:** only **production-ready** features are advertised. Anything
> "requires verification" is held back from the store listing until confirmed on a device.

| Screen / feature | Status | Evidence | Advertise? |
|---|---|---|---|
| **Dashboard (Home)** | ✅ Production-ready | `ui/dashboard/DashboardScreen.kt`; device-verified repeatedly (this session). Continue Watching hero, section rails, library stats. | Yes |
| **Video library / formats** | ✅ Production-ready | Folder browser + video grid (`VideoCard`, `VideoFrameDecoder` thumbnails); device-verified. Plays what MediaStore indexes + Media3's built-in + HLS/DASH/RTSP. | Yes — **formats caveat below** |
| **Video player** | ✅ Production-ready | `ui/player/PlayerScreen.kt`, Media3 surface, resume, HUD, options panel; device-verified. | Yes |
| **Video folders** | ✅ Production-ready | `ui/library/FolderBrowserScreen.kt`; folder list → in-folder grid; device-verified. | Yes |
| **Continue Watching** | ✅ Production-ready | Dashboard carousel + MMKV/Room resume positions; device-verified (this session). | Yes |
| **Subtitles** | ✅ Production-ready (core) | `SubtitleSheet` track select + external SAF load + per-file sync offset + styling presets. SRT/VTT/basic SSA via Media3. _Full ASS fidelity (libass) + online search = fast-follow, not shipped._ | Yes (show selection UI / synced subs) |
| **Playback controls / gestures** | ✅ Production-ready | Volume/brightness vertical drags, double-tap ±10s, drag-seek with thumbnail preview, long-press variable speed, pinch-to-zoom + pan; options panel (speed/aspect/repeat/sleep/lock/PiP…). device-verified. | Yes |
| **Audio library + audio player** | ⚠️ Implemented, **requires verification** | `ui/audio/*` (Tracks/Albums/Artists/Folders/Playlists, mini-player, audio player, EQ). Built + compiles; CLAUDE.md marks it **"not yet device-verified"**. | **Hold** until device-confirmed |
| **Picture-in-Picture** | ⚠️ Implemented, **requires verification** | `PipController` + `MainActivity` PiP hooks; auto-enter + play/pause RemoteAction built but **not fully device-exercised** per CLAUDE.md. | **Hold** until device-confirmed |
| **Network streaming (URL/HLS)** | ✅ Production-ready (URL slice) | HLS playback device-verified; SMB/FTP/Cast = "Coming soon". | Optional (not in the 6 ASO shots) |
| **Vix Pro Rewards (ads→Pro)** | ✅ Shipped this session | Rewarded-ad entitlement. **Monetization — deliberately NOT featured in ASO shots** (lead with the player, not ads). | No |

### Formats caveat (must respect in Screenshot 02 copy)
The app plays whatever **MediaStore + Media3's built-in decoders** handle (very commonly
**MP4/H.264, MKV/H.265, WebM, 3GP**, plus **HLS/DASH/RTSP** streams). **FFmpeg extension is
not in the build yet**, and **AC3/DTS are deliberately omitted** (licensing). So:
- ✅ Safe to claim: **MP4, MKV, WebM** and "and more".
- ⚠️ **AVI** (the prompt's suggested copy) is _not guaranteed_ without the FFmpeg extension — **verify on a device before putting "AVI" in the screenshot**, or use "MP4, MKV, WebM and more".

## The six ASO screens → where each is captured

| # | ASO screen | Real app surface to capture | Production-ready? |
|---|---|---|---|
| 01 | Offline Video Player | `PlayerScreen` playing a (safe sample) clip, HUD visible | ✅ |
| 02 | Video format support | Video library grid / folder contents | ✅ (formats caveat) |
| 03 | Subtitle support | Player with subtitles on, **or** the `SubtitleSheet` selection UI | ✅ |
| 04 | Continue Watching | Dashboard Continue Watching hero with real progress | ✅ |
| 05 | Advanced controls / gestures | Player with the options panel or a gesture HUD (brightness/volume/speed) | ✅ |
| 06 | Video folders | `FolderBrowserScreen` folder list | ✅ |

All six map to production-ready surfaces → **all six are advertisable.** Audio + PiP are
excluded from the set (correctly) until verified.

## ⚠️ Privacy gate on capture source (blocking decision)

The only connected device holds the **owner's personal video library** (family clips,
WhatsApp videos, an "Engagement" folder, etc.). The library / folders / Continue-Watching
screens render that content directly. **Publishing those to a public Play Store listing would
expose private personal media** — which this prompt explicitly forbids ("Do not include private
user information") and which is hard to reverse once live.

**Resolution (pending user choice):** capture the six screens against **safe sample media**
(original/CC0 clips seeded on a clean profile or emulator), not the owner's real library.
The capture script + composition pipeline are ready; the authentic captures are gated on this
choice. See `SCREENSHOT_GENERATION_REPORT.md`.
