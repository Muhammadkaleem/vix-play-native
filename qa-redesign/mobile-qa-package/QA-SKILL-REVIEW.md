# Review & Redesign: `test-feature` → a production-grade mobile QA orchestration system

**Reviewed:** `~/.claude/skills/test-feature/SKILL.md` (7.2 KB, single file)
**Reviewer stance:** Staff QA Architect. Critical by request. Grounded in your actual project history — every major claim below is backed by something that really happened in the VixPlay repo (per `.claude/agents/grill-tracker.md` and `CLAUDE.md`).
**Date:** 2026-08-07

---

## 1. Verdict up front

The current skill is an **excellent field manual and a poor QA system**. Its execution discipline — evidence-first verification, honest PASS/FAIL/BLOCKED reporting, separation of product bugs from tooling flakiness, mandatory cleanup — is genuinely staff-level, and rarer than you'd think. Those rules should survive any redesign untouched; they are the constitution.

Everything *around* a single test run is missing. The skill has no memory (nothing accrues between runs), no risk model (every feature gets the same generic checklist), no lifecycle tiers (there is only "test this feature" — no PR validation, no regression, no RC, no certification), no quality gates (the verdict is vibes), and no non-functional coverage at all (performance, accessibility, security, upgrade-path). It is one loop of a QA process, mistaken for the process.

The strongest evidence for both halves of this verdict is your own repo:

- **The skill's core thesis is proven.** Every serious bug in the grill-tracker log was found *only* on device, never by reading code: `READ_MEDIA_AUDIO` missing from the manifest (the entire audio subsystem silently dead on API 33+), `PlaybackService` never started for audio (no notification, no lock-screen, no Bluetooth), the folder browser's rename/copy/move dialogs **never wired at all** while everything compiled and `assembleDebug` passed, move silently degrading into copy behind a swallowed `RecoverableSecurityException`, the `LIMIT`-in-sort-order crash that killed startup on every launch. "Prove it on a device" is the right religion.
- **The skill's gaps are also proven.** At its peak, the Shipped table carried **~15 features marked "⚠️ not device-verified"** — a verification debt queue that existed only in a hand-maintained tracker file, invisible to the QA skill itself. Fixed bugs (the drag-reorder `pointerInput(index)` bug, the move-degrades-to-copy bug) became prose in a log, not regression tests — nothing stops them from silently regressing next month. And the recurring emulator limits (pinch injection impossible, no SMB server, Play review unverifiable off-Play, multi-day WorkManager triggers, API ≤ 28 branches unreachable on an API-35 image) were rediscovered *during* runs instead of planned around, every time.

**Recommendation: redesign, don't patch.** Keep the execution core as one mode of a larger skill; build the missing system around it.

---

## 2. Detailed review of the current skill

### 2.1 Scorecard

| Dimension | Grade | Assessment |
|---|---|---|
| Execution discipline | **A−** | Evidence-first hard rules, honest reporting, cleanup contract. The best part of the skill. |
| Device-driving craft | **B+** | The gotchas (zsh word-split, `monkey` uselessness, `uiautomator` idle failures, premature build-launcher exit) are hard-won and valuable. But coordinate-tap driving is fragile and there is no Compose-aware strategy. |
| Scope coverage | **D** | Feature testing only. The stated ambition ("complete mobile application releases") has no implementation: no smoke suite, no PR validation, no regression, no RC validation, no certification. |
| Memory & reuse | **F** | Fully stateless. No test-case corpus, no regression suite, no bug ledger, no flake history, no deferred-verification ledger. Every run starts from zero and evaporates. |
| Risk-based selection | **D** | One generic scenario checklist for everything. No diff → impact mapping, no risk tiers, no knowledge of where this codebase's bugs actually cluster. |
| Quality gates | **D** | "✅ Ready / ⚠️ Ready with issues / ❌ Not ready" with no severity taxonomy and no definition of what blocks what. Two people running this skill on the same evidence could return different verdicts. |
| Non-functional coverage | **F** | Zero performance, accessibility, security, upgrade-path, or release-build testing. For an app now at Play-release stage with five Room schema migrations, ads, a PIN vault, and network credentials, this is the most dangerous gap. |
| Context economy / architecture | **C−** | One monolithic session does analysis, planning, driving, and reporting. Screenshots are enormous in context; by scenario 12 the session has half-forgotten scenario 2. No delegation, no isolation, no parallelism. |
| Portability | **C** | React Native/Expo/Metro assumptions are baked in (`adb reverse 8081`, `ReactNativeJS` logcat tag, dev-client picker) — while the flagship project it serves is **native Kotlin/Compose**. The skill assumes a stack instead of detecting one. |
| Triggering | **C** | The description fires on "test this feature" but not on "run regression", "is this release ready?", "validate this RC", or "certify the build". The skill will silently fail to exist for most of the lifecycle it should own. |

