# The qa/ workspace — persistent QA memory in the repo

A QA process that forgets is a demo. This directory is the compounding asset: every run
reads it and leaves it richer. Plain markdown/YAML, in-repo — diffable, greppable,
versioned with the commit it describes, readable by every agent with no database.

```
<repo>/qa/
├── risk-map.md
├── regression/
│   ├── suite.md
│   └── cases/<AREA>-<###>.yaml
├── fixtures/manifest.md
├── ledgers/
│   ├── bugs.md
│   ├── deferred-verification.md
│   └── flakes.md
├── device-matrix.md
└── reports/YYYY-MM-DD-<slug>/
```

## Bootstrap (mode: bootstrap, one-time)

1. Create the tree above. 2. Seed `risk-map.md` by mining the repo's history (decision
logs, fixed bugs, `SDK_INT` branches, permission surfaces). 3. Write the smoke suite:
~8 cases covering launch + each core flow, tagged `tier: smoke`. 4. Seed the ledgers —
if the project keeps a decision log (e.g. CLAUDE.md / a tracker agent), harvest its
"not device-verified" and "deferred" items into deferred-verification.md. 5. Draft
fixtures/manifest.md and device-matrix.md (seeds for VixPlay ship in this package's
assets/vixplay-seed/). Confirm the whole tree with the user before committing anything.

## File contracts

### risk-map.md
Per area: name → owning source paths (for diff mapping) → risk tier (high/med/low) →
why (defect history) → standing scenarios that must run when touched. Also holds the
app's perf budgets if they differ from quality-gates.md defaults.

### regression/cases/<AREA>-<###>.yaml
```yaml
id: STORAGE-007
title: Move between folders keeps original recoverable on failure
area: storage-ops            # must match a risk-map area
tier: [regression, rc]       # which suites include it
origin: bug:2026-07-move-degraded-to-copy   # or feature:<name> — provenance mandatory
device: { min_api: 30 }      # requirements; matrix picks targets
preconditions: [fixture clip_c.mp4 in Movies/, second folder exists]
steps:
  - Long-press clip_c.mp4 → overflow → Move → pick DCIM/Camera
  - Accept the system consent prompt
expected:
  - File present in destination, absent from source listing
  - Original exists as .trashed-* (recoverable), not hard-deleted
evidence: [screenshot of destination list, adb ls of source dir]
status: active               # active | flaky | retired(reason)
last_run: {date: 2026-08-07, result: PASS, report: 2026-08-07-rc-3.0.0}
```
The shape is deliberately close to a Maestro flow — if the suite later graduates to
automated runs, cases translate nearly 1:1.

### regression/suite.md
Ordered manifest: id · title · area · tier · device needs · status · last result.
Smoke cases first. The curator re-ranks by risk × recency after every run.

### ledgers/bugs.md
One entry per bug ever found: id · date · severity · one-line · root cause · fix commit
· **guarding regression case id** (entry cannot close without one — "no bug dies
without a test") · status.

### ledgers/deferred-verification.md
The honest debt queue. id · what's unverified · why (which emulator-honesty limit) ·
escape plan · status (open / verified <date, report> / accepted-risk <by, revisit-by>).
Reviewed at every T4; every open item forced to a decision at T5.

### ledgers/flakes.md
Environment noise: symptom · diagnosis · workaround. Consulted before debugging any
weird tooling behavior; the reason flaky infrastructure never gets re-diagnosed.

### fixtures/manifest.md
The curated on-device test corpus: file name · what it exercises · where it lives on
device · how to regenerate (ffmpeg one-liners preferred) · push script. Media apps:
cover codecs, durations (incl. 0 and multi-hour), multi-audio-track, embedded + external
subtitles, corrupt/truncated files, unicode/emoji/very-long filenames.

### device-matrix.md
Targets per tier: primary (daily driver image) · secondary images chosen at the
codebase's `SDK_INT` fault lines · physical device duties (multi-touch, haptics,
real ads, Bluetooth, Play-track installs).

### reports/
One directory per run: report.md (contract in quality-gates.md) + shots/ + logs/.
Never edited after the fact; corrections are new runs.

## Curator pass (every run, any mode)

1. Every bug fixed since the last pass → regression case filed, bug entry updated.
2. Every new feature shipped → 1–3 durable happy-path cases (tier: regression).
3. New BLOCKED items → deferred ledger; newly verified deferred items → closed with the
   report reference.
4. Flakes observed → flakes.md.
5. Re-rank suite.md; propose retirements (case irrelevant after a redesign) rather than
   silently deleting.
