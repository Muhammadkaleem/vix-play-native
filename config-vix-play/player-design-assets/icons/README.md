# In-App Icon Set

52 UI icons for the media player, designed as one consistent system: 24×24 canvas, 2px stroke, round caps/joins, transparent background.

## Folders
- `svg/` — **the source of truth.** Each icon uses `stroke="currentColor"` (and `fill="currentColor"` on solid parts), so it inherits whatever color you set in the UI. One file works for every theme, every state.
- `accent/` — indigo→violet gradient versions of 12 primary/active icons (play, pause, cast, subtitles, equalizer, favorite, the 4 nav items, settings, download, add). Use these for selected/active states if you don't want to tint at runtime.
- `icon-sheet.svg` — reference contact sheet of the whole set with names.

## Using in Android (recommended)
Convert each SVG to a `VectorDrawable` (Android Studio: right-click `res/drawable` → New → Vector Asset → Local file). Because they're `currentColor`, tint them in code/XML instead of shipping color copies:

```xml
<ImageView
    android:src="@drawable/ic_play"
    app:tint="?attr/colorPrimary" />   <!-- or your theme token -->
```
In Compose:
```kotlin
Icon(painterResource(R.drawable.ic_play), null, tint = PlayerTheme.colors.accent)
```

## Variations available
- **Default (outline + solid):** `svg/` in `currentColor` — tint to primary, secondary, disabled, white, etc.
- **Accent gradient:** `accent/` — pre-colored violet for active states.
- **Monochrome / muted / white:** no separate files needed — just tint the `svg/` version (that's the point of `currentColor`).

## Icon inventory
Playback: play, pause, stop, next, previous, forward, rewind, shuffle, repeat, repeat-one, speed
Controls: subtitles, audio-track, volume, mute, brightness, fullscreen, aspect, rotate, lock, pip, cast, screenshot, equalizer, timer
Navigation: nav-video, nav-audio, nav-network, settings, search, back, more-vert, sort, filter, grid, list, close, check, chevron-right, chevron-down
Library: folder, playlist, history, favorite, download, share, delete, add, info, edit, eye, eye-off

## Consistency notes
- Keep the 2px stroke and 24×24 grid if you add more icons, so the set stays uniform.
- For a "filled/active" look on any outline icon, tint it to the accent and add a subtle background chip rather than making a separate filled file.
