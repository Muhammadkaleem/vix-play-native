# Vix Play — Ad Monetisation Technical Design

**Status:** phases 0–2 implemented 2026-11-18 (see the CLAUDE.md log entry). Deviations from this design: the `SuppressReason` set was not built — the single player-exit placement can't coincide with a file operation or dialog, so `BreakContext` carries only Pro / watch time / external open; native ads live in a destination-scoped `NativeAdsViewModel` instead of inside `FolderBrowserViewModel`; watch time is measured off the shared `PlayerController.player`, so `PlayerScreen` is unchanged; the player-exit hook is a route-change observer (system Back bypasses the player's `onBack`). Not yet built: persisting a pending reward across process death.
**Constraint:** extend the existing `ads/` package and `ProRepository`. **No second monetisation architecture and no new third-party dependencies.** Everything needed (Mobile Ads SDK, UMP, Firebase Analytics, Remote Config, DataStore, Hilt) is already in the build.

## 1. Component map

The brief suggested component names. Here is how each maps onto what exists:

| Brief's name | Becomes | New / existing |
|---|---|---|
| AdManager | **`AdsController`**: consent, SDK init, the single `adsAllowed` flow, suppression state | existing, extended |
| AdFrequencyManager | **`AdFrequencyPolicy`** (pure Kotlin, no Android types) + **`AdFrequencyStore`** (DataStore) | new |
| AdPlacementManager | **`AdPlacement`** enum + `InterstitialAdManager.onNaturalBreak(placement, …)` | new (small) |
| RewardedAdManager | **`RewardedAdManager`** | existing, fixed |
| (interstitial) | **`InterstitialAdManager`**, same shape as `RewardedAdManager` | new |
| ProEntitlementManager | **`ProRepository`** (already the single source of truth) | existing, unchanged |
| AdAnalyticsManager | **`AdEvents`**: thin wrapper over Firebase Analytics + `OnPaidEventListener` | new |
| (config) | **`AdConfig`**: defaults + Remote Config overrides | new |
| (native caching) | **`NativeAdHolder`**, owned by a view model | new |

```
MainActivity ── ensureConsentAndInit ──► AdsController ◄── ProRepository.isProActive
                                         │  adsAllowed: StateFlow<Boolean>   (consent ∧ ¬Pro ∧ enabled)
                                         │  suppressed: StateFlow<Set<Reason>>
          ┌──────────────────────────────┼──────────────────────────────┐
          ▼                              ▼                              ▼
   AdBanner / AdNativeCard      InterstitialAdManager            RewardedAdManager
   (Compose, lifecycle-aware)   ├─ AdFrequencyPolicy (pure)      (shares lastFullScreenAt)
   NativeAdHolder (VM-owned)    ├─ AdFrequencyStore (DataStore)
                                └─ AdConfig (Remote Config)
                       all ──► AdEvents (Firebase Analytics + paid-event revenue)
```

## 2. AdsController changes

- Replace `val adsEnabled: Boolean = true` with
  `val adsAllowed: StateFlow<Boolean> = combine(canRequestAds, pro.isProActive, config.adsEnabled) { c, p, e -> c && !p && e }`.
  The Pro check moves out of `AdsViewModel` so non-Compose code (the interstitial manager) reads the same value. `AdsViewModel` just re-exposes it.
- **Test devices:** add `setTestDeviceIds(BuildConfig.AD_TEST_DEVICE_IDS)` to the existing `RequestConfiguration` builder. The IDs come from a `local.properties` key and are not committed.
- **Suppression set:** `suppressed: StateFlow<Set<SuppressReason>>`, where `SuppressReason ∈ {PLAYER_ACTIVE, PIP, EXTERNAL_OPEN, FILE_OPERATION, SYSTEM_DIALOG, FULLSCREEN_AD}`. Screens call `suppress(reason)` and `release(reason)` from a `DisposableEffect`, e.g. the move/copy progress dialog and the delete-consent launcher. That keeps "is something sensitive happening?" in one place instead of scattered booleans.

## 3. Frequency management