### 2.2 What is genuinely excellent (keep verbatim)

1. **"Reading the code is not a test."** The single most important sentence in the file. Your repo history is a monument to it.
2. **Temporary instrumentation must be reverted; never committed.** The `git status`-clean exit contract.
3. **Faithful reporting** — never infer a pass; BLOCKED is a first-class result; environment flakiness separated from product bugs. (The grill log applies this discipline manually and it repeatedly paid off — e.g. the "wrong file trashed" scare that turned out to be an embedded-title assumption.)
4. **Scope confirmation before touching a device** — grill-me style, matches how you actually work.
5. **Transport-layer verification for network/analytics** — "don't trust the UI alone" is exactly right; it just needs a real playbook behind it.
6. **The device gotchas block.** This is institutional knowledge; it belongs in a reference file that keeps growing.

### 2.3 What is wrong, specifically

**W1 — It's a single loop, not a lifecycle.** There is no notion of *why* you're testing (new feature vs. PR vs. release), so there's no way to scale effort to risk. A release gets the same treatment as a tweak.

**W2 — Statelessness throws away the org's most valuable QA asset.** A real QA function's compounding asset is its accumulated corpus: test cases, regression suite, bug history, flake patterns, known-unverifiable items. The skill accumulates nothing. Meanwhile your grill-tracker *manually* maintains exactly this kind of memory for features — proof the pattern works and that the QA skill should own its own version.

**W3 — No "no bug dies without a test" rule.** The move-degrades-to-copy bug, the notification-registration bug, the startup `LIMIT` crash — all fixed, none converted into permanent regression cases. The current skill's step 6 reports bugs; nothing ensures they stay dead.

**W4 — The generic checklist ignores where this codebase's bugs live.** Your defects cluster in: permission flows across API levels (24–28 / 29 / 30+ / 33+ / 34 partial-media), MediaStore consent flows (`IntentSender` paths), things that compile but were never wired into composition, service/session lifecycle, and Room migrations. A risk catalog should encode this so scenario derivation targets it automatically.

**W5 — No verdict semantics.** Without a severity taxonomy (P0–P3) and per-tier gate tables, "Ready with issues" is unfalsifiable. Gates must be computable from the evidence table.

**W6 — Environment recorded, commit not pinned.** The report template captures device/variant/backend but **not the git SHA under test**. A QA report that doesn't pin the commit can't be trusted a week later, and can't anchor a regression bisect.

**W7 — Emulator honesty is reactive, not planned.** "Note any you consciously skip" happens at step 5, after the plan is made. The known-unverifiable classes (pinch, Play review, multi-day WorkManager, SMB, absent API images, Bluetooth routing, real ad fill) should be a standing matrix consulted at *planning* time, feeding a persistent deferred-verification ledger with explicit escape plans (real-device pass, Play internal track, local SMB server).

**W8 — Release builds are never tested.** Everything runs debug. Release has R8/ProGuard (reflection risk for kotlinx-serialization, smbj, media3), real AdMob units, no debug tooling, signed AAB packaging. The 16 KB page-size issue was caught in the AAB — by luck of someone looking, not by process.

**W9 — Upgrade-path testing doesn't exist.** You've shipped Room v1→v2→v3→v4→v5 migrations, DataStore key evolutions (orphaned `useAmoledTheme` removed), and an MMKV major-version bump (1.3.9 → 2.4.1 for 16 KB alignment). Not one skill line covers *install previous release → upgrade → verify data survives*. This is the single most likely way you destroy real users' resume positions, playlists, and EQ profiles.

**W10 — Monolithic context is a real quality ceiling.** Screenshots-as-images are context-expensive; long device sessions degrade. Analysis and static work that could run in parallel subagents (with results returned as compact structured summaries) instead competes with the device log for the same window.

**W11 — Stack assumptions.** Metro/`adb reverse`/ReactNativeJS in a skill whose primary consumer is a native Compose app. Detection belongs in step 0, and per-stack notes belong in a reference file, not the hot path.

