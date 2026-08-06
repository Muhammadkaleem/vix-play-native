# Growth Foundation (Release 3.1 — "Measure, Monetize, Return")

Post-MVP release that turns a working player into a distributable, measurable, revenue-earning, habit-forming product. The player works; the *business scaffolding* doesn't exist yet. This is the umbrella for four feature docs: `analytics.md`, `distribution.md`, `pro-billing.md`, `notifications.md`.

## Problem — four gaps
1. **Distribution** — the app doesn't register for `ACTION_VIEW`/`ACTION_SEND`, so it only plays content browsed from inside it. It can't be a user's real player. → `distribution.md`
2. **Measurement** — no analytics or crash reporting; every product decision (including the FFmpeg go/no-go) is a guess. → `analytics.md`
3. **Revenue** — the ad seam ships but there is no way to pay to remove ads; no Pro. → `pro-billing.md`
4. **Re-engagement** — nothing brings a lapsed user back; the app can't reach out respectfully. → `notifications.md`

## Goals
- Be selectable as the default video/audio player and appear in the system chooser.
- Instrument the install → activation → playback → revenue funnel and watch Play vitals.
- Ship a one-time **VixPlay Pro** that removes ads and unlocks perks, driven by the existing `AdsController.adsEnabled` seam.
- Bring lapsed users back with a few local, opt-in, frequency-capped notifications.

## Non-goals
- No subscription tiers (one-time Pro only until a recurring perk exists).
- No backend for billing verification (local Play verification per the architecture decision).
- No server-pushed / third-party notification content — everything is derived on-device.
- No new analytics vendor beyond Firebase Analytics + Crashlytics.

## Success metrics (day 90)
| Metric | Target |
|---|---|
| Weekly Active Viewers (≥1 completed playback ≥60s/wk) | baseline +20% |
| Activation (install → first completed playback) | ≥ 70% |
| D1 / D7 / D30 retention | 28% / 12% / 5% |
| Crash-free sessions / ANR | ≥ 99.5% / < 0.47% |
| Playback success (start vs codec_error) | ≥ 98% |
| Free → Pro conversion | measured from zero; establish a baseline |

## Guardrails
- No ads on the player surface (unchanged from `monetization.md`); Pro removes ads app-wide via one seam.
- Analytics carry counters/timings/error codes only — no content, titles, or PII.
- Every notification path is opt-in, frequency-capped, and one-tap disable.

## Rollout
- Staged Play rollout behind remote-flag posture; watch crash-free and ANR as release-exit gates.
- Pro validated in a Play test track before production.

## Roadmap (post-3.1, §13 of source PRD)
| # | Item | Notes |
|---|---|---|
| F-1 | FFmpeg extension (software decode) | Gated on `codec_error` data quantifying the real gap first |
| F-2 | "Up Next" / autoplay queue depth | Builds on the ExoPlayer-native queue |
| F-3 | Home-screen widget (Continue Watching) | |
| F-4 | Chromecast (finish Cast/DLNA) | `screens/09` |
| F-5 | AI subtitles (on-device / Whisper) | |
| F-6 | Pro perk depth (skins, backglow, subtitle online search) | `pro-billing.md` upsell targets |
| F-7 | Trash / recoverable delete refinement | |
| F-8 | Rewarded skin trials | ad-format experiment |
| F-9 | Cross-device resume | needs a backend — deferred |
| F-10 | Android TV | `androidx.tv` |

## Related
Daily Briefing (`features/daily-briefing.md`, `screens/16–18`) is a separate P1 differentiator release, not part of 3.1, but shares the retention thesis: a daily-habit surface is the strongest D30 driver.
