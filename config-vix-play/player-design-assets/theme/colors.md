# Color Theme

Semantic token palette. Components read **roles** (accent, surface, contentPrimary…), never raw hex — so themes are swappable. Brand identity is indigo→violet, matching the Orbit launcher icon.

## Brand gradient
`#6D5BFF → #B24BFF` (used on the icon, active states, scrubber fill, primary buttons).

## Dark (default)
| Role | Hex |
|---|---|
| background | #121316 |
| surface (cards) | #16171C |
| sheet (menus/dialogs) | #1C1D22 |
| overlay (controls/HUD) | #1A1B1F |
| scrim | #000000 @ 60% |
| contentPrimary | #F2F3F5 |
| contentSecondary | #A0A3AD |
| contentDisabled | #565863 |
| accent | #8B5BFF |
| accentPressed | #6D5BFF |
| accentSubtle | #6D5BFF @ 20% |
| onAccent | #FFFFFF |
| scrubberTrack | #FFFFFF @ 20% |
| scrubberFill | #8B5BFF |
| scrubberBuffer | #FFFFFF @ 33% |
| scrubberThumb | #FFFFFF |
| success / warning / error | #3FBE72 / #F0B455 / #FF6369 |

## AMOLED (pure black)
Same as Dark, overriding: background #000000 · sheet #0A0A0A · overlay #000000 @ 80%.

## Light
| Role | Hex |
|---|---|
| background | #F7F7F8 |
| surface | #FFFFFF |
| sheet | #FFFFFF |
| contentPrimary | #14151A |
| contentSecondary | #5B5E6B |
| contentDisabled | #B0B3BD |
| accent | #7A4BFF |
| onAccent | #FFFFFF |
| scrubberTrack | #D5D7DE |

## Files here
- `colors.xml` — Android `res/values` colors.
- `Color.kt` — Compose semantic tokens (drop into `core:designsystem`).
- `theme-swatches.svg` — visual reference.