**W12 — No CI/headless story.** Everything assumes an interactive session. A nightly scheduled smoke+regression run (Claude Code headless or a scheduled Cowork task on a machine with an emulator) is cheap and catches drift while you sleep.

---

## 3. Missing capabilities (the complete list)

Grouped; roughly ordered by value-for-cost within each group.

**Lifecycle**
1. Smoke suite (T0) — a 10-minute launch-and-core-paths pass runnable on every build.
2. PR validation mode (T2) — diff → impacted-area map → targeted regression subset + smoke + static sweep.
3. Regression mode (T3) — a persistent, selectable, prioritized suite.
4. Release-candidate validation (T4) — full regression × device matrix + upgrade-path + release-build + soak.
5. Production certification (T5) — gate audit, deferred-risk acceptance, sign-off document.

**Memory (the `qa/` workspace)**
6. Versioned test-case registry (YAML, one file per case, tagged by area/tier/device-needs).
7. Regression suite manifest that *accrues automatically* from every fixed bug and every shipped feature.
8. Bug ledger with severity, root cause, and the regression case that now guards it.
9. Deferred-verification ledger (the "⚠️ not device-verified" queue, made first-class, with per-item escape plans).
10. Flake/environment-issue log, so tooling noise stops being rediscovered.
11. Persistent, dated report archive with pinned commit SHA + device fingerprint + evidence files.

**Selection & planning**
12. Risk catalog encoding where this codebase's bugs cluster (API-level branches, consent flows, unwired-UI, service lifecycle, migrations).
13. Diff-driven impact analysis (changed files → owning features → their regression cases).
14. Emulator-honesty matrix consulted at planning time, not discovered at execution time.
15. Fixture corpus: a curated on-device media set (short/long/4K/HEVC/AV1/multi-audio/embedded-subs/external-srt/corrupt/zero-duration/unicode-names) with a push script and manifest — so "test subtitles" doesn't start with twenty minutes of hunting for a file.

**Non-functional**
16. Performance: cold start (`am start -W`), open→first-frame latency, seek latency, jank (`dumpsys gfxinfo`), memory trend across repeated player open/close (`dumpsys meminfo`), APK/AAB size budget.
17. Accessibility: TalkBack pass on library + player HUD (a gesture-driven player is an a11y minefield), touch-target sizes, Light-theme contrast, contentDescription audit.
18. Security: exported-component audit, intent-filter fuzzing on the external-VIEW path, `allowBackup` vs. the private vault, credential storage (SMB `CredentialStore`), cleartext policy for URL streaming, PIN-hash review.
19. Upgrade-path: prior-release install → upgrade → Room/DataStore/MMKV data intact.
20. Release-build validation: R8 smoke on the signed AAB via bundletool, real ad behavior, Crashlytics symbol upload.

**Verdict & governance**
21. Severity taxonomy (P0–P3) with definitions.
22. Per-tier gate tables; verdict computed, not felt.
23. Sign-off artifact suitable for a release record.

**Architecture & ops**
24. Agent delegation with structured hand-offs (charter → plan → evidence → verdict) and context isolation for heavy evidence.
25. Stack detection (native Android / iOS / RN / Flutter) with per-stack reference notes.
26. Headless/scheduled operation for nightly smoke+regression.
27. Trigger coverage for the whole lifecycle in the skill description ("regression", "RC", "release readiness", "certify", "smoke").

---

## 4. Redesign principles (opinionated)

**P1 — Keep the constitution, grow the government.** The v1 hard rules become shared law across all modes. Nothing in the redesign weakens them.

**P2 — Memory is the system.** The single highest-value change is the persistent `qa/` workspace inside the repo. A QA process that forgets is a demo. Every run must leave the corpus richer: new cases, updated ledger, archived report. This mirrors — and should eventually interoperate with — your grill-tracker's decision log.

