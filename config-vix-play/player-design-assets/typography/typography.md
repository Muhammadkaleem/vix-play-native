# Typography

## Fonts
| Use | Font | Why | License |
|---|---|---|---|
| UI (everything) | **Inter** | Clean, modern, superb at small sizes; huge weight range | SIL OFL 1.1 (free to bundle) |
| Timecodes / Pro scrubber | **JetBrains Mono** | Fixed-width digits so `01:23:45` doesn't jitter while playing | SIL OFL 1.1 |

Both are open-licensed and safe to ship inside the app. Download links + install steps in `fonts/README.md`.

## Type scale (sp)
| Token | Size | Weight | Line ht | Use |
|---|---|---|---|---|
| displayLarge | 28 | 700 | 34 | Brand / large screen headers |
| headlineSmall | 22 | 700 | 28 | Screen title in app bar ("Player", "Gestures") |
| titleMedium | 16 | 600 | 22 | Section headers ("Continue watching") |
| titleSmall | 14 | 600 | 20 | Card titles, list item titles |
| bodyLarge | 15 | 400 | 22 | Primary reading text |
| bodyMedium | 14 | 400 | 20 | Default body / descriptions |
| bodySmall | 12 | 400 | 16 | Metadata (duration, size, path) |
| labelLarge | 14 | 500 | 18 | Buttons, chips |
| labelSmall | 11 | 500 | 14 | Bottom-nav labels, badges |
| timecode | 13 | 500 (mono) | 16 | Player current/total time, HUD deltas |

## Rules
- One UI typeface (Inter). Don't mix in a second display face — hierarchy comes from size/weight/color, not more fonts.
- Secondary text uses `contentSecondary`, never a lighter font weight, to keep contrast predictable.
- Timecodes and any live-updating numbers use the mono family so digits don't shift width.
- Keep to this scale; avoid one-off sizes.

## Files here
- `Type.kt` — Compose `Typography` + font families (drop into `core:designsystem`).
- `type-scale.svg` — visual specimen.
- `fonts/README.md` — where to get the fonts and how to add them to `res/font`.