### 3.1 `AdConfig` (immutable snapshot)
```kotlin
data class AdConfig(
    val adsEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
    val minGapMs: Long = 4 * MIN,
    val maxPerSession: Int = 3,
    val maxPerDay: Int = 8,
    val installGraceMs: Long = 24 * HOUR,
    val graceSessions: Int = 2,
    val minWatchMs: Long = 60 * SEC,
    val rewardedCooldownMs: Long = 10 * MIN,
    val sessionTimeoutMs: Long = 30 * MIN,
    val nativeVideoListFirst: Int = 4,
    val nativeVideoListInterval: Int = 10,
)
```
Read from Remote Config keys `ads_*` with these defaults (`setDefaultsAsync`). The fetch already runs in `VixPlayApp`. If the fetch fails, the defaults apply, so ads never break because of config.

### 3.2 `AdFrequencyPolicy` (pure and unit-testable)
```kotlin
data class AdFrequencyState(
    val installedAt: Long, val sessionCount: Int,
    val lastFullScreenAt: Long, val lastRewardedAt: Long,
    val dayKey: Int, val todayCount: Int, val sessionShownCount: Int,
)
sealed interface Decision { data object Show : Decision; data class Skip(val reason: String) : Decision }

fun decide(now: Long, s: AdFrequencyState, c: AdConfig, ctx: BreakContext): Decision
```
Checks run in order, and the first failing check returns `Skip(reason)`, which is logged for analytics:
`!interstitialEnabled` → `ctx.isPro` → `ctx.suppressed.isNotEmpty()` → `now - installedAt < installGraceMs` → `sessionCount <= graceSessions` → `ctx.watchedMs < minWatchMs` → `now - lastFullScreenAt < minGapMs` → `now - lastRewardedAt < rewardedCooldownMs` → `sessionShownCount >= maxPerSession` → `todayCount(now) >= maxPerDay` → `Show`.

There is no clock or Android dependency, so every rule gets a JUnit test with a fake `now`.

### 3.3 `AdFrequencyStore`
DataStore `vixplay_ads` holds `installed_at`, `session_count`, `last_fullscreen_at`, `last_rewarded_at`, `day_key`, `today_count`. The per-session count stays in memory. **Session start:** `ProcessLifecycleOwner` `ON_START` when the app has been backgrounded longer than `sessionTimeoutMs` (or on a cold start) → `session_count++`. The existing ProcessLifecycle dependency covers this; nothing new is needed.

## 4. Ad lifecycles

### 4.1 Interstitial (`InterstitialAdManager`, `@Singleton`)
```
preloadIfEligible()        ← called on library ON_RESUME and after each dismiss; no-op if !adsAllowed,
                             already loading, or ad warm & < 55 min old
onNaturalBreak(activity, PLAYER_EXIT, watchedMs)
   ├─ decide(...) == Skip  → AdEvents.interstitialSkipped(reason); return
   ├─ no warm ad           → AdEvents.interstitialSkipped("not_ready"); preloadIfEligible(); return
   └─ show(activity)
        onAdShowedFullScreenContent → store.recordFullScreen(now); suppress(FULLSCREEN_AD); AdEvents.shown
        onAdDismissedFullScreenContent → release(FULLSCREEN_AD); ad = null; preloadIfEligible()
        onAdFailedToShow → release; ad = null; AdEvents.showFailed; preloadIfEligible()
```
- One warm ad at a time. Track `loadedAt`, and treat an ad older than 55 min as expired (the SDK's limit is 60).
- **Never wait for a load at a natural break.** No ad ready means no ad.
- **Hook point:** the player route's `onBack` in `VixPlayNavGraph` (and the Up Next overlay's Back). Pop the back stack **first**, then call `onNaturalBreak` with the `Activity`, so the ad covers the library, not the player. `PlayerScreen` reports `watchedMs` (wall time with `isPlaying == true` this visit) through `PlayerViewModel`. External-open visits (player is the root, `finish()` path) set `EXTERNAL_OPEN` and never reach the hook.
- **Activity safety:** `show()` only when `activity.lifecycle.currentState.isAtLeast(RESUMED)` and `!isFinishing`. The `Activity` is never stored; it is passed per call. Callbacks capture only the application context.

