# Quality gates — severity taxonomy, gate tables, report contract

Verdicts are computed, not felt. Two people with the same evidence table must reach the
same verdict. Disagreement with a gate is a proposal to change this file, not a mood.

## Severity taxonomy

| Sev | Definition | Examples |
|---|---|---|
| **P0** | Crash, ANR, data loss/corruption, security exposure, store-policy rejection, payment error | Startup crash; user file deleted by a failed move; PIN stored in clear; cleartext credential |
| **P1** | Core flow broken with no workaround | Video won't play; library empty on a supported API level; resume position lost; can't grant permission |
| **P2** | Feature degraded, workaround exists | Subtitle style not applied until reopen; sort resets; notification missing artwork |
| **P3** | Cosmetic / polish / UX friction | Misaligned chip; truncated label; awkward copy; minor jank |

Severity attaches to the *user impact*, not the code size of the fix. A one-line bug
that kills startup is P0.

## Gate tables

**T0 smoke** — PASS requires: every smoke case PASS; zero crashes/ANRs in the session.

**T1 feature** — PASS requires: all acceptance-criteria scenarios PASS; zero open P0/P1
in the feature; every FAIL has severity + repro; every BLOCKED has an escape plan and a
deferred-ledger entry; new regression cases filed for the feature and for every bug found.
PASS-WITH-RISKS: only P2/P3 open, each listed. FAIL: otherwise.

**T2 pr** — PASS requires: static sweep clean of new P0/P1 findings; T0 PASS; no
diff-selected regression case newly failing. A pre-existing failure (already in the bug
ledger) doesn't block the PR but must be cited.

**T3 regression** — PASS requires: suite pass-rate ≥ 95% on the primary target AND every
FAIL triaged to severity the same session; zero untriaged failures. P0/P1 findings open
bugs immediately regardless of verdict.

**T4 rc** — CERTIFIED-candidate requires ALL of:
- T3 PASS on the primary target; smoke PASS on every matrix target (qa/device-matrix.md)
- Upgrade-path scenario PASS (previous released build → real data → upgrade → data intact)
- Signed release artifact smoke PASS (R8 build via bundletool — not a debug build)
- Soak PASS: 30-min continuous core-usage session, zero crashes; memory not monotonically climbing
- Perf within budgets (below), or regressions explicitly accepted
- Zero open P0; zero open P1 in core flows; all P2s listed with owners

**T5 certify** — the auditor independently re-checks that every T4 gate has *evidence on
file* (a claimed PASS without evidence is treated as not run), reviews the deferred
ledger and forces an explicit accept/block decision per item, confirms rollout plan
(staged % + crash-rate abort threshold + rollback path). Verdicts:
**CERTIFIED** (all gates, no accepted risks) / **CERTIFIED-WITH-RISKS** (gates pass,
named risks consciously accepted in writing) / **REJECTED** (any gate failed).

## Perf budgets (tune per app in qa/risk-map.md; defaults)

Cold start ≤ 2.5 s on the primary target; key interaction latency ≤ 1 s;
no scenario with >5% janky frames in gfxinfo; steady-state memory flat across 10
open/close cycles of the heaviest screen; release artifact size delta per release ≤ +10%
unless accepted.

## Report template (all modes)

```markdown
# QA Report — <scope> · <mode/tier>
Pin: <git SHA> (<branch>) · <variant> · backend <host> · device <model, API/OS, image>
Date: <date> · Executor: <main-loop | qa-device-executor>

**Verdict (computed):** <PASS | PASS-WITH-RISKS | FAIL | CERTIFIED | CERTIFIED-WITH-RISKS | REJECTED>

| # | Scenario | Result | Sev | Evidence |
|---|----------|--------|-----|----------|
| 1 | <name> | PASS | — | shots/01-*.png |
| 2 | <name> | FAIL | P1 | shots/02-*.png, logs/02.txt |
| 3 | <name> | BLOCKED | — | deferred: <ledger id> — <escape plan> |

## Bugs
<id> · <sev> · <one-line> · repro: 1) … 2) … · evidence: … · regression case: <filed id>

## Blocked / not tested
<scenario> — <why> — <escape plan> — <deferred-ledger id>

## Environment & tooling notes (NOT product bugs)
<flakes observed → also appended to qa/ledgers/flakes.md>

## Gate evaluation
<each gate for this tier: met / not met / n-a, with pointer to evidence>

## Lenses
Transport: <events/requests verified at the wire>
Perf: <numbers recorded>
A11y: <segment result>
UX (P3 notes): <list>
```

Store as `qa/reports/YYYY-MM-DD-<slug>/report.md` with `shots/` and `logs/` beside it.

## Sign-off template (T5 only)

```markdown
# Release sign-off — <app> <versionName> (<versionCode>)
Pin: <SHA> · artifact: <aab path + sha256> · date · auditor: qa-release-auditor

Verdict: <CERTIFIED | CERTIFIED-WITH-RISKS | REJECTED>

Evidence trail: <list of report dirs backing each T4 gate>
Accepted risks: <each: deferred-ledger id · description · why acceptable · revisit-by>
Rollout: staged <n>% → <n>% …; abort if crash-free-sessions < <x>%; rollback: <plan>
```
