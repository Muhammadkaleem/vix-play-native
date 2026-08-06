# PRD: VixPlay Growth Foundation — v3.1 "Measure, Monetize, Return"

*Status: Draft for review · Author: Growth Product Analyst · Date: 2026-08-05 · Target release: 3.1.0 (versionCode 59+) · Baseline: v3.0.0 / versionCode 58*

*Evidence base: full growth audit of 2026-08-05 (overall product health 46/100 — scorecard: Analytics 8, Growth 30, ASO 35, Monetization 38, Retention 45, Technical 55, UX 58, Product 62), monetization deep-dive, and retention-feature ICE-R analysis. Sources cited inline: `AndroidManifest.xml`, `app/build.gradle.kts`, `AdsController.kt`, `AdUnits.kt`, `AboutProScreen.kt`, `SplashScreen.kt`, `vix-play-docs/media-player-docs/*`.*

---

## 1. Problem Statement

VixPlay 3.0 is an unusually deep player attached to a near-zero growth system. The player surface (gestures, precision seek, subtitle sync/styling, EQ, PiP, playlists, private vault, network streaming) out-engineers most 1.0 competitors — but four outward-facing systems sit at zero, and each one is measurable in the codebase itself:

1. **Distribution**: the manifest declares only `MAIN/LAUNCHER` — no `ACTION_VIEW` intent filter — so VixPlay can never appear in Android's "Open with" sheet or become the default player. This is the #1 acquisition-and-habit channel in the category (it is how MX Player became a default on hundreds of millions of devices), and it contradicts VixPlay's own spec (`screens/03-video-player.md` lists "external intent (ACTION_VIEW)" as an entry point).
2. **Measurement**: no analytics or crash SDK in `build.gradle.kts`. Retention, funnel drop-off, codec-failure rate, and the vision doc's own 99.5% crash-free target are all unmeasurable today.
3. **Revenue**: the declared model is "calm ads + one-time Pro unlock" (`features/monetization.md`), but the Pro half doesn't exist — no Play Billing dependency, `AdsController.adsEnabled` is a hardcoded `val = true`, and the Settings row labeled "About / Pro" opens a screen with no purchase path. A user who wants to pay cannot. 100% of revenue sits in the lowest-eCPM formats (banner/native, typically $0.30–1.50 T1), and every ad is annoyance with no conversion pressure.
4. **Re-engagement**: nothing designed brings a user back. The app already stores exactly the data (resume positions, titles, timestamps in Room + MMKV) that powers the one notification this category forgives — "continue watching" — and never uses it.

This release fixes all four with three workstreams (A: Distribution & Measurement, B: Monetization Completion, C: Library Pulse retention notifications) and commits a prioritized roadmap of new value features gated on the data this release starts collecting.

## 2. Goals & Non-Goals

**Goals** (all measured via events defined in §3; baselines established in the first 2 instrumented weeks)

1. **Become a candidate default player.** ≥ 15% of player opens arrive via external `ACTION_VIEW`/`SEND` intent within 90 days of release.
2. **See everything.** 100% instrumentation of the install → activation → playback → revenue funnel; crash-free sessions ≥ 99.5% *measured*, not assumed.
3. **Complete the business model.** Pro unlock live with restore; Pro conversion ≥ 0.5% of MAU by day 90, on a credible path to the vision doc's 2% target.
4. **Create the first designed return trigger.** Notification-attributed sessions ≥ 8% of all sessions by day 60 of full rollout; +2–4 pts D7 for the opted-in cohort vs. holdback.
5. **Stay calm.** Guardrails: uninstall rate flat vs. pre-release, notification-channel disable rate < 15% of notified users, zero ads on the player surface (existing contract, now lint-enforced).

**Non-Goals**

