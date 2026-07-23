# Player — Design Assets

Everything visual for the media player app in one place: the launcher icon, the in-app icon set, the color theme, and the typography system. Built around one identity — the **Orbit** mark and an **indigo→violet** palette on a dark, AMOLED-friendly canvas.

## Contents
```
player-design-assets/
├── launcher-icon/     Orbit adaptive launcher icon (drop-in res/ + Play Store 512 + source)
├── icons/             52 in-app UI icons (transparent, currentColor) + 12 accent variants + sheet
├── theme/             color tokens: colors.md, colors.xml, Color.kt, theme-swatches.svg
├── typography/        type system: typography.md, Type.kt, type-scale.svg, fonts/
└── concepts/          exploration boards + screen mockups (reference only)
```

## Quick start for a developer
1. **Launcher icon** — merge `launcher-icon/res/` into `app/src/main/res/`. See `launcher-icon/README.md`.
2. **In-app icons** — convert `icons/svg/*.svg` to VectorDrawables; tint at runtime with the theme accent (they're `currentColor`). See `icons/README.md`.
3. **Colors** — drop `theme/Color.kt` into `core:designsystem` (or use `theme/colors.xml`).
4. **Typography** — drop `theme/../typography/Type.kt` in, add the fonts per `typography/fonts/README.md`.

## Design principles
- **One identity:** Orbit mark, indigo→violet accent (`#6D5BFF → #B24BFF`), dark surfaces.
- **Player-first, calm:** minimal chrome, no clutter; hierarchy from size/weight/color, not decoration.
- **Token-driven:** components read semantic roles (accent, surface, contentPrimary), so Light / Dark / AMOLED / Material You all work from the same components.
- **Consistent icons:** 24×24 grid, 2px stroke, round joins. Keep these specs for any new icon.

## Brand quick reference
- Gradient: `#6D5BFF → #B24BFF`
- Dark bg: `#121316` · AMOLED bg: `#000000`
- Content: `#F2F3F5` / secondary `#A0A3AD`
- Fonts: Inter (UI) · JetBrains Mono (timecodes)

*The `concepts/` folder is exploration/mockup reference — not production assets.*
