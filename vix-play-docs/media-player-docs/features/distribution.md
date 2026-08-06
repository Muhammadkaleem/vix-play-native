# Distribution & Open-With

## Capability
Make VixPlay reachable as the user's real media player: appear in the Android "Open with" chooser for video/audio files and shared media, offer to become the default player, and prompt happy users for a Play review. Without this the app is a walled garden — content only plays if browsed from inside it (`product/05-growth-foundation.md`, Workstream A).

## Intent handling
- `ACTION_VIEW` — register intent filters for common video/audio MIME types and file extensions (mp4, mkv, avi, mov, webm, m4v, mp3, flac, m4a, …) plus `file`/`content` schemes, so VixPlay lists in the system chooser.
- `ACTION_SEND` (+ `SEND_MULTIPLE`) — accept media shared from other apps.
- External intents map to the existing player deep-link URI (Navigation Compose `@Serializable` destinations). External entries start at position 0 with no persisted resume when the URI is not a MediaStore item (the documented degradation the network-streaming path already uses).
- "Set as default player" — **Android has no `RoleManager` role for a media/video player** (roles are a fixed platform set: Browser, Dialer, SMS, Home, Assistant, …), so there is nothing to claim programmatically. The real mechanism is the per-file-type "Open by default" association the user controls: being in the "Open with" chooser (above) lets them tap **Always**, and a Settings row deep-links to the system screen via `Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS` (API 31+, `package:` uri), falling back to `ACTION_APPLICATION_DETAILS_SETTINGS` below 31. Never auto-claims anything.

## Review prompt
- Google Play In-App Review (`ReviewManager`), triggered only after a genuine positive signal (e.g. ≥3 completed playbacks, never on error/first-run), rate-limited by the API itself. Fire `review_prompt_shown` (`analytics.md`).

## Permission flow
- Fix the current double-request (permission asked in both splash and library) — splash owns it. Handle Android 14 (API 34) **partial media access** (`READ_MEDIA_VISUAL_USER_SELECTED`) with a "manage selection / grant all" path so a partial grant is a first-class state, not a silent empty library.

## Edge cases
- Unsupported codec from an external intent surfaces the existing codec-name error overlay + `codec_error` event, never a blank screen.
- A `content://` URI without persistable permission still plays for the session; it just isn't added to resume history.
- Declining the default-player role must not degrade normal in-app playback.

## Acceptance criteria
- [ ] VixPlay appears in "Open with" for the declared types and plays the handed-off URI full-screen.
- [ ] Shared media (`ACTION_SEND`) opens and plays.
- [ ] "Set as default player" opens the system Open-by-default screen (31+) / app details (below) and never crashes on OEM builds without the exact screen.
- [ ] Partial-media grant (API 34) yields a usable, clearly-labeled library, not an empty one.
- [ ] Review prompt only fires on a positive signal and is instrumented.
