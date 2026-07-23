# Theming

## Capability
Token-based theming with Light/Dark/AMOLED, Material You dynamic color, custom accent, player skins, ambient backglow, adaptive fullscreen dim. See screens/14-settings (Appearance).

## Shipped implementation
- `theme/Tokens.kt` — semantic `PlayerColors` + Light/Dark/AMOLED palettes.
- `theme/PlayerTheme.kt` — CompositionLocal provider, Material You mapping (API 31+), high-contrast, adaptive fullscreen dim.

## Rules
- **No hardcoded colors anywhere** — every component reads semantic tokens. This is the #1 architectural rule; retrofitting is the worst rewrite.
- Manual accent overrides dynamic/static accent.

## Skins (P2)
Recompose control layout independently of colors: Minimal (auto-hide), Classic (MX-like), Cinema (dimmed), Pro (timecode + frame-step). Control bar is a reorderable component list persisted per skin.

## Ambient backglow (P2)
In windowed playback, sample dominant edge colors from a downscaled frame at ~2–4fps and bleed a soft gradient behind the video. Crossfade on cuts; disable in fullscreen + battery saver.

## Acceptance criteria
- [ ] All base themes apply globally with no hardcoded-color leaks.
- [ ] Material You derives palette from wallpaper on Android 12+ with fallback.
- [ ] Skin switch reflows controls without restart.
