# Roadmap & MVP Cut

## Build order (dependencies first)
1. **Foundations** — design-system tokens + `PlayerTheme`, `GestureDispatcher` + dispatcher unit tests, Room schema, scoped-storage access layer.
2. **Playable core** — Media3 + FFmpeg extension, folder browser, video player with default gestures, resume position.
3. **Subtitles** — SRT/ASS via libass, external load, sync offset.
4. **Feel differentiators** — precision seek + thumbnails, variable-speed hold, haptics.
5. **Modern shell** — Material You theming, AMOLED, clean video library home, audio player + background service.
6. **Reach** — network streaming, Chromecast, PiP, equalizer.
7. **Polish/retention** — gesture remap UI, subtitle online search, playlists, private folder, ambient backglow, Android TV.

## MVP (P0 only) — shippable baseline
Video library + folder browser · Media3+FFmpeg local playback with HW/SW · default gestures + gesture lock · SRT/ASS subtitles + sync · resume · zoom modes · speed control · background audio + media notification · Light/Dark themes · basic settings · free tier + Pro unlock.

## Rough sizing
- MVP: ~2–3 months, 1–2 strong Android engineers.
- Full P0+P1+P2: ~6–12 months, small team. Scope ruthlessly per release.

## Release train
- **0.1 Alpha** — MVP internal.
- **0.5 Beta** — + subtitles online, precision seek, theming, casting. Closed testing track.
- **1.0** — + PiP, equalizer, playlists, TV, private folder, ambient. Production.
