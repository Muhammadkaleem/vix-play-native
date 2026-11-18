# Vix Play — Ad Revenue Optimisation Roadmap

> **Status 2026-11-18:** Phase 0 code items (0.3, 0.5), all of Phase 1 and Phase 2.2–2.5 are implemented and emulator-verified. Still open: 0.1, 0.2, 0.4 (console/process), 2.1 (create the interstitial unit — release requests none until its ID is in `AdUnits.REAL_INTERSTITIAL`), 2.6 (staged rollout), and a GDPR consent message in AdMob (UMP reports none configured, so EEA users currently get no ads).

Ordered by **risk removed first, then revenue recovered, then revenue added.** Each phase ships, gets measured for about 2 weeks against the baseline in `ADMOB_REVENUE_ANALYSIS.md`, and only then does the next one start. No revenue figures are promised: the current traffic is too small to project from.

## Phase 0: Protect the account (this week; mostly not code)

| # | Task | Owner | Type |
|---|---|---|---|
| 0.1 | Check AdMob → Policy center and the account home page for an "ad serving limited" notice | You | Console |
| 0.2 | Tell everyone with a release / internal-track build **not to click live ads** | You | Process |
| 0.3 | Register team phones as test devices (`setTestDeviceIds`, IDs kept in `local.properties`) | Code | Quick win |
| 0.4 | Link AdMob ↔ Firebase (`devbytes-d260c`). Re-export AdMob reports with Country / App version / Ad source | You | Console |
| 0.5 | Make the Pro perk list honest (strategy §5.1, option A) | Code | Quick win, policy |

## Phase 1: Stop wasting the requests already made (≈ 1 release)

| # | Task | Fixes |
|---|---|---|
| 1.1 | Rewarded: preload on card visibility, not Home open; handle expiry; preload after a failed show; show a loading state | RC-3, RC-7 |
| 1.2 | Banner: lifecycle (`pause`/`resume`/`destroy`), one main `AdView` across tabs, height reserved | RC-3 |
| 1.3 | Native: `NativeAdHolder` per screen; no reload on scroll | RC-3 |
| 1.4 | Video-list native: first slot at 4, then every 10 (config) | RC-6 |
| 1.5 | Load backoff for full-screen preloads | Request bursts |
| 1.6 | `AdEvents` + `ad_impression` paid events | Measurement |

**Exit criteria:** overall show rate goes from **45 %** to over 70 %. Rewarded show rate goes from **28 %** to over 50 %. Per-impression revenue is visible in Firebase.

## Phase 2: Add the capped interstitial (after approving the CLAUDE.md decision change)

| # | Task |
|---|---|
| 2.1 | Create the interstitial unit in the AdMob console and add it to `AdUnits` |
| 2.2 | `AdConfig` + Remote Config defaults (`ads_*` keys) |
| 2.3 | `AdFrequencyPolicy` (TDD) + `AdFrequencyStore` + session tracking |
| 2.4 | `InterstitialAdManager` + player-exit hook + suppression reasons |
| 2.5 | Device test pass: free / Pro / regression (technical design §7) |
| 2.6 | Staged rollout on Play (20 % → 50 % → 100%), watching guardrails |

**Exit criteria:** ARPDAU rises without guardrail regressions. Guardrails: D1 / D7 retention not down more than 2 points, uninstall rate not up noticeably, average rating not dropping. Kill switch: `ads_interstitial_enabled = false`.

## Phase 3: Conversion and tuning

| # | Task |
|---|---|
| 3.1 | Post-interstitial "go ad-free" prompt (after the 2nd interstitial in a session, once a day) |
| 3.2 | Banner_VideoList decision: retire it if match rate is still below 30 % |
| 3.3 | Secondary interstitial trigger (Home after ≥ 5 min), only if player-exit inventory turns out too thin |
| 3.4 | Server-side verification for rewarded, only once a backend exists |

## Phase 4: Experiments (only once DAU allows)

**Requirement:** each arm needs enough users for the result to mean something. As a rule of thumb, hold off until there are several hundred DAU. Until then, use sequential rollouts with guardrails. Tooling: **Firebase A/B Testing on Remote Config** (already integrated). No new dependency.

| Experiment | Arms | Primary metric | Guardrails |
|---|---|---|---|
| Interstitial gap | 3 min vs 4 min vs 6 min | ARPDAU | D7 retention, uninstalls |
| Session cap | 2 vs 3 vs 4 | ARPDAU | D7 retention |
| Home: banner vs native card | bottom banner vs inline native | Home ad revenue per DAU | Home → player taps |
| Trigger type | player-exit only vs player-exit + time-based | Impressions per DAU, ARPDAU | Session length, D7 |
| Pro prompt timing | after 2nd interstitial vs after 1st vs none | Rewarded starts per DAU, Pro activations | ARPDAU (rewarded can replace interstitials) |
| Collapsible banner (later) | standard vs collapsible on Home | Banner eCPM | Rating, accidental-click signals (CTR spike) |

Run **one experiment at a time** at small scale, for at least 14 days so weekly cycles are covered.

## Phase 5: Grow the user base (runs in parallel; outside ad engineering)

This is the biggest lever according to the data (RC-1). It covers ASO (see `docs/aso/`), store screenshots (`assets/store/raw/` exists), D1 retention, and Library Pulse re-engagement (already built).

## Success metrics

| Metric | Baseline (21 Oct – 17 Nov) | Source |
|---|---|---|
| Requests / day | 17.8 | AdMob |
| Match rate | 54.3 % | AdMob |
| Show rate | 45.0 % | AdMob |
| eCPM (blended) | ≈ $1.33 | AdMob |
| CTR | 8.3 % (**should fall** to a normal 0.5–2 % once self-clicks stop) | AdMob |
| ARPDAU, impressions/DAU, revenue/session | **Not measurable yet.** Needs Phase 0.4 + 1.6 | Firebase |
| Rewarded completions / DAU, Pro activations / DAU | Not measurable yet | Firebase |
| D1 / D7 retention, uninstalls, rating | Read from Play Console before Phase 2 ships, as the guardrail baseline | Play Console |
