# Scenario design — risk-based derivation, impact analysis, transport verification

## 1. Start from risk, not from a checklist

Generic checklists spend effort evenly; bugs don't distribute evenly. Read
`qa/risk-map.md` first — it records where this codebase's defects actually cluster —
and weight scenario count toward the intersection of *changed* and *historically fragile*.

Mobile risk catalog (universal starting weights; the repo's risk-map overrides):

- **API-level branches.** Any code with `SDK_INT` checks has one scenario per branch,
  and each branch needs a device image that actually reaches it. A branch nobody can
  boot is a deferred-verification entry, not a silent skip.
- **Permission & consent flows.** Grant / deny / revoke-while-running / partial grants
  (Android 14 partial media) / system consent dialogs (MediaStore write/delete/trash
  IntentSenders). Denial paths break more often than grant paths.
- **Wired-but-dead UI.** State machines and ViewModels can exist, compile, and never be
  rendered. For every new control: tap it and observe the effect. (A whole file-ops
  dialog layer once shipped compiled-but-never-inserted.)
- **Lifecycle & process death.** Background→foreground, rotation, `am force-stop` +
  relaunch (persistence), service survives app-swipe when it should, dies when it should.
- **Data migrations.** Any schema/preferences change ⇒ upgrade-path scenario (old build →
  real data → new build → data intact). See release-certification.md.
- **Concurrency & interruption.** Double-tap, rapid repeated actions, cancel mid-operation,
  interruptions mid-flow (call/notification), slow network, offline→reconnect.
- **External entry points.** Deep links, VIEW/SEND intents with hostile inputs: wrong MIME,
  malformed URIs, missing extras, files the app can't read.
- **Boundary data.** Empty library, single item, huge list, zero-duration/corrupt media,
  unicode/emoji filenames, exactly-at-threshold values.

## 2. Derivation method (feature mode)

1. Read the acceptance criteria / PRD / diff; locate screens, entry points, flags, events,
   network calls in code.
2. Enumerate: happy path(s) → state variations (empty/loading/error/success; first-run vs
   returning; logged-out vs in) → risk-catalog hits for the touched areas → negatives →
   one exploratory slot (10 min of "try to break it" — scripted scenarios never find
   everything).
3. For each scenario write: id, preconditions (fixtures!), numbered steps, expected
   observable evidence (what screenshot/log proves it), device/API requirement.
4. Check every scenario against the emulator-honesty table (device-driving.md). Mark the
   unreachable ones BLOCKED-planned with escape plans now.
5. Prioritize: acceptance-criteria scenarios first, then highest-risk, then nice-to-have.
   State explicitly which you'll skip if time runs short.

## 3. Impact analysis (pr mode)

1. `git diff --stat <base>...HEAD` → changed files.
2. Map files → owning areas via qa/risk-map.md (maintain the mapping as areas are added).
3. Select: all regression cases tagged with those areas + the smoke suite + any case
   whose last FAIL touched these files.
4. New behavior in the diff without a covering case → write the case first, then run it.
   (That's how the suite accrues without a backfill project.)

## 4. Transport-layer verification (API & analytics lens)

UI success proves rendering, not integration. For anything that claims to talk to a
backend or emit analytics, verify at the wire during the same device session:

- **App logs first**: most stacks log requests in debug (OkHttp logging interceptor,
  RN network inspector). If the app lacks one, adding a temporary interceptor is
  legitimate instrumentation — revert it after.
- **Firebase Analytics**: `adb shell setprop debug.firebase.analytics.app <pkg>` then
  watch DebugView / `logcat -s FA FA-SVC` for the event name + params. Verify the event
  fires once (not 0, not 2) and params match the tracking plan.
- **Crashlytics**: force a test crash on a debug build; confirm arrival in the console.
- **Proxy when logs aren't enough**: mitmproxy on the host + emulator proxy
  (`emulator -http-proxy`), with the debug build trusting user CAs
  (networkSecurityConfig). Assert request shape AND server response code.
- **Contract checks**: for critical endpoints, assert response schema (curl the endpoint
  with the app's parameters) — the server accepting the request is not the server
  understanding it.
- Verdict rule: an analytics/API scenario passes only when the transport evidence is in
  the report. "The screen looked right" is not evidence for this lens.

## 5. UX / a11y / perf lenses (same session, capped cost)

- **UX lens**: while driving, note friction — dead-feeling taps, jank, inconsistent
  vocabulary, truncation, layout breaks in landscape/small screens. Severity cap P3
  unless a flow is genuinely blocked. Taste goes in the report, not the gate.
- **A11y segment** (one per T1 on a touched screen, full pass at T4): enable TalkBack
  (`a shell settings put secure enabled_accessibility_services
  com.google.android.marvell...` varies — prefer toggling via UI), traverse the screen;
  every interactive element must announce a meaningful label; check touch targets ≥48 dp
  and Light-theme contrast on changed screens. Static complement lives in the sweeper.
- **Perf lens** (cheap, every T1): timed cold start (`am start -W`), the feature's key
  latency (e.g. tap-to-first-frame), one `gfxinfo` snapshot after the heaviest scenario,
  `meminfo` before/after repeated open-close of the new screen. Record numbers in the
  report even without budgets — trends need baselines. Budgets live in quality-gates.md.
