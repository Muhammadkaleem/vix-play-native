# Vix Play — Ad Monetisation Strategy

**Status:** proposal for review. No code has been changed.
**Builds on:** `ADMOB_REVENUE_ANALYSIS.md` (root causes RC-1 … RC-7).

## 0. Principles

1. **Account safety before revenue.** One invalid-traffic suspension wipes out everything else.
2. **Fix the ads already requested before adding more.** 55 % of matched ads are currently thrown away.
3. **Full-screen ads only at natural breaks.** Never during playback, never on launch, never in the middle of a task.
4. **Pro must keep its promise.** It removes every ad except the rewarded ads the user chooses to watch.
5. **Every limit lives in config.** Defaults in code, overrides from Firebase Remote Config, which is already initialised in `VixPlayApp`.

### A locked CLAUDE.md decision this changes

CLAUDE.md records *"AdMob. Banner only in library chrome; never on the player surface. No interstitials during playback."* This strategy keeps "never on the player surface" and "no interstitials **during** playback", but **adds interstitials at the moment the user leaves the player.** That is a change to a locked decision. It needs your explicit approval, and CLAUDE.md should be updated once it is approved.

---

## 1. What's live today (audit summary)

| Item | Current state |
|---|---|
| SDK | `play-services-ads` 25.4.0, UMP 3.1.0. Consent goes through `AdsController.ensureConsentAndInit` in `MainActivity.onCreate`. Max content rating G. |
| Banner (main) | App-level `Scaffold` bottom bar, above the mini-player. Shown on Home, Audio, Network, Search, History and others. Hidden in the Settings tab, the folder browser and full-screen routes. |
| Banner (video list) | First row of the in-folder video grid. |
| Native (folder list) | After the 3rd folder row. |
| Native (video list) | After every 8th video. Needs ≥ 9 videos, so it rarely renders. |
| Rewarded | "Watch ad → +8 h Pro, bank up to 48 h". Entry points: the Home `PremiumCard`, its details modal, and Settings → Vix Pro Rewards. |
| Interstitial / app-open | **None.** The `APP_OPEN` unit ID exists but is parked. |
| Pro entitlement | `ProRepository` (DataStore `vixplay_pro`): absolute `pro_until`, idempotent transaction IDs, rollback protection within one boot, 48 h bank cap. |
| What Pro actually gates | **Only ads.** `AdsViewModel.adsEnabled = controller.adsEnabled && !isPro` is the only place Pro is read outside the Pro screens. |
| Frequency control | None (no full-screen ads exist). |
| Ad analytics | Firebase events `watch_ad_tapped`, `reward_earned`, `pro_extended`, `rewarded_ad_unavailable`, `pro_rewards_viewed`. **No impression revenue tracking.** |

### ⚠️ Pro claims that don't match the app [Confirmed]

`PremiumModals.kt` lists six Pro perks: *Ad-Free, Background Play, All Formats & 4K, Equalizer, Speed Control (0.25×–4×), Sleep Timer*. **Only "Ad-Free" is actually gated.** The other five are free for everyone today: background play is a toggle in Settings, and the equalizer, speed control and sleep timer have no Pro check. Telling users they are unlocking features they already have is misleading. That hurts trust and reviews, and is the kind of claim Play's deceptive-behaviour policy covers. It has to be fixed whatever else is decided (§5).

---

## 2. Free (non-Pro) users: formats and placements

### 2.1 Banner: keep, and fix the waste
| Decision | Detail |
|---|---|
| Keep the main banner in the bottom bar | It is the main revenue source (49 %). Anchored adaptive, sits above the navigation bar, and doesn't jump the layout. |
| Keep exclusions | Player, audio player, splash, Settings tab, multi-select mode. |
| Fix lifecycle | Keep **one `AdView` alive** across tab switches instead of rebuilding it. Call `pause`/`resume`/`destroy` with the lifecycle. Reserve the banner's height before it loads so content doesn't jump. |
| Banner_VideoList | Fix the reload churn the same way. If its match rate is still below 30 % after 2 weeks, **retire the unit** and let the main banner cover the folder browser. |
| Collapsible banner | **Not now.** Allowed by policy, but intrusive in a library you scroll through. Revisit as an A/B arm once DAU makes A/B testing meaningful (§6). |

