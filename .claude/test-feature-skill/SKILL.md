---
name: test-feature
description: Act as a Staff SQA engineer and verify a feature works by running the app on a simulator, emulator, or connected real device — driving the UI, capturing evidence (screenshots/logs), and checking real behavior against acceptance criteria. Use when the user wants a feature tested/QA'd on a device, an on-device smoke test, or says "test this feature", "QA this", "run it on the device/emulator/simulator".
---

# Test Feature (Staff SQA on device)

Verify a feature by **observing real behavior on a real target** — never by reading code alone.
Act as a meticulous Staff SQA engineer: reproduce, capture evidence, and report faithfully.

<hard-rules>
- Prove it on a device. A feature is "verified" only when observed running on a simulator, emulator, or physical device — with evidence (screenshot and/or log). Reading the code is not a test.
- Temporary instrumentation is allowed (extra logs, a stub endpoint, lowered timeouts) but MUST be reverted at the end. NEVER commit or push test scaffolding, log lines, or config overrides (host URLs, feature flags, lowered constants).
- Report faithfully. If a scenario could not be run, say so — never infer a pass. Separate real product bugs from environment/tooling flakiness.
- Escalate native/build/signing/toolchain blockers instead of hacking around them.
- Do not commit, push, or open PRs unless the user asks.
</hard-rules>

## Process

### 1. Establish scope (and confirm before touching a device)

Determine what to test, from whichever the user gave — accept ALL of these:
- an explicit feature/behavior description or **acceptance criteria**,
- a **ticket / PRD / issue** path (read it),
- **nothing** → infer scope from the current branch name + `git diff` / recent commits,
- an **exploratory smoke** request → plan a broad launch-and-navigate pass.

Explore the codebase to locate the feature's screens, entry points, flags, events, and network calls.

Then **restate the scope + the exact scenarios you intend to run, and get the user's OK before driving a device** (grill-me style — ask if the target device, build variant, or acceptance bar is unclear). Skip the confirmation only for a pure exploratory smoke the user already asked for.

### 2. Derive test scenarios

From the acceptance criteria + code, enumerate concrete scenarios. Cover more than the happy path:

<scenario-checklist>
- Happy path(s) — the primary flow end to end.
- State variations — empty / loading / error / success; first-run vs returning; logged-out vs logged-in.
- Edge & resilience — offline / airplane mode, then reconnect; kill + relaunch (persistence); background → foreground; slow network; permission denied.
- Navigation — back gesture / hardware back, deep links, tab switches.
- Negative — invalid input, cancel, double-tap/rapid actions.
- Data/analytics/API features — verify the request actually fires AND the server accepts it; don't trust the UI alone.
</scenario-checklist>

Prioritize the scenarios tied to the acceptance criteria; note any you consciously skip.

### 3. Select and prepare the target

- Detect available targets: `adb devices`, `xcrun simctl list devices booted`. If more than one (e.g. emulator + real device), ask which — or use the one the user named.
- Get the app running: prefer the project's own run command / a `run` skill (e.g. `expo run:ios` / `expo run:android`), or launch the already-installed build. Ensure the dev server (Metro) and `adb reverse tcp:8081 tcp:8081` if it's a dev build over USB.
- Record the **environment under test**: build variant (debug/release), and which **backend/host** the build points at. State this in the report — a test against the wrong environment is misleading.

### 4. Drive the app and capture evidence

Exercise each scenario via device automation, capturing a screenshot and/or relevant logs at each meaningful step.

<driver-cheatsheet>
Android (adb):
- launch: `adb -s <serial> shell am start -n <pkg>/<activity>` (get activity via `cmd package resolve-activity --brief <pkg>`); `monkey -p <pkg> -c android.intent.category.LAUNCHER 1` also works.
- interact: `adb shell input tap X Y` / `input text "..."` / `input keyevent 4` (back) / `3` (home).
- screenshot: `adb exec-out screencap -p > shot.png`.
- logs: `adb logcat` — RN `console.log` lands under the `ReactNativeJS` tag.
- offline: `adb shell cmd connectivity airplane-mode enable|disable` (then wait ~30s for wifi to re-associate before expecting requests to succeed).
- kill/restart: `adb shell am force-stop <pkg>` then relaunch.

iOS (simulator):
- `xcrun simctl launch booted <bundleid>` / `terminate` / `io booted screenshot shot.png` / `openurl booted <deeplink>`.
- logs: `xcrun simctl spawn booted log stream --predicate '...'`, or the Metro console.
</driver-cheatsheet>

<device-gotchas>
- The shell is **zsh**: unquoted `$var` does NOT word-split. `D="adb -s X"; $D shell ...` fails ("command not found"). Use a function: `a(){ adb -s X "$@"; }`.
- Random `monkey` rarely navigates a real RN app meaningfully — its taps miss nav targets. Drive **deterministically** (known coordinates / tab bar / deep links); read a screenshot to get coordinates.
- `uiautomator dump` often fails on animated RN screens ("could not get idle state") — take a **screenshot and read it** instead of dumping the hierarchy.
- A backgrounded build launcher can report **exit 0 prematurely** while a Gradle/Xcode daemon keeps building — watch the build log for `BUILD SUCCESSFUL/FAILED`, not just the launcher's exit.
- If the app shows the dev-client server picker or a red screen, it hasn't loaded JS — connect Metro (tap the server / `adb reverse`) before testing.
</device-gotchas>

### 5. Verify each scenario

For every scenario, compare **observed** behavior (screenshot/log/network) to **expected**. Mark PASS / FAIL / BLOCKED. For a FAIL, capture the exact evidence and minimal repro steps. For anything network/analytics-related, confirm at the transport layer (request sent, server response), not just the screen.

### 6. Report

Produce a report the user can forward:

<report-template>
## QA Report — <feature> on <device/OS> (<build variant>, backend: <host>)

**Verdict:** ✅ Ready / ⚠️ Ready with issues / ❌ Not ready

| # | Scenario | Result | Evidence / Notes |
|---|----------|--------|------------------|
| 1 | <happy path> | ✅ PASS | <screenshot/log ref> |
| 2 | <offline → reconnect> | ❌ FAIL | <what happened + repro> |
| 3 | <kill/restart> | ⚠️ BLOCKED | <why it couldn't run> |

**Bugs found:** product defects with repro steps.
**Not tested / blocked:** scenarios skipped, and why.
**Environment/tooling notes:** flakiness or blockers that are NOT product bugs (device drops, build/signing, Metro state) — clearly separated from real bugs.
</report-template>

### 7. Clean up

- Revert every temporary change made for testing (instrumentation, stubbed endpoints, lowered constants, host overrides). Confirm `git status` is clean of test scaffolding.
- Restore device state (airplane mode off, etc.).
- Do not commit/push anything unless the user explicitly asked.