- **No backend, no accounts, no subscription SKU.** One-time Pro stays the spine; cloud/sync subscription remains P2 (`features/monetization.md`) and enters only with cross-device resume (§13).
- **No FFmpeg in this release.** "Plays everything" is the R3 headline, deliberately *after* `codec_error` analytics from this release quantify the real failure rate (2 weeks of data sizes a 3-week build).
- **No series/episode intelligence, no home-screen widget.** Both are strong P2 follow-ups that reuse Library Pulse's candidate-query layer (§13); bundling them doubles QA surface.
- **No app-open ads by default.** The parked `APP_OPEN` unit (`AdUnits.kt`) may run only as a capped, risk-gated experiment (§8), never on external-intent opens, never as a launch default.
- **No retroactive feature-gating.** Features shipped free (EQ, network streaming, private folder) stay free forever — clawbacks are the review-bomb pattern. Pro depth is forward-only (§13, F-6).

## 3. Success Metrics

> **Instrumentation is a launch-blocking requirement of this PRD** (FR-A5…A8), not a nice-to-have. All "Current" values are unknown because the app ships no analytics today.

| Metric | Current | Target (day 90) | Measurement (event/funnel) |
|---|---|---|---|
| **North star: Weekly Active Viewers** (≥1 completed playback ≥60s/week) | not instrumented | baseline +20% | `playback_complete` per user per week |
| % player opens via external intent | 0% (impossible today) | ≥ 15% | `playback_start {source: external}` / all `playback_start` |
| Activation (install → first completed playback) | not instrumented | ≥ 70% | `first_open` → `playback_complete` funnel |
| D1 / D7 / D30 retention | not instrumented | 28% / 12% / 5% (category-typical: 25–35 / 10–15 / 4–8) | Firebase retained-user cohorts |
| Pro conversion | 0% (no product) | ≥ 0.5% of MAU | `paywall_view` → `pro_purchase` |
| Paywall view rate | 0% | ≥ 8% of MAU sees paywall/month | `paywall_view {source}` |
| Notification-attributed sessions | 0% | ≥ 8% of sessions | `session_start {source: notification}` |
| Rating volume | unmanaged | 3× baseline volume, ≥ 4.4★ | Play Console + `review_prompt_shown` |
| Crash-free sessions | unknown | ≥ 99.5% | Crashlytics |
| Playback success rate | not instrumented | ≥ 98% measured (sizes the FFmpeg decision) | `playback_start` vs `codec_error {codec_name}` |

**Guardrails (must not degrade)**

| Guardrail | Threshold | Measurement |
|---|---|---|
| Uninstall rate | flat vs. 30-day pre-release baseline | Play Console cohorts |
| Notification disable rate | < 15% of notified users | `areNotificationsEnabled()` check on app open |
| Ad revenue during Pro rollout | ≥ 85% of pre-release run-rate (some loss to Pro conversions is the *point*) | AdMob dashboard / DAU |
| Session length on list screens | flat (ad-density check) | `session_end {duration}` split by screen |
| ANR rate | < 0.47% (Play vitals threshold) | Play Console vitals |

## 4. User Stories & Key Flows

**Flow 1 — "Open with VixPlay" (WS-A)**
*As an Android user, I want VixPlay offered when I tap a video anywhere, so the player I prefer is the player I get.*
1. User taps `movie.mkv` in Files/WhatsApp/browser download → Android app chooser lists VixPlay.
2. Selecting VixPlay (or having set it default) cold-starts straight into full-screen playback of that URI — no library detour. The player already handles arbitrary non-MediaStore URIs gracefully (session-only subtitle offsets, no-crash degradation).
3. Back exits to the caller, not into VixPlay's library (matches user intent; the library back-stack applies only to launcher entries).
4. First external open fires a one-time, dismissible snackbar after playback ends: "Make VixPlay your default player?" → system default-apps settings. Never repeated after dismissal.

