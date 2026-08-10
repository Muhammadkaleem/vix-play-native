# Device driving — platform commands, stack notes, gotchas, emulator honesty

This file is institutional memory. When a run hits a new tooling trap, append it here
before the session ends — the next run should never re-diagnose it.

## Android (adb)

- Serial-scope everything when >1 target: define `a(){ adb -s <serial> "$@"; }` — the
  shell is often **zsh**, where unquoted `$var` does NOT word-split, so
  `D="adb -s X"; $D shell ...` fails with "command not found".
- Launch: `a shell am start -n <pkg>/<activity>` (find the activity via
  `a shell cmd package resolve-activity --brief <pkg>`);
  `monkey -p <pkg> -c android.intent.category.LAUNCHER 1` also works for a plain launch.
- Timed launch (perf lens): `a shell am start -W -n <pkg>/<activity>` → TotalTime.
- Interact: `a shell input tap X Y` · `input swipe x1 y1 x2 y2 [ms]` ·
  `input text "..."` (no spaces — use `%s`) · `input keyevent 4` (back) / `3` (home) /
  `66` (enter) / `85` (play-pause) / `24`/`25` (volume).
- Screenshot: `a exec-out screencap -p > shot.png`. Screen record:
  `a shell screenrecord --time-limit 30 /sdcard/rec.mp4` then `a pull`.
- Logs: `a logcat -d -t 300` for a snapshot; filter by tag/PID. React Native console.log
  → tag `ReactNativeJS`. Crashes: `a logcat -b crash -d` and
  `a shell dumpsys dropbox --print | tail -100`.
- View hierarchy: `a shell uiautomator dump && a pull /sdcard/window_dump.xml` — works
  well on **Compose** (nodes carry text/content-desc; testTags appear as resource-ids
  only if the app sets `testTagsAsResourceId`). Often fails on animated RN screens
  ("could not get idle state") — there, screenshot-and-read instead.
- Deep links / intents: `a shell am start -a android.intent.action.VIEW -d "<uri>"`;
  external-open testing: `am start -a android.intent.action.VIEW -d "file:///sdcard/x.mp4" -t video/mp4`.
- Offline: `a shell cmd connectivity airplane-mode enable|disable` (allow ~30 s for
  wifi to re-associate before expecting requests). Kill/restart:
  `a shell am force-stop <pkg>` then relaunch (process-death persistence check).
- Permissions: `a shell pm grant|revoke <pkg> <permission>`;
  `a shell appops set <pkg> <op> allow|deny` for special ops. Reset to first-run:
  `a shell pm clear <pkg>` (wipes app data — re-push fixtures after).
- Files: `a push fixture.mp4 /sdcard/Movies/` then force a MediaStore scan:
  `a shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/fixture.mp4`.
- Perf: jank `a shell dumpsys gfxinfo <pkg>` (reset with `framestats reset` between
  scenarios); memory `a shell dumpsys meminfo <pkg>` (trend it across repeated
  open/close of the heavy screen — a monotonic rise is a leak signal); battery
  `dumpsys batterystats`; jobs/workers `a shell dumpsys jobscheduler | grep -A5 <pkg>`.
- Services/sessions: `a shell dumpsys media_session` (who owns media buttons),
  `dumpsys activity services <pkg>` (foreground service alive?),
  `dumpsys notification --noredact | grep -A10 <pkg>`.

## iOS (simulator)

- `xcrun simctl launch booted <bundleid>` / `terminate booted <bundleid>` /
  `io booted screenshot shot.png` / `openurl booted <deeplink>` /
  `simctl push booted <bundleid> payload.json` (notification test) /
  `simctl privacy booted grant|revoke <service> <bundleid>` /
  `simctl status_bar booted override ...` (clean screenshots).
- Logs: `xcrun simctl spawn booted log stream --predicate 'subsystem == "<bundleid>"'`,
  or the Metro console for RN.
- No touch-coordinate injection via simctl — UI driving needs XCUITest/Maestro or
  accessibility APIs; plan iOS runs around deep links + app-side affordances, or a Mac
  with Maestro installed.

## Stack notes (detect in step 0, then apply the right block)

- **Native Android (Gradle/Compose):** build+install `./gradlew installDebug`; watch the
  Gradle log for `BUILD SUCCESSFUL/FAILED` — a backgrounded launcher can exit 0 while a
  daemon is still building. No Metro, no `adb reverse`. Release artifact: build the AAB,
  then `bundletool build-apks --mode=universal` + `install-apks` to test what ships (R8!).
- **React Native / Expo:** dev builds need Metro + `adb reverse tcp:8081 tcp:8081` over
  USB; a dev-client server picker or red screen means JS never loaded — fix that before
  "testing" anything. `console.log` → logcat tag `ReactNativeJS`.
- **Flutter:** `flutter run` for dev; `flutter build apk --release` for what ships;
  logs via `flutter logs` or logcat tag `flutter`.

## Gotchas (append-only; hard-won)

- Random `monkey` rarely navigates a real app meaningfully — drive deterministically
  (known coordinates / hierarchy / deep links); read a screenshot to get coordinates.
- `uiautomator dump` fails on animated screens ("could not get idle state") —
  screenshot-and-read instead.
- Emulator wifi re-association after airplane-mode off takes ~30 s; don't call a request
  FAIL inside that window.
- `pm clear` also revokes runtime permissions — the app is back to true first-run.
- `ContentResolver.query` sort orders are validated on modern Android — SQL smuggled
  into sortOrder (e.g. `LIMIT 1`) throws `IllegalArgumentException` at runtime, invisible
  at compile time. (Found as a startup crash in the field.)
- String-anchored code edits can silently no-op and still compile — "it compiles" says
  nothing about whether UI is actually wired into composition. Only the device knows.

## Emulator honesty — what a stock emulator CANNOT verify

Consult at **planning** time. Each item in a plan that lands here becomes BLOCKED with
an escape plan and an entry in qa/ledgers/deferred-verification.md.

| Not verifiable on emulator | Escape plan |
|---|---|
| Multi-touch (pinch/rotate) — no `adb root` on prod images; SELinux blocks `sendevent`; `input` is single-pointer | Physical-device pass |
| Play in-app review / billing / real ad fill | Play internal-track build on hardware |
| Multi-day scheduled work (WorkManager gaps, quiet hours) | Time-shift device clock where the logic allows, else soak on hardware over days |
| API levels other than the booted image (e.g. ≤28 legacy branches on an API-35 image) | Boot the matrix image that hits the branch (see qa/device-matrix.md) |
| Bluetooth audio routing, real haptics, thermal/battery behavior | Physical device |
| SMB/network shares without a reachable server | Spin up a local server (works for FTP/SMB via docker) or hardware on the LAN |
| Real camera/sensor input | Physical device or emulator virtual-scene where sufficient |
