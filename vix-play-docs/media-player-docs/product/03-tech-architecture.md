# Technical Architecture

## Stack
| Concern | Choice |
|---|---|
| Language / UI | Kotlin + Jetpack Compose |
| Player engine | Media3 (ExoPlayer) + `media3-decoder-ffmpeg` extension |
| Subtitles | Media3 text renderers + libass (native) for ASS/SSA + PGS decoder |
| DI | Hilt |
| Async | Coroutines + Flow |
| Local DB | Room |
| Key-value / prefs | DataStore (settings) + MMKV (hot paths like resume positions) |
| Images/thumbnails | Coil + custom sprite-sheet provider |
| Casting | Cast SDK (Chromecast) + Cling/jUPnP (DLNA) |
| Networking | OkHttp; SMBJ for SMB; commons-net for FTP |
| Analytics/crash | Firebase Crashlytics + a privacy-respecting analytics layer |
| Billing | Google Play Billing v6 |

## Module structure (multi-module Gradle)
```
:app                     # Entry, navigation host, DI wiring
:core:designsystem       # Theme tokens, PlayerTheme, components, skins
:core:common             # Utils, Result types, coroutine dispatchers
:core:data               # Room, DataStore, repositories
:core:model              # Domain models
:feature:player          # Video player, gestures, HUD, controller
:feature:audioplayer     # Audio player + equalizer + background service
:feature:library         # Video/audio library, folder browser, scanning
:feature:subtitles       # libass render, online search, styling
:feature:streaming       # Network URL, SMB, FTP, cast/DLNA
:feature:settings        # Settings + gesture remap + theme picker
:feature:tv              # Android TV (Leanback) surfaces
```

## Package layout inside :feature:player (already scaffolded)
```
com.yourapp.player
├── gesture/     GestureModels, GestureDispatcher, PlayerCommandSink, PlayerGestureModifier
├── playback/    PlayerController (implements sink), PlayerHost, Media3 setup
├── thumbnail/   ThumbnailProvider (sprite-sheet scrub previews)
├── theme/       Tokens, PlayerTheme
├── ui/          PlayerHudOverlay, control bar, skins
└── settings/    GestureRemapScreen
```

## Data flow
```
UI (Compose) ──intent──> ViewModel ──> Repository ──> Room / DataStore / Media3
     ^                                     |
     └────────── StateFlow <───────────────┘
```
- ViewModels expose immutable UI state via `StateFlow`.
- The player layer is decoupled: gestures emit intents through `PlayerCommandSink`; `PlayerController` applies them to ExoPlayer/AudioManager/window.

## Threading
- Decoding on ExoPlayer's internal threads.
- Scanning, thumbnailing, subtitle extraction on `Dispatchers.IO`.
- Never block the main thread; gesture math runs in the pointer input scope, effects applied on main.

## Background & foreground services
- `MediaSessionService` (Media3) for background audio + notification + lock screen.
- Foreground service type `mediaPlayback`.
- Headless work (scanning) via WorkManager.