**P3 — No bug dies without a test.** Converting every fixed bug into a permanent regression case is non-negotiable and automatic (the Regression Curator's core job).

**P4 — Effort follows risk, and risk is learned.** Scenario selection starts from a risk catalog seeded with this codebase's actual defect history and updated as new bugs land.

**P5 — Agents where they pay, not where they impress.** Sub-agents earn their cost in exactly three currencies: *parallelism* (only for device-free work — the device is a mutex), *context isolation* (screenshot- and log-heavy execution returning compact structured results), and *fresh-eyes independence* (an auditor who didn't run the tests). An eleven-agent org chart for a solo developer is cosplay; six roles with clear hand-offs is engineering. Scale the roster up only when you get parallel hardware or parallel humans.

**P6 — The device is a mutex.** Functional, UX, on-device a11y, and on-device perf checks *share one serialized device session* as lenses, rather than being four agents fighting over one emulator. Parallel device agents require parallel devices — when you have a farm, revisit.

**P7 — Plan around emulator honesty.** Known-unverifiable items are planned as BLOCKED-with-escape-plan up front and land in the deferred ledger, instead of surfacing as apologetic footnotes.

**P8 — Verdicts are computed.** Severity taxonomy + gate tables per tier. The auditor applies the table; disagreement with the table is a proposal to change the table, not a mood.

**P9 — Test what ships.** Debug-on-emulator is for development loops. RC and certification run the signed release artifact, and include upgrade-path testing over real prior data.

**P10 — Progressive disclosure.** SKILL.md stays a lean router (<300 lines): modes, shared law, delegation policy. Deep playbooks live in `references/`, loaded only when the mode needs them.

---

## 5. Redesigned architecture

### 5.1 The skill package

```
~/.claude/skills/mobile-qa/
├── SKILL.md                          # Router: modes, hard rules, delegation policy, workspace bootstrap
├── references/
│   ├── device-driving.md             # v1 cheatsheet, expanded: Compose-aware, iOS, injection limits
│   ├── scenario-design.md            # Risk catalog + derivation method + transport-layer verification
│   ├── quality-gates.md              # Severity taxonomy, per-tier gate tables, report contract
│   ├── release-certification.md      # T4/T5 playbook: matrix, upgrade-path, release-build, soak, sign-off
│   └── qa-workspace.md               # The persistent qa/ directory contract + file templates
├── agents/                           # Copy into <repo>/.claude/agents/ (or ~/.claude/agents/)
│   ├── qa-scope-analyst.md
│   ├── qa-test-planner.md
│   ├── qa-device-executor.md
│   ├── qa-static-sweeper.md
│   ├── qa-regression-curator.md
│   └── qa-release-auditor.md
└── assets/
    └── vixplay-seed/                 # Starter workspace content for THIS project
        ├── deferred-verification.md  # Seeded from your real grill-tracker backlog
        ├── device-matrix.md          # API-level strategy for your minSdk 24 / target 36 spread
        └── fixtures-manifest.md      # The media corpus to build once and reuse forever
```

### 5.2 The persistent workspace (lives in each repo under test)

```
<repo>/qa/
├── risk-map.md                   # Where bugs cluster; feature → files → risk tier
├── regression/
│   ├── suite.md                  # Ordered manifest: case id → tier → area → device needs
│   └── cases/AREA-###.yaml       # One case per file: preconditions, steps, expected, evidence hints
├── fixtures/manifest.md          # Media corpus + push script
├── ledgers/
│   ├── bugs.md                   # Every bug: severity, root cause, guarding regression case
│   ├── deferred-verification.md  # Known-unverified items + escape plan + status
│   └── flakes.md                 # Environment/tooling noise, so it's never re-diagnosed
├── device-matrix.md              # Targets per tier
└── reports/YYYY-MM-DD-<slug>/    # report.md + shots/ + logs/ — pinned SHA + device fingerprint
```

Everything is plain markdown/YAML in-repo: diffable, greppable, reviewable in PRs, and readable by every agent without a database.

### 5.3 Agent roster (6, not 11) and why

Your proposed eleven agents map onto six roles. The merges are deliberate:

| Your proposal | Redesign home | Why |
|---|---|---|
| Requirements Analysis Agent | **qa-scope-analyst** | Requirements and risk are one judgment — what matters and what's dangerous are the same question asked twice. |
| Test Planning Agent | **qa-test-planner** | Stays distinct: turns a charter into executable scripts with fixtures and expected evidence. |
| Functional QA Agent | **qa-device-executor** | The device is a mutex (P6). |
| API Testing Agent | **qa-device-executor** (transport lens) | For a mobile client, API validation happens *through the app's own traffic* during the same session — separate agents would duplicate the drive. |
| Analytics Validation Agent | **qa-device-executor** (transport lens) | Same session, same interception point (logcat / DebugView / proxy). |
| UX Review Agent | **qa-device-executor** (UX lens) | UX findings come from the same screenshots; a separate agent would re-drive everything. Severity-capped as P3 unless it blocks a flow. |
| Accessibility Agent | split: static → **qa-static-sweeper**; TalkBack → **qa-device-executor** (a11y lens) | contentDescription/touch-target audits are code-side and parallelizable; TalkBack needs the device. |
| Performance Testing Agent | split: budgets/setup → **qa-static-sweeper**; measurement → **qa-device-executor** (perf lens) | Same split logic. |
| Security Testing Agent | **qa-static-sweeper** (primary) + executor for dynamic checks | Manifest/exported-components/backup/cleartext audits are static and parallel; intent-fuzzing the VIEW path is a short device segment. |
| Regression Analysis Agent | **qa-regression-curator** | Owns the suite: selection by diff, and accrual from bugs/features. |
| Release Readiness Agent | **qa-release-auditor** | Fresh-eyes independence (P5): it did not run the tests it judges. |

**Role charters:**

- **qa-scope-analyst** — Input: feature description / PR diff / release tag. Reads PRDs, diff, risk-map, deferred ledger. Output: a *test charter* — what's in scope, risk-ranked areas, what's known-unverifiable here (with escape plans), what regression subset the diff touches. Read-only; parallelizable.
- **qa-test-planner** — Charter → concrete numbered scenarios with fixtures, exact steps, expected evidence per step, and device/API requirements. Flags any scenario needing hardware the current target lacks *before* execution starts.
- **qa-device-executor** — The v1 skill's soul. Serialized per device. Runs scenario scripts through five lenses in one pass (functional, transport, UX, a11y, perf), captures evidence to the report directory, returns a compact structured results table — not raw screenshots — to the orchestrator. Interactive T1 runs stay in the main loop so you can steer; batch T3/T4 runs go to this agent for context isolation.
- **qa-static-sweeper** — Device-free, runs in parallel with execution: lint/compile warnings deltas, manifest & exported-component audit, permission diff, dependency/license scan, a11y static audit, perf budget checks (APK size delta, startup-path review), R8 mapping sanity. Output: findings with severity, file:line.
- **qa-regression-curator** — After every run: converts fixed bugs and new features into regression cases, re-ranks the suite, prunes stale cases, updates ledgers. Before every run: selects the subset the diff/charter demands. Mechanical; cheap model.
- **qa-release-auditor** — T4/T5 only. Reads all reports, ledgers, and gate tables; verifies evidence exists for every claimed PASS; computes the verdict from `quality-gates.md`; writes the sign-off document. Must not be the session that executed.

### 5.4 Collaboration flow

```
                    ┌─ qa-scope-analyst ──┐            (parallel, device-free)
 request ─ intake ──┤                     ├─ charter ─ qa-test-planner ─ plan
                    └─ qa-static-sweeper ─┘                               │
                                                                          ▼
                              serialized per device:  qa-device-executor (5 lenses)
                                                                          │ evidence + results
                                                                          ▼
                                 qa-regression-curator (accrue cases, update ledgers)
                                                                          │
                                                     T4/T5 only: qa-release-auditor ─ verdict + sign-off
```

Hand-offs are structured artifacts (charter → plan → results table → verdict), each written into `qa/reports/<run>/` so any agent — or you, next month — can reconstruct the run without replaying context.

---

## 6. The testing lifecycle (T0–T5)

| Tier | Trigger | Scope | Time | Gate to pass |
|---|---|---|---|---|
| **T0 Smoke** | every build / nightly | launch, core happy paths (~8 scripted cases), no crash, no ANR | ~10 min | all smoke cases PASS |
| **T1 Feature** | feature complete | charter-driven scenarios: happy, states, edges, negatives + transport + feature-local regression | 30–90 min | 0 open P0/P1 in feature; new regression cases filed |
| **T2 PR validation** | before merge | static sweep + T0 + diff-selected regression subset | 15–30 min | 0 new P0/P1; no regression case newly failing |
| **T3 Regression** | weekly / pre-RC | full suite on primary target, prioritized risk×recency; flake triage | 2–4 h | pass rate ≥ threshold; every FAIL triaged to severity |
| **T4 RC validation** | release branch cut | T3 across device matrix + upgrade-path + signed-release-build smoke + soak (30-min playback, monkey pass) + perf baselines | 0.5–1 day | gate table in quality-gates.md |
| **T5 Certification** | before Play submission | audit of T4 evidence + deferred-risk acceptance + rollout plan + sign-off doc | 1–2 h | signed CERTIFIED / CERTIFIED-WITH-RISKS / REJECTED |

Design notes:

- **T2 is the regression minimizer.** Diff → `risk-map.md` → owning areas → their regression cases. Cheap enough to run on every merge, which is what actually prevents the "15 unverified features" queue from re-forming.
- **T4's upgrade-path test is mandatory**: install the previous released APK/AAB, create real data (positions, playlists, EQ profiles, PIN vault, SMB credentials), upgrade in place, verify all of it survives. Five Room migrations and an MMKV major bump say this is your likeliest catastrophic-regression vector.
- **T4 runs what ships**: the signed release AAB via bundletool on at least one target — R8, real ad units, no debug scaffolding.
- **T5 produces a document, not a feeling** — the sign-off records the evidence trail, accepted risks (each pointing at a deferred-ledger entry with an owner and an escape plan), and the rollout/rollback plan (staged rollout %, crash-rate abort threshold).

---

## 7. Quality gates (summary — full tables in `references/quality-gates.md`)

Severity: **P0** crash/data-loss/security/store-rejection · **P1** core flow broken, no workaround · **P2** degraded but workaround exists · **P3** cosmetic/UX polish.

Non-negotiable release gates: zero known P0; zero P1 in core playback/library/storage flows; upgrade-path clean; crash-free smoke on the signed artifact; every deferred item consciously accepted in writing. The auditor computes the verdict; overrides are recorded as accepted risks with names on them.

---

## 8. VixPlay-specific assets (seeded, in `assets/vixplay-seed/`)

- **Deferred-verification ledger** pre-populated from your real backlog: pinch/pan on physical hardware, SMB against a real share, Pulse delivery/quiet-hours/deep-link taps (multi-day), Play in-app review (internal track), API ≤ 28 legacy rename/copy branches, cancel-mid-copy cleanup, multi-file move batches, biometric unlock, Cast (needs Chromecast), native-ad fill on hardware, on-screen title fix visual confirm.
- **Device matrix** for minSdk 24 / target 36: emulator images at the behavioral fault lines (24, 26, 29, 30, 33, 34, 36) + one physical device for touch/haptics/real-ads/Bluetooth.
- **Fixtures manifest**: the media corpus (codecs, durations, multi-audio, embedded/external subs, corrupt, unicode) to build once and push via script.

---

## 9. Adoption roadmap

**Phase 1 (one session):** Install skill + agents. Bootstrap `qa/` in the repo; seed ledgers from the grill-tracker; write the 8-case smoke suite. Immediate payoff: T0 exists.
**Phase 2 (1–2 sessions):** Backfill regression cases for the highest-risk shipped areas (storage ops, permissions, playback resume, playlists); encode the risk map; build the fixture corpus.
**Phase 3:** Run T1 on the next grill; run T2 on the next merge. Tune gates.
**Phase 4 (pre-release):** First full T4 + T5 against a signed AAB, including the upgrade-path test from the last shipped versionCode. This is the first release where "ready" means something computable.
**Phase 5 (ongoing):** Nightly scheduled T0 (+weekly T3) headless on a machine with an emulator; description-optimization pass on the skill (skill-creator's trigger-eval loop) once the content settles.

---

## 10. What I would *not* do

- **Don't build 11 agents.** You'd pay context-startup cost eleven times per run to simulate an org you don't have. Split the executor's lenses into real agents only when you have parallel devices; split the sweeper only when static jobs exceed one session's budget.
- **Don't adopt Appium/Maestro yet.** Claude-driven adb/simctl with screenshot-reading is slower per step but needs zero harness code in the app and survives UI churn. Revisit when the smoke suite runs nightly and speed matters; Maestro (YAML flows, Compose-friendly) is the natural first upgrade, and regression case YAML in this design is deliberately shaped to translate into Maestro flows nearly 1:1.
- **Don't put QA state anywhere but the repo.** No databases, no external trackers. Diffable text next to the code it tests, versioned with the commit it describes.
- **Don't let UX opinions inflate verdicts.** UX lens findings cap at P3 unless a flow is genuinely blocked; taste goes in the report, not the gate.

---

## Appendix: the redesigned skill

The complete implementation ships alongside this review — `mobile-qa/` (SKILL.md, five reference playbooks, six agent definitions, VixPlay seed files). Install:

```bash
cp -R mobile-qa ~/.claude/skills/mobile-qa
cp mobile-qa/agents/*.md <repo>/.claude/agents/
# then, in a session:  "bootstrap the qa workspace"   (SKILL.md handles the rest)
```

Retire `test-feature` after a couple of satisfactory `mobile-qa` runs — its constitution lives on in the new SKILL.md's hard rules, its cheatsheet in `references/device-driving.md`.