### 2.2 Native: keep, cache, and actually render
| Decision | Detail |
|---|---|
| Folder list | Keep after row 3. Cache the loaded ad per screen so scrolling never fires a new request. |
| Video list | Interval 8 → **first ad after video 4, then every 10**, with a minimum folder size of 5. Remote-configurable. |
| Home dashboard | **Experiment only:** one native card between the "Recently added" and "Your folders" sections, compared against the bottom banner on Home (§6). Not in the first release. |

### 2.3 Interstitial: new, tightly capped
**Placement, one trigger only to start:** *Player exit.* The interstitial shows when the user leaves the video player and returns to the library, after watching at least **60 s** of content in that player visit. Leaving a video is a natural break, and nothing is interrupted.

Explicitly **excluded**:
- Entering the player (it would delay content the user just asked for).
- Any time the player is playing, paused or in Picture-in-Picture.
- **Leaving the player after an external open** (another app sent the file): Back returns to that other app, so an ad there would be "unexpected".
- Swapping to the next video via Up Next ("Play next" / "Play again").
- Cold launch, and the splash screen.
- During or right after: file move/copy, delete or trash consent, rename, permission dialogs, the update snackbar, a rewarded ad.
- In the audio player / background audio session.

**Second trigger (phase 2, gated by data):** tab switch to Home after **≥ 5 min** in the session. Added only if player-exit inventory turns out too thin.

### 2.4 App-open ad: not now
The unit exists, but in a video player an app-open ad collides with the most common entry: opening a file from another app. It also adds a full-screen ad right next to the interstitial. Revisit only as a **warm-resume-only** experiment (return after ≥ 4 h in the background, never on an external open), and only after the interstitial data is in.

