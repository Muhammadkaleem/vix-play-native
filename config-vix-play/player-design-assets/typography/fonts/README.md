# Fonts

The app uses two open-licensed (SIL OFL 1.1) typefaces. They are **not bundled here** — download the official files and drop them in `res/font/`.

## Inter (UI)
- Source: https://rsms.me/inter/  or  https://fonts.google.com/specimen/Inter
- Add these weights to `res/font/` (lowercase, no hyphens for Android resource names):
  - `inter_regular.ttf` (400)
  - `inter_medium.ttf` (500)
  - `inter_semibold.ttf` (600)
  - `inter_bold.ttf` (700)

## JetBrains Mono (timecodes)
- Source: https://www.jetbrains.com/lp/mono/  or  https://fonts.google.com/specimen/JetBrains+Mono
- Add: `jetbrainsmono_medium.ttf` (500)

## Wire up
Uncomment the `FontFamily` blocks in `../Type.kt` and point them at these files.
Alternatively use Google Fonts downloadable fonts (`androidx.compose.ui.text.googlefonts`) to avoid bundling.

## License
Both are SIL OFL 1.1 — free for commercial use and safe to embed in the APK. Keep the OFL license text with the font files and list them on your in-app "Open source licenses" screen.
