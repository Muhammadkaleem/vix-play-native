# Permissions & Scoped Storage

The single decision that shapes the whole file-access UX.

## The problem
Android 10+ enforces scoped storage. A media player needs broad read access to browse arbitrary folders — but broad access is exactly what Google scrutinizes.

## Options
1. **`MANAGE_EXTERNAL_STORAGE` (All files access)**
   - Pro: MX-like free-roaming file browser; simple `File` APIs; USB/SD easy.
   - Con: Play Console requires a **declaration + review**; must justify as a file manager / media player. Risk of rejection if justification is weak.
2. **Storage Access Framework (SAF)**
   - Pro: No special-access review; user grants tree access per location.
   - Con: `DocumentFile` is slower; UX friction (user must pick trees); some flows clunkier.
3. **`READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (Android 13+) + MediaStore**
   - Pro: Clean, granular, no special review.
   - Con: MediaStore only — misses files the scanner ignores; less "browse anywhere."

## Recommendation
- **Primary:** MediaStore granular media permissions (13+) / legacy read (≤12) for the library.
- **Folder browsing / network / arbitrary files:** SAF tree access, cached listings.
- **Offer `MANAGE_EXTERNAL_STORAGE` as an opt-in "power mode"** with in-app rationale for users who want a full file browser — and prepare the Play Console declaration. Don't make it mandatory.
- Android 14+ partial media selection: handle "manage selection" gracefully.

## Other permissions
- `POST_NOTIFICATIONS` (13+) for playback controls.
- Foreground service `mediaPlayback`.
- `INTERNET` + network state for streaming/cast.
- USB host for OTG.

## Acceptance criteria
- [ ] App is fully usable without all-files access (SAF/MediaStore paths).
- [ ] All-files access is opt-in with a compliant Play declaration.
- [ ] Partial media access (14+) handled.