### 2.5 Rewarded: keep the mechanic, fix the delivery
- Keep "watch → +8 h ad-free, bank up to 48 h". The user chooses to watch and sees the reward amount before it plays (existing modal text: "Watch Ad · Earn 8 hours").
- **Stop preloading on Home open.** Preload when the Pro card is actually visible on screen or the Pro details modal opens. Re-preload after a completed watch, as today.
- **Show a loading state** ("Loading ad…", with a cancel option) when no ad is warm, instead of a silent 10-second wait.
- Respect the 1-hour expiry of a warm ad.
- After a failed show, preload again (currently it doesn't).

---

## 3. Pro users

| Rule | Detail |
|---|---|
| No banners, no native ads, no interstitials, no app-open ads | Pro = no ads the user didn't ask for. Already true for banner and native through `AdsViewModel`. Interstitials must check the same source. |
| Rewarded still available | To extend Pro time ("Extend +8h"), as today. |
| Source of truth | `ProRepository.isProActive` only. No second copy of the flag. |
| Expiry mid-session | Ads come back at the next eligible placement, not instantly. Interstitial frequency counting starts fresh at expiry, so nobody gets an ad the moment Pro ends. |
| Persistence | Already persisted in DataStore as an absolute expiry, so it survives restarts. |
| Tamper resistance | Within-boot clock rollback is handled. **Full protection (rooted device editing DataStore, clock changes across reboots) needs server-side verification (SSV)**, which needs a backend. At the current scale the cost of that abuse is negligible, so SSV is deferred until a backend exists. It is listed as an accepted risk rather than solved. |

---

## 4. Frequency rules (starting values; all remote-configurable)

These are **hypotheses to measure, not final values.**

| Rule | Starting value | Why |
|---|---|---|
| First-launch protection | No interstitial for the first **24 h after install**, and none in **sessions 1–2** | First impressions drive retention |
| Minimum watch before a player-exit ad | **60 s** in that player visit | Bouncing out of a clip shouldn't be "punished" |
| Minimum gap between any two full-screen ads | **4 min** (the brief's 3–5 min range) | Keeps it reasonable for people who watch several short clips |
| Max interstitials per session | **3** | Session = app in the foreground; a new session starts after 30 min in the background |
| Max interstitials per day | **8** | Upper bound for heavy users |
| Cooldown after a rewarded ad | **10 min** with no interstitial | Someone who just chose to watch an ad shouldn't immediately get another |
| No back-to-back full-screen ads | Rewarded, interstitial and app-open share one "last full-screen ad" timestamp | |
| Sensitive states | Suppressed (see §2.3 exclusions) | |
| Ad not ready | **Skip silently; never wait for one to load** | No spinners at a natural break |
| Kill switch | `ads_interstitial_enabled` | Turn interstitials off remotely if retention dips |

**Persistence:** install time, session count, last full-screen ad time and today's count go in DataStore. The per-session count stays in memory.

**Expected effect, as a formula only.** The data can't support a dollar estimate yet. With *S* sessions a day, *p* the share of sessions that hit a qualifying player exit, *f* fill × show, and *e* the interstitial eCPM:
`added revenue/day ≈ S × p × f × min(capped ads/session) × e / 1000`.
*S*, *p* and *e* are unknown until instrumentation ships, so no revenue increase is promised.

---

## 5. Pro conversion strategy

Pro here is time-limited and earned by watching ads, and both the ads and Pro bring in revenue. The goal is that people who dislike ads opt into rewarded views (the highest eCPM) instead of uninstalling.

1. **Fix the Pro promise first (required).** Either:
   - **(A) Honest copy, recommended now:** Pro = *Ad-free: no banners, no full-screen ads*. Drop the five perks that are already free, or move them under a line like "Everything else stays free" (which is itself a selling point).
   - **(B) Re-gate some features.** Product decision; not recommended right now. Taking features away from existing users causes review backlash, and it pushes toward the deferred Billing work.
2. **Context prompt after an interstitial.** After the user's **2nd interstitial in a session**, show a small, dismissible bottom sheet (not full-screen): *"Prefer no ads? Watch one short ad for 8 hours ad-free."* At most **once a day**, never to Pro users, and **never with a second full-screen ad**. The prompt is the offer; the rewarded ad plays only if they tap.
3. **Pro-expiry nudge.** When Pro has under 1 h left, the Home card shows "Extend +8h". It already supports this, so no new surface is needed.
4. **No dark patterns:** no fake countdowns, no "close" button disguised as "watch", no ad started without the user asking.
5. **Measure:** rewarded starts / completions per DAU, the share of interstitial-exposed users who start a rewarded ad, Pro activations per DAU, and retention of rewarded watchers vs non-watchers (§7 of the technical design).

Play Billing v6 (a paid ad-free tier) stays **deferred by your earlier decision**. The design leaves room for it: `ProRepository` can take a second entitlement source later.

---

## 6. Experiments, and their limits at this scale

At roughly 7 sessions a day (estimate), **no A/B test will reach statistical significance**. Until DAU is in the hundreds, changes roll out **one at a time with guardrails**: watch D1/D7 retention, uninstalls and ratings for 2 weeks per change. The planned experiments (frequency caps, banner vs native on Home, action vs time triggers, prompt timing) are in `AD_REVENUE_OPTIMISATION_ROADMAP.md` and run through **Firebase A/B Testing on Remote Config** once traffic allows.

---

## 7. Policy compliance checklist

| Risk | How the strategy handles it |
|---|---|
| Unexpected interstitials | Only at player exit after real watching. Never on launch, external-open return or in-flow actions. |
| Too many full-screen ads | Session/day caps, a 4 min gap, a shared full-screen timestamp, first-session grace. |
| Ads during playback / on the player | None. Players, PiP and audio are excluded. |
| Accidental clicks | The banner is separated from nav controls (existing). Native ads have a clear "Ad" badge and are styled like cards, not list rows, so they're never confused with a video row. No ad appears right after a tap in the same spot. |
| Ads overlapping controls | The banner sits in reserved layout space and never overlaps. |
| Rewarded consent and delivery | User-initiated, reward shown before playing, credited only on `onUserEarnedReward`, idempotent transaction IDs (existing). |
| Invalid traffic | Register test device IDs for every team phone. Team members never click live ads. Release QA uses test devices. |
| Pro promise | Fix the misleading perk list (§5.1). Pro shows no ads except ones the user chooses. |
| Content rating | G cap stays (`AdsController`). |
| Families | Not child-directed (existing tags). |

## 8. Risks

| Risk | Mitigation |
|---|---|
| Interstitials hurt retention or ratings | Conservative caps, kill switch, guardrail metrics |
| Player-exit trigger fires too rarely to matter | Phase-2 secondary trigger, decided by data |
| AdMob serving limits from past invalid clicks | Check the Policy center now; send no further self-traffic |
| The real fix (more users) is outside ad engineering | ASO work (`docs/aso/` exists) runs in parallel |