**Flow 2 — Paying for calm (WS-B)**
*As an ad-fatigued regular user, I want to remove ads with one purchase, so the app I use daily feels like mine.*
1. Entry points: (a) "Remove ads — Go Pro" caption on native ad cards and under the library banner; (b) a dedicated primary-tinted "Go Pro" row atop Settings (the current combined "About / Pro" row splits: Pro row above the fold, About demoted to the bottom); (c) a one-time nudge card after the 5th completed playback (queryable today from Room positions).
2. Paywall screen (rebuilt `AboutProScreen` → `ProScreen`): headline "Remove all ads, forever — one purchase." Benefit list contains **only what ships today**: ad-free everywhere, support an indie player, future Pro perks included (skins/backglow named as "coming", not promised dates). Price localized via Play; single SKU `vixplay.pro.unlock`.
3. Purchase → Play Billing flow → acknowledge → entitlement flips `adsEnabled` reactively → every ad surface disappears **in the same session, without restart** (StateFlow recomposition — same live-apply pattern as ThemeViewModel).
4. Restore: "Already purchased?" on the paywall + automatic entitlement re-query on app start (reinstall case). Pending purchases show "Payment pending…" state; refund/revocation downgrades gracefully on next query (edge cases per `features/monetization.md`).

**Flow 3 — Library Pulse: new video lands (WS-C)**
*As a media hoarder, I want a heads-up when a download is ready to play, so I don't hunt through a file manager or another player.*
1. `The.Expanse.S02E04.mkv` lands in `Download/`. Within ≤15 min (WorkManager periodic diff of MediaStore; ≤60s if the process is alive via ContentObserver), notification posts on channel "New videos": thumbnail, "New video ready", "The.Expanse.S02E04 · Download · 43 min", actions **Play** / **Later**.
2. Tap/Play deep-links to `playerRoute(uri)`. 2+ files in one window collapse to a single digest ("3 new videos in Download") opening the folder browser.
3. First scan after install/update seeds the known-media baseline **silently** — no "400 new videos!" blast.

**Flow 4 — Library Pulse: continue watching (WS-C)**
*As an everyday viewer, I want a nudge about the movie I fell asleep in, so I finish it instead of forgetting it.*
1. `Dune.Part.Two.mkv` sits at 62%, last played 24–72h ago (row already in `PlaybackPositionDao`, within the Continue-Watching 5–90% window).
2. In the evening window (18:00–21:30 local) the top candidate posts on channel "Continue watching": "Continue watching? · Dune Part Two — 58 min left", actions **Resume** / **Dismiss**.
3. Resume deep-links into the player; the existing resume-gate seeks and shows the Restart chip — zero new player behavior.
4. Caps: each video nudged **once ever**; ≤2 resume nudges/week; ≤1 notification/day globally across all Pulse types; quiet hours 22:00–08:00 (overnight arrivals → 08:30 digest).

**Flow 5 — Contextual opt-in (WS-C, replaces cold permission ask)**
1. After the 2nd completed playback session (proof of aha), a one-time card above the library grid: "Never lose your place — get a heads-up when new videos arrive, and pick up where you left off." **Turn on** / **Not now**.
2. Turn on → `POST_NOTIFICATIONS` runtime prompt (API 33+) fired *in context* (in-context asks typically convert 50–70% vs <40% cold). Not now → never re-shown; feature reachable in Settings ▸ Notifications.

**Unhappy paths (all workstreams)**
- External intent for an unplayable codec → existing error overlay, now firing `codec_error {codec_name}` — this is the data that sizes the FFmpeg build.
- Billing unavailable (no Play, region block) → paywall shows "Purchases unavailable on this device," never a dead button; ads continue.
- Notification tapped after file deleted → validate URI on tap; fall back to Video Library + toast "That video is no longer on this device."
- Notifications disabled at OS level after opt-in → scans pause entirely (no zombie work); Settings shows an inline "Notifications are off for VixPlay" row deep-linking to system settings.
- Android 14 partial media access ("Select photos and videos") → library shows a persistent "Manage selection" affordance instead of silently rendering a subset (adds `READ_MEDIA_VISUAL_USER_SELECTED`; closes the activation bug from the audit).

## 5. Functional Requirements

### Workstream A — Distribution & Measurement

