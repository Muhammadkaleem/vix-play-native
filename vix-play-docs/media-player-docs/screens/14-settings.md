# Settings — PRD

**Route:** `/settings` · **Priority:** P0
**Entry:** Settings tab / player overflow.

## Purpose
Central configuration. Grouped, searchable, with sensible defaults so most users never need it.

## Sections
- **Playback:** default decoder (HW/SW/auto), resume behavior, autoplay next, default speed, default zoom, background audio, hardware acceleration, seek step sizes.
- **Gestures:** open Gesture Remap (see settings/GestureRemapScreen), presets, sensitivity, haptics, gesture lock, edge exclusion.
- **Appearance:** base theme (Light/Dark/AMOLED/Dynamic/High-contrast), custom accent, player skin, ambient backglow, adaptive fullscreen dim, auto-theme by time.
- **Subtitles:** default enable, language priority, style preset + custom, encoding default, online-search account.
- **Audio:** equalizer, audio output, gapless, crossfade, audio delay default.
- **Library:** storage locations, included/excluded folders, rescan, thumbnail quality, show hidden.
- **Privacy & Lock:** app lock, private folder, incognito, clear history.
- **Network:** saved servers, streaming buffer size, cast options.
- **About / Pro:** upgrade, restore purchase, licenses (FFmpeg/libass/OSS), version, feedback.

## States
Default · Modified (persist immediately) · Search-filtered.

## Edge cases
- Settings that require permission (all-files) → inline prompt.
- Reset-to-defaults with confirmation.
- Import/export settings profile (P2).

## Acceptance criteria
- [ ] Every setting persists (DataStore) and takes effect without restart where feasible.
- [ ] Settings search finds any option.
- [ ] OSS license attributions present (compliance).
