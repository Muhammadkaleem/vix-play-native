---
name: mobile-qa
description: Staff-level mobile QA orchestration — validate features, PRs, regressions, release candidates, and full app certification for Android/iOS apps by running them on a simulator, emulator, or real device and judging real observed behavior against explicit quality gates. Use whenever the user wants anything tested, QA'd, verified, regression-checked, smoke-tested, or judged ready to ship — "test this feature", "QA this", "run it on the device/emulator", "run regression", "validate this PR/build/RC", "is this ready for release?", "certify the build", "sign off on the release", "smoke test", "did we break anything?" — even if they don't say the word "test". Also use to bootstrap or update the repo's qa/ workspace (regression suite, bug ledger, deferred-verification ledger).
---

# Mobile QA (Staff SQA orchestration)

Verify by **observing real behavior on a real target** — never by reading code alone —
and leave the repo's QA memory richer after every run.

<hard-rules>
These apply to every mode, every agent, every run. They exist because every serious bug
this process has ever caught was invisible in the code and obvious on a device.

- Prove it on a device. Nothing is "verified" until observed running on a simulator,
  emulator, or physical device, with evidence (screenshot and/or log). Reading the code
  is not a test. Compiling is not a test. A green build has shipped entirely-unwired UI before.
- Report faithfully. PASS / FAIL / BLOCKED are the only results. Never infer a pass.
  BLOCKED is honorable; a fake PASS is poison. Separate product bugs from
  environment/tooling flakiness — log the latter in qa/ledgers/flakes.md.
- Pin the run. Every report records: git SHA, branch, build variant, backend/host,
  device fingerprint (model, API/OS, image type). A report without a SHA is a rumor.
- Temporary instrumentation (extra logs, stubbed endpoints, lowered timeouts) is allowed
  and MUST be reverted. Exit with `git status` clean of scaffolding. Never commit or push
  test scaffolding, and never commit/push/open PRs unless the user asks.
- No bug dies without a test. Every fixed bug becomes a regression case in
  qa/regression/cases/ before its ledger entry is closed.
- Escalate native/build/signing/toolchain blockers instead of hacking around them.
- Plan around what this target cannot do (see the emulator-honesty section of
  references/device-driving.md) — known-unverifiable items enter the plan as BLOCKED
  with an escape plan and land in qa/ledgers/deferred-verification.md, instead of
  surfacing as apologies at the end.
</hard-rules>

## Step 0 — Detect context (always)

1. **Stack**: inspect the repo — `build.gradle.kts`/Compose → native Android; `Podfile`/
   SwiftUI → native iOS; `metro.config.js`/`app.json` → React Native/Expo; `pubspec.yaml`
   → Flutter. Never assume; the driving details differ (references/device-driving.md).
2. **Workspace**: does `<repo>/qa/` exist? If not, offer to bootstrap it per
   references/qa-workspace.md (one-time, ~5 min). All modes read and write it.
3. **Targets**: `adb devices` / `xcrun simctl list devices booted`. If several, ask which.
4. **Mode**: pick from the table below based on what the user asked for; confirm scope +
   intended scenarios with the user before driving a device (skip confirmation only for
   an explicitly requested exploratory smoke).

## Modes

| Mode | When | Read next |
|---|---|---|
| **smoke** (T0) | "smoke test", every build, nightly | qa/regression/suite.md smoke cases; run directly |
| **feature** (T1) | "test this feature", "QA this" | references/scenario-design.md |
| **pr** (T2) | "validate this PR/diff/merge" | references/scenario-design.md (impact section) |
| **regression** (T3) | "run regression", "did we break anything" | qa/regression/suite.md + references/quality-gates.md |
| **rc** (T4) | "validate the RC/release build" | references/release-certification.md |
| **certify** (T5) | "ready for release?", "sign off" | references/release-certification.md + quality-gates.md |
| **bootstrap** | "set up the qa workspace" | references/qa-workspace.md |

Every mode ends by (a) writing the report to `qa/reports/YYYY-MM-DD-<slug>/report.md`
with evidence files beside it, and (b) running the curator pass (below) so the corpus accrues.

## Delegation policy

Use subagents where they pay, not everywhere. Three currencies justify one:
parallelism (device-free work only — the device is a mutex), context isolation
(screenshot/log-heavy execution returning a compact results table), and fresh eyes
(an auditor who didn't run the tests). Agent charters live in `.claude/agents/qa-*.md`;
if they aren't installed, spawn a general-purpose subagent and paste the charter from
this package's `agents/` directory.

- **Analysis fan-out (parallel, device-free):** `qa-scope-analyst` (charter: scope, risk
  ranking, regression subset, known-unverifiables) and `qa-static-sweeper` (manifest/
  permissions/exported-components audit, lint deltas, size budget, a11y static scan) —
  launch both at once for feature/pr/rc modes.
- **Planning:** `qa-test-planner` turns the charter into numbered scenario scripts with
  fixtures and expected evidence. For small T1 runs, plan inline instead.
- **Execution:** serialized per device, five lenses in one pass — functional, transport
  (API/analytics at the wire, per references/scenario-design.md), UX (severity-capped P3),
  accessibility (TalkBack segment), performance (timings per scenario where cheap).
  Run **interactive T1 in the main loop** so the user can steer; delegate **batch T3/T4
  chunks to `qa-device-executor`** so screenshots don't flood the orchestrator context —
  it must return the structured results table, not raw images.
- **Curator pass (every run):** `qa-regression-curator` — convert fixed bugs + new
  features into cases, update ledgers, re-rank the suite. Mechanical; cheap model.
- **Audit (T4/T5 only):** `qa-release-auditor` — recompute the verdict from
  references/quality-gates.md against the evidence; write the sign-off. Never the same
  session that executed.

## Execution core (all device-driving modes)

1. **Prepare**: get the app running per the stack notes in references/device-driving.md.
   Push fixtures from qa/fixtures/manifest.md if the plan needs them. Record the pin
   (SHA, variant, backend, device fingerprint).
2. **Drive**: execute each scenario deterministically (known coordinates / semantic
   hierarchy / deep links — never `monkey` for navigation). Capture a screenshot and/or
   log excerpt at each meaningful step into the report directory. Consult
   references/device-driving.md for platform commands and the gotchas list — and add any
   new gotcha you hit to that file; it is the institutional memory.
3. **Verify**: per scenario, compare observed vs expected → PASS/FAIL/BLOCKED. FAIL gets
   minimal repro + evidence + severity per references/quality-gates.md. Anything
   network/analytics-shaped is confirmed at the transport layer, not the pixels.
4. **Report** using the template in references/quality-gates.md (verdict is computed from
   the gate table for the tier, not felt). Deliver the report summary conversationally,
   file the full report + evidence in qa/reports/.
5. **Clean up**: revert instrumentation, restore device state (airplane mode off, etc.),
   `git status` clean.

## Report contract (summary — full template in references/quality-gates.md)

Every report carries: header pin (SHA/variant/backend/device), scenario table
(# / scenario / result / severity / evidence ref), bugs with repro, BLOCKED items with
escape plans → deferred ledger, environment notes (flakes, separated), gate evaluation,
computed verdict. Verdicts: **PASS / PASS-WITH-RISKS / FAIL** (T0–T3) and
**CERTIFIED / CERTIFIED-WITH-RISKS / REJECTED** (T4–T5).
