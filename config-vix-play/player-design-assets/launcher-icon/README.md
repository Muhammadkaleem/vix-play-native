# Orbit — Android Launcher Icon (drop-in)

Production-ready adaptive launcher icon for the media player. Vector-based (crisp at every density) with PNG fallbacks for pre-Android-8 devices and a Play Store hi-res icon.

## Install
Copy the contents of `res/` into your app module's `src/main/res/`, merging the folders:

```
res/drawable/ic_launcher_background.xml
res/drawable/ic_launcher_foreground.xml
res/drawable/ic_launcher_monochrome.xml
res/mipmap-anydpi-v26/ic_launcher.xml
res/mipmap-anydpi-v26/ic_launcher_round.xml
res/mipmap-{mdpi..xxxhdpi}/ic_launcher.png        # legacy fallback (API < 26)
res/mipmap-{mdpi..xxxhdpi}/ic_launcher_round.png
```

Then point your manifest at it (usually already set):

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ... >
```

## What each piece is
- **background / foreground / monochrome** — the three adaptive-icon layers on the 108dp canvas. Key art sits inside the 72dp safe zone, so no mask (circle/squircle/rounded) clips it.
- **monochrome** — enables Android 13+ *themed icons* (system tints it to match the wallpaper). Already wired in `ic_launcher.xml` via `<monochrome>`.
- **mipmap PNGs** — shown only on Android 7 and below, which don't support adaptive/vector mipmaps.
- **playstore/ic_launcher-web-512.png** — the 512×512 hi-res icon for the Play Console listing (upload separately; not bundled in the APK).

## Colors
- Background: #16171F → #0E0F14 (dark, AMOLED-friendly)
- Play + dot: #6D5BFF → #B24BFF (indigo → violet)
- Orbit ring: #4A4560 (muted)

## Editing
`source/orbit-square.svg` and `source/orbit-rounded.svg` are the editable masters (108-unit coordinate space, 1:1 with the vector drawables). Re-run rasterization if you change them.

## Notes
- The Play Store 512 is full-bleed (square); the device renders the adaptive layers and the launcher applies its own mask.
- If you want a lighter icon for a light-themed launcher, swap the background gradient in `ic_launcher_background.xml` to a light pair and change the play/orbit fills to the gradient.
