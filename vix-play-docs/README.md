# Media Player — Product Requirements Documentation

Production-ready Android video & audio player. Positioned as an advanced, modern alternative to MX Player: same codec breadth, but a clean player-first experience, fully customizable gestures, modern theming (Material You, AMOLED, ambient backglow), and AI-assisted subtitles.

## How to read these docs

| Folder | Contents |
|---|---|
| `product/` | Vision, scope, the full feature matrix with priorities, tech architecture, and the MVP roadmap. **Start here.** |
| `screens/` | One PRD per screen. Each follows the same template: purpose, layout, states, interactions, edge cases, acceptance criteria. |
| `features/` | Cross-cutting feature specs (playback engine, subtitles, gestures, theming, streaming, storage). |
| `engineering/` | Data models, scoped-storage strategy, the licensing/patent plan, and the QA + release checklist. |

## Platform

- **Native Android**, Kotlin + Jetpack Compose
- **Media3 (ExoPlayer)** core + **FFmpeg extension** for codec breadth
- Min SDK 24 (Android 7) · Target latest · Android TV (Leanback) supported

## Priority legend

- **P0** — MVP. The app is not shippable without it.
- **P1** — Fast-follow. Needed to be competitive with MX Player.
- **P2** — Differentiators / delight. What makes this "advanced."