- **FR-A1** `MainActivity` intent filters for `ACTION_VIEW` (`video/*`, `audio/*`; schemes `content`, `file`, plus `http/https` for direct stream links — the HLS/DASH/RTSP modules already handle them) and `ACTION_SEND` (`video/*`). Route to the existing player route with the encoded URI (pattern proven by the network-stream flow).
- **FR-A2** External-intent playback cold-start to first frame < 1.5s on a mid-tier device; no library load on that path.
- **FR-A3** One-time "make default" snackbar after first external playback ends; dismiss = never again (DataStore flag).
- **FR-A4** In-app review prompt (Play In-App Review API) after the 3rd `playback_complete`, max once per version, never after an error session. (Typically converts 5–15% of prompted users; rating volume is a top-3 ranking factor.)
- **FR-A5** Firebase Analytics + Crashlytics via `google-services.json` in `app/` (the architecture's own plan). No other trackers. Play data-safety form updated in the same release.
- **FR-A6** Core events: `first_open`, `permission_result {granted, type}`, `library_loaded {video_count}`, `session_start {source: launcher|notification|external|pip}`, `playback_start {source}`, `playback_complete`, `codec_error {codec_name, container}`, `ad_impression {placement}`, `paywall_view {source}`, `pro_purchase {price_micros, currency}`, `review_prompt_shown`, `pulse_*` family (FR-C10). Debug builds must not report to the production project (build-type-scoped config, mirroring the `AdUnits` debug/release split).
- **FR-A7** R8 + resource shrinking enabled for release, with keep rules for SMBJ/BouncyCastle/MMKV reflection surfaces; ship behind a staged rollout (risk gate: reflection breakage).
- **FR-A8** Crash-free and ANR dashboards reviewed as a release-exit criterion (≥ 99.5% / < 0.47%).
- **FR-A9** Android 14: add `READ_MEDIA_VISUAL_USER_SELECTED`; library detects partial grant and renders a "Manage selection" row (spec: `screens/00-splash-onboarding.md`).
- **FR-A10** Replace the cold-start double permission blast (`SplashScreen.kt` fires video+audio at entry) with: video permission asked with a one-line rationale card at splash; audio permission deferred to first Audio-tab visit.

### Workstream B — Monetization Completion

- **FR-B1** Add Google Play Billing (current major version — verify against Play's version floor at build time; the "v6" in the architecture table is stale). Single non-consumable product `vixplay.pro.unlock`.
- **FR-B2** Refactor `AdsController.adsEnabled` from `val` to a `StateFlow<Boolean>` backed by a local entitlement cache (MMKV/DataStore) so ad-free applies instantly, offline, and on every surface without restart.
- **FR-B3** Purchase lifecycle: launch → acknowledge within 3 days (else auto-refund) → entitlement write. Handle PENDING (show state, don't grant), refund/revocation (downgrade on next query), restore on reinstall (query on app start + explicit paywall button). QA per `engineering/qa-release.md` matrix.
- **FR-B4** Rebuild `AboutProScreen` as `ProScreen` (paywall) + `AboutScreen` (version/licenses, demoted to Settings bottom). Paywall promises only shipped capabilities.
- **FR-B5** Pricing: $4.99 USD base (anchor: MX Player Pro ≈ $5.49 — verify current), Play regional price templates on day one (category volume is IN/SEA/MENA-weighted), no intro discount at launch (§12 Q2).
- **FR-B6** Upsell surfaces: (a) "Remove ads — Go Pro" caption on native ad cards + banner containers; (b) primary-tinted Settings row; (c) one-time post-value nudge after 5th `playback_complete`, dismissible, never on the player surface. Each fires `paywall_view {source}` for attribution.
- **FR-B7** First-session ad grace: no ad units render until the 2nd cold start (one DataStore flag). Protects activation; costs ≈ one session of banner impressions per user.
- **FR-B8** Lint/CI check codifying the existing contract: no ad composable may enter the player surface module.

### Workstream C — Library Pulse (retention notifications)

- **FR-C1** WorkManager `PeriodicWorkRequest` (15 min, `requiresBatteryNotLow`) diffs MediaStore video against a Room `known_media` table — `MediaStore.getGeneration()` on API 30+, `DATE_ADDED` fallback below. p90 incremental scan < 2s on a 5k-item library.
- **FR-C2** In-process `ContentObserver` on the video content URI triggers the same diff with ≤60s debounce.
- **FR-C3** First scan seeds silently (FR-mandated QA case). Exclusions: private-vault items, `.nomedia` (unindexed anyway), files < 60s, user-muted folders.
- **FR-C4** New-media notification per Flow 3; ≥2 files → digest. Channel `pulse_new_media` (default importance).
- **FR-C5** Resume candidate = progress ∈ [0.05, 0.90] ∧ `lastPlayedAt` ∈ [24h, 72h] ∧ URI resolvable. One per evaluation, once ever per video (`PulseLog`), ≤2/week. Channel `pulse_resume` (low importance).
- **FR-C6** Global cap ≤1 Pulse notification/day; quiet hours 22:00–08:00 with 08:30 digest; resume nudges only 18:00–21:30.
- **FR-C7** Expanded new-media notifications carry a "Mute this folder" action → DataStore mute list.
- **FR-C8** Contextual opt-in per Flow 5; state machine `UNSEEN/ACCEPTED/DECLINED` in DataStore; master + per-type toggles in a new Settings ▸ Notifications screen (new route in `Routes.kt`). Toggle-off cancels all scheduled work, verified via `dumpsys jobscheduler`.
- **FR-C9** Deep links route through `MainActivity` → Navigation Compose with a Library→Player back-stack (notification-opened player backs out to the library, not an empty task). `FLAG_IMMUTABLE`, request codes keyed by mediaStoreId.
- **FR-C10** Events: `pulse_optin_shown/result`, `pulse_scan_completed {duration_ms, new_count, trigger}`, `pulse_notif_shown/opened/dismissed {type}`, user property `pulse_enabled`.

## 6. UX Notes

- **Paywall**: dark-first, brand gradient header (the splash's indigo→violet is the app's most premium-feeling asset), three benefit rows with tinted icons, price button full-width. No countdown timers, no fake discounts — the anti-dark-pattern stance *is* the brand. All colors from `colorScheme`/tokens (project rule #1).
- **Upsell captions**: `labelSmall`, `onSurfaceVariant`, single line — visible, never shouting. The caption converts at the moment of ad-annoyance; volume comes from placement count, not size.
- **Opt-in card**: reuses `LibraryEmptyState` visual vocabulary (tinted icon badge, title, one-line body, primary + text button). TalkBack-traversable; buttons carry full labels.
- **Notifications**: `BigPictureStyle` with a frame thumbnail via the existing Coil `VideoFrameDecoder` pipeline; 3s extraction timeout → fall back to large-icon layout, never delay posting. Copy is a receipt, not an ad: filename-without-extension · folder · duration; remaining time uses the Continue-Watching chip's `durationMs − positionMs` math.
- **"Make default" snackbar**: single line + "Settings" action; appears once, post-playback, never mid-session.

## 7. Technical Considerations

- **New dependencies**: `androidx.work:work-runtime-ktx` (first WorkManager use — `storage-scanning.md` already planned it), Firebase BoM (Analytics, Crashlytics, Remote Config for kill switches), Play Billing, `google-services` plugin. All added to `libs.versions.toml` per project convention.
- **Schema**: additive Room migration adding `known_media(mediaStoreId PK, dateAdded)` and `pulse_log(mediaStoreId PK, type, notifiedAt)`. Verify the current schema version at build time and copy DDL from Room's exported schema JSON (established project practice from `MIGRATION_2_3`/`MIGRATION_3_4`).
- **Entitlement cache**: MMKV boolean on the hot path (same rationale as resume positions — read at every ad-slot composition), DataStore mirror for observability. Entitlement is Play-verified locally (no backend — per architecture decision; server verification arrives only with a subscription tier).
- **Intent handling**: external URIs already degrade gracefully (session-only subtitle offset, null folder-next). New work is manifest + task-affinity/back-stack behavior; test the `ACTION_VIEW`-while-app-backgrounded case (singleTask re-entry).
- **Hilt**: `@HiltWorker` for the scan worker; WorkManager init via `Configuration.Provider` on `VixPlayApp`.
- **OEM reality**: 15-min periodic work is best-effort on aggressive OEMs (Xiaomi/Huawei) — acceptable; the ContentObserver covers active-device moments. Do **not** request battery-optimization exemption for a notification feature.
- **Riskiest assumption**: MediaStore indexes downloads promptly enough for Flow 3's "moment of intent." **Spike (2 days, M0)**: measure indexing lag end-to-end on a Pixel + a Xiaomi via Chrome and a torrent client. If p50 lag > 30 min, re-scope Flow 3 to an in-app "new videos" banner on next open and keep notifications for periodic catches.
- **R8 risk**: SMBJ/BouncyCastle are reflection-heavy; keep rules exist under `keepRules/` but are untested under full R8. Staged rollout + Crashlytics (which lands in the same release) is the safety net.

## 8. Monetization & Growth Hooks

This release *is* the monetization completion, so the hooks are structural: every ad surface becomes a Pro salesperson (FR-B6), external-intent sessions expose new users to the calm free tier at their moment of need, and Pulse sessions land on the contractually ad-free player surface — deliberately unmonetized, because the retained-user base is the multiplier on both ad and Pro revenue.

Explicitly deferred, risk-gated (P2, post-data): a **capped app-open ad experiment** — ≤1/day, never on external-intent opens (< 1.5s time-to-frame target), never within 30s of backgrounding, 10–20% staged rollout judged against D1/D7 guardrails with a Remote Config kill switch. Interstitials, collapsible banners, and notification ads are permanently out — they are the MX-clutter pattern this brand exists to oppose.

## 9. Rollout Plan

- **Flags** (Remote Config): `pro_enabled`, `pulse_enabled`, `intent_default_snackbar`, plus the R8 build itself as a staged binary rollout.
- **Phase 0 (internal, 1 wk)**: OEM matrix (Pixel/Samsung/Xiaomi, API 24/29/33/36); billing test track with license testers; MediaStore-lag spike results applied.
- **Phase 1 (10%, 2 wks)**: all three workstreams on; **10% permanent holdback for Pulse** (90 days — the retention control group; without it Goal 4 is unmeasurable).
- **Phase 2 (50% → 100%)**: after 14 clean guardrail days.
- **Kill criteria**: crash-free < 99.0% → halt binary rollout; notification disable rate > 25% in any 7-day window → `pulse_enabled` off; billing acknowledge failures > 2% → `pro_enabled` off (ads continue, nothing breaks); uninstall rate for rollout cohort > holdback +15% relative → full stop and diagnose.

## 10. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Pulse perceived as spam → channel kills, 1★ reviews | Medium | High | 1/day cap, quiet hours, once-ever resume rule, per-type channels, folder mute, kill criteria |
| R8 breaks SMB/FTP reflection paths in production | Medium | High | Keep rules + staged rollout + Crashlytics same-release; SMB smoke test in Phase 0 matrix |
| Billing edge cases (pending, refund, offline restore) mishandled | Medium | Medium | FR-B3 lifecycle table in QA matrix; entitlement re-query on every start; test-track refund drills |
| Default-player push feels pushy | Low | Medium | Single dismissible snackbar, post-value timing, never repeated |
| MediaStore lag guts the "new video" moment | Medium | Medium | M0 spike; fallback = in-app banner scope change |
| Paywall promises unshipped perks (skins/backglow are P2) | Low | High | FR-B4 copy rule: shipped-only benefits; "coming" items unpriced and undated |
| First Firebase integration triggers data-safety scrutiny | Low | Medium | Analytics+Crashlytics only; form updated same release; debug builds isolated |
| Scope creep (widget, series parsing, FFmpeg pulled forward) | Medium | Medium | §2 non-goals; each has its own ticket and gate in §13 |

## 11. Effort Estimate & Milestones

Assumes 1 Android engineer full-time in the existing stack (Kotlin, Compose, Hilt, Room, DataStore, MMKV, Media3). **Total ≈ 6–7 weeks build + rollout.**

| Milestone | Scope | Est. | Done = |
|---|---|---|---|
| **M0 — See (wk 1)** | Firebase + Crashlytics + core events (FR-A5/6); MediaStore-lag spike; R8 build prepared | 5 d | Events in DebugView; spike memo decides Flow-3 posture; R8 build passes smoke tests |
| **M1 — Be found (wk 2)** | Intent filters + external playback path + default snackbar (FR-A1–3); review prompt (FR-A4); permission-flow fix + Android 14 (FR-A9/10) | 5 d | External open → first frame <1.5s on test matrix; review prompt fires on 3rd completion; partial-access affordance renders on API 34 |
| **M2 — Get paid (wks 3–4)** | Billing + entitlement StateFlow + paywall + upsell surfaces + ad grace (FR-B1–8) | 8 d | Test-track purchase/restore/refund drills pass; ads vanish in-session on purchase; `paywall_view` attribution live |
| **M3 — Bring back (wks 5–6)** | Pulse detection engine + delivery + opt-in + Settings screen (FR-C1–10) | 9 d | All §4 flows pass on API 24/33/36; caps proven by forced-clock tests; toggle-off cancels work |
| **M4 — Prove (wk 7)** | OEM matrix, data-safety form, staged rollout + holdback, guardrail dashboard | 5 d | Phase 1 live; kill switches verified in production |

## 12. Open Questions

1. **Pro price: $4.99 or $5.99?** $4.99 undercuts the MX Pro anchor and suits a first paywall with ad-removal-only value; $5.99 leaves headroom when skins/backglow land. **Recommendation: $4.99 now**, revisit at Pro-perk depth (never raise on existing intenders mid-funnel; regional templates do the geo work).
2. **Launch intro offer?** A "launch week $3.99" creates urgency but flirts with discount-pattern optics. **Recommendation: no** — the brand sells honesty; run a considered seasonal sale later with real data.
3. **Audio files in Pulse new-media?** Music syncs create notification storms. **Recommendation: video-only** (current scope); revisit with per-folder opt-in after 60 days of Flow-3 data.
4. **Onboarding: full 3-slide flow (per `screens/00-splash-onboarding.md`) or the lean permission-rationale fix (FR-A10)?** **Recommendation: lean fix now** — activation friction drops immediately; build full onboarding only if funnel data shows pre-permission drop-off persisting.
5. **App-open ad experiment: schedule it for R2 or leave unscheduled?** **Recommendation: leave unscheduled** until Pro conversion and D7 baselines exist — the experiment's guardrails are meaningless without them.

---

## 13. New Value Features — Suggested Roadmap (post-3.1)

Each candidate scored ICE-R (Impact × Confidence ÷ Effort, risk-gated) per the audit framework; gates name the data this release starts collecting.

| # | Feature | Problem it solves / user value | KPI moved | Effort | ICE-R | Priority · Gate |
|---|---|---|---|---|---|---|
| F-1 | **FFmpeg decoder fallback — "Plays Everything"** (Media3 FFmpeg extension, LGPL-audited per architecture constraint) | Unsupported codecs hard-fail today; hoarders whose files don't play reinstall MX/VLC. Vision doc's headline promise, feature matrix P0 | Playback success ≥99%, retention, rating | 3 wks | 8×0.7÷3 = 1.9 | **P1 — R3 headline.** Gate: 2 wks of `codec_error` data quantifies failure rate first |
| F-2 | **Up Next — series intelligence** (filename episode parsing `S01E05`, next-unwatched rail, episode-order autoplay, new-episode awareness) | Binge-watchers manually hunt the next episode; folder-next is file-order, not episode-order. ~80% of plumbing (positions, rail, autoplay) already shipped | D7, sessions/user | 2–3 wks | 7×0.6÷2.5 = 1.7 | **P2.** Gate: Pulse open-rate data proves the return loop; reuses its candidate layer |
| F-3 | **Continue-Watching home-screen widget** (Glance widget over the same resume-candidate query as Pulse) | Zero-friction re-entry; permanent home-screen presence = daily brand impression | D1→D7 bridge, sessions/user | 1.5 wks | 5×0.6÷1.5 = 2.0 | **P2.** Cheap follow-on once Pulse's query layer exists |
| F-4 | **Chromecast** (Cast SDK; P1 in feature matrix; `CastScreen` stub exists) | The most-requested reach feature in the category; local-file casting is a genuine VLC pain point | Retention (reach), rating | 4 wks | 6×0.6÷4 = 0.9 | **P2 — R3/R4.** Gate: none technical; sequence after F-1 |
| F-5 | **AI subtitles — on-device Whisper** (generate subs for any video, offline; the vision doc's marquee differentiator no major competitor ships) | Foreign/no-sub content becomes watchable; huge ASO keyword surface ("AI subtitles video player"); press-worthy | Acquisition, Pro conversion | 8 wks | 8×0.5÷8 = 0.5 (pre-spike) | **P2 game-changer.** Gate: 2-wk spike — whisper.cpp small model on a mid-tier SoC vs a 45-min episode; kill criteria: RTF > 1.5× or battery > 15%/hr. **Pro-gated from day one** (compute-costly = defensible perk that never blocks basic playback) |
| F-6 | **Pro perk depth: player skins + ambient backglow + gesture-preset export** (all specified Pro in `features/monetization.md`; skins/backglow currently deferred P2) | Pro today = ad removal only; perk depth lifts conversion and ARPPU. Forward-only — nothing shipped-free gets gated | Pro conversion depth, ARPPU | 2–3 wks | 5×0.6÷2.5 = 1.2 | **P2.** Gate: Pro live 60 days; build if conversion plateaus < 1% |
| F-7 | **In-app Trash screen** (restore UI over `QUERY_ARG_MATCH_TRASHED`; audit noted discoverability cost of system-trash-only) | Users who trash files can recover them without leaving the app | Trust/rating; support-complaint deflection | 1 wk | 3×0.7÷1 = 2.1 | **P3.** Quality-of-life; bundle with a file-management pass |
| F-8 | **Rewarded skin trials** ("watch an ad → try Cinema skin for a week") | Monetizes never-payers without new surfaces; paywall trial exposure | ARPU, paywall views | 1 wk | 3×0.4÷1 = 1.2 | **P3.** Gate: F-6 shipped (needs a perk to lend); brand-risk-gated |
| F-9 | **Cross-device resume (account + sync)** | Multi-device users lose their place across phone/tablet/TV | Multi-device retention, LTV | 10+ wks | 6×0.4÷10 = 0.24 | **P3.** Gate: >20% of Pro users look multi-device (Play multi-install signal + settings survey); requires the backend the architecture deliberately avoids — bundle with the P2 cloud subscription or don't do it |
| F-10 | **Android TV** (Compose for TV, per architecture decision) | TV is where long-form local video lives; feature matrix P2 | New surface, sessions | 8 wks | 5×0.5÷8 = 0.3 | **P3.** Gate: TV-intent/cast analytics from F-4 justify it |

**Sequencing logic:** 3.1 (this PRD) builds the measurement + revenue + return-trigger foundation → R3 spends it on the core promise (F-1 "plays everything", F-4 reach) → R4 differentiates (F-5 AI subtitles, F-2/F-3 habit depth). Every feature after 3.1 is gated on data 3.1 collects — that is the point of shipping measurement first.

---

### Assumptions carried from the audit (verify)
1. App is live/near-live on Play (inferred from versionCode 58, release ad IDs); store listing and install base not observed.
2. No usage metrics existed at write time — all targets are directional until the first 2 instrumented weeks set baselines.
3. Solo developer; estimates sized for one engineer.
4. Competitive set: MX Player / VLC / PLAYit (knowledge-based; verify current market moves before the ASO pass).