### 4.2 Rewarded (fixes to the existing manager)
| Problem found in the audit | Fix |
|---|---|
| Preload on every Home open (`PremiumViewModel.init`) | Remove it from `init`. Preload when the `PremiumCard` becomes visible (`LaunchedEffect` in the card) or the modal opens. |
| No expiry handling | Track `loadedAt`. Throw away and reload after 55 min. |
| `onAdFailedToShowFullScreenContent` doesn't preload | Preload there too. |
| Silent wait of up to 10 s when cold | Expose `state: StateFlow<RewardedState { Idle, Loading, Ready }>`. The UI shows "Loading ad…" and a cancel button. Cancelling drops the pending show callback. |
| `earned` → credit only on dismiss | Keep (it's correct: credit after the ad closes). Additionally, **persist the pending transaction ID in `onUserEarnedReward`** so a process death between earn and dismiss still credits on the next launch, idempotently through the existing transaction-ID set. |
| Not coordinated with interstitials | `onAdShowed` writes `last_rewarded_at` and `last_fullscreen_at` to `AdFrequencyStore`. |

### 4.3 Banner (`AdBanner`)
- Remember the `AdView` with `remember(adUnitId)` and **attach a `DisposableEffect` that calls `destroy()`**. Observe `LocalLifecycleOwner` and call `pause()` / `resume()` on `ON_PAUSE` / `ON_RESUME`.
- **Main banner stays alive across tabs.** The host in `VixPlayNavGraph` currently drops it out of composition on the Video tab. Instead, keep it composed and hide it with zero height (`Modifier.height(0.dp)` plus `pause()`), so returning to another tab reuses the loaded ad instead of firing a new request. (Pausing also stops the refresh timer, so no impressions are counted while it's hidden.)
- **No layout jump:** reserve the adaptive size's height (`AdSize.getHeightInPixels`) from the first frame.
- `adListener` → `AdEvents` (loaded / failed with error code / clicked / impression). `setOnPaidEventListener` → revenue (§5).

### 4.4 Native (`NativeAdHolder`)
- A small class owned by the **screen's view model** (`FolderBrowserViewModel`): `MutableStateFlow<NativeAd?>` per slot key, `load(slot)` only if the slot is empty and not loading, `destroy()` everything in `onCleared()`.
- `AdNativeCard` becomes a pure renderer of a `NativeAd`. Scrolling an item off-screen and back **reuses the ad**, with no new request.
- One ad per slot per screen visit. Folder list: 1 slot. Video list: slots at `nativeVideoListFirst` and then every `nativeVideoListInterval`, from config.
- `NativeAdOptions`: `setMediaAspectRatio(LANDSCAPE)` and AdChoices in the top-right corner (the default position; keep it explicit).

### 4.5 Failure handling (all formats)
- Log `LoadAdError.code` through `AdEvents.loadFailed(unit, code)`.
- **No tight retry loops:** native and banner don't retry within a screen visit. The interstitial and rewarded preloads use exponential backoff (30 s, 60 s, 120 s, capped at 5 min) and reset on success. That stops the bursts of requests seen on 31 Oct (81 requests, 11 % matched).
- No ad is ever shown before `canRequestAds` (UMP). That's already the case and stays.

## 5. Analytics (`AdEvents`)

Firebase Analytics collection is already on in release builds. Events (snake_case, ≤ 40 characters):

| Event | Params | Source |
|---|---|---|
| `ad_impression` (**Firebase standard event**) | `ad_platform=admob`, `ad_source`, `ad_format`, `ad_unit_name`, `value`, `currency` | `OnPaidEventListener` on every format. Turns on revenue / ARPDAU in Firebase and GA4. |
| `ad_load` / `ad_load_failed` | `ad_format`, `placement`, `error_code`, `latency_ms` | load callbacks |
| `ad_click` | `ad_format`, `placement` | `onAdClicked` |
| `interstitial_shown` / `interstitial_skipped` | `placement`, `reason` (the `Decision.Skip` reason) | `InterstitialAdManager` |
| `rewarded_start` / `rewarded_complete` / `rewarded_abandon` | `entry_point` (home_card / modal / settings / post_interstitial_prompt) | `RewardedAdManager` |
| `pro_activated` / `pro_extended` / `pro_expired` | `bank_ms` | `ProRepository` transitions |
| `pro_prompt_shown` / `pro_prompt_accepted` | `trigger` | the §5.2 strategy prompt |

User properties: `is_pro` (true/false), `ad_cohort` (Remote Config experiment arm).

Existing events (`watch_ad_tapped`, `reward_earned`, …) are kept for continuity, or renamed with a note in the analytics doc.

**AdMob ↔ Firebase linking** (in the console, not code): link the AdMob app to Firebase project `devbytes-d260c`. That fills the empty user metrics (DAU, ARPDAU, impressions per user) seen in the analysis.

### Before/after dashboard (Firebase / GA4 exploration, or a weekly export)
- ARPDAU = Σ `ad_impression.value` / DAU, split by format.
- Impressions per DAU by format.
- Show rate = impressions / loads, by unit (shows the waste).
- Interstitials per session and the distribution of skip reasons.
- Rewarded funnel: tapped → start → complete → `pro_activated`.
- Guardrails: D1/D7 retention (Firebase cohorts), Play Console uninstalls and ratings.
- Baseline = the four weeks in `ADMOB_REVENUE_ANALYSIS.md`.

## 6. Pro entitlement integration

- `ProRepository.isProActive` is the only Pro read. `AdsController.adsAllowed` combines it; `InterstitialAdManager` calls `adsAllowed.value` at decision time; banner and native hosts already collect it through `AdsViewModel`.
- When Pro expires, `pro_expired` is logged on the next `isProActive` true → false transition (an observer in `AdsController`). No interstitial is allowed until a new natural break, which is guaranteed because the decision only runs at breaks.
- Persistence and rollback protection: unchanged. SSV is deferred (needs a backend), as recorded in the strategy doc.

## 7. Testing plan

| Layer | Tests |
|---|---|
| `AdFrequencyPolicy` | JUnit, one test per rule, plus combinations (grace + cap, rewarded cooldown vs gap, day rollover at local midnight, session timeout) |
| `AdConfig` parsing | Bad or missing Remote Config values fall back to defaults |
| Rewarded credit | Duplicate transaction ID → `Duplicate`; earn without dismiss → credited on next launch (pending transaction ID) |
| Device: free user (test ad units, debug) | Banner on Home, no reload on tab round-trip; native reused after scrolling; interstitial only after ≥ 60 s of watching then Back, never on external open / PiP / Up Next / launch; caps respected; rewarded shows a loading state |
| Device: Pro user | No banner, native or interstitial on any screen; survives restart; rewarded "Extend" works |
| Regression | Playback start/resume, Back from player (normal and external), PiP, background audio, Up Next, move/copy/trash flows unaffected |

Device testing uses **test ad units (debug)** and **registered test devices (release)**. Never click a live ad.

## 8. Files touched (estimate)

- **New:** `ads/AdConfig.kt`, `ads/AdFrequencyPolicy.kt`, `ads/AdFrequencyStore.kt`, `ads/InterstitialAdManager.kt`, `ads/NativeAdHolder.kt`, `ads/AdEvents.kt`, `test/.../AdFrequencyPolicyTest.kt`.
- **Edited:** `AdsController`, `AdsViewModel`, `AdUnits` (+interstitial unit, which must be created in the AdMob console), `AdBanner`, `AdNativeCard`, `RewardedAdManager`, `PremiumViewModel`, `PremiumModals` (honest perk list + loading state), `DashboardScreen` (preload trigger), `FolderBrowserScreen`/`ViewModel` (native holder, interval), `VixPlayNavGraph` (banner host, player-exit hook), `PlayerScreen`/`PlayerViewModel` (watched time), `app/build.gradle.kts` (test-device IDs field), `CLAUDE.md` (decision change).
