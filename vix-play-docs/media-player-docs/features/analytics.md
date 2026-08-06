# Analytics & Crash Reporting

## Capability
Instrument the full install → activation → playback → revenue funnel, and measure crash-free/ANR rates. Nothing is measurable today (no analytics/crash SDK in the build); this is a launch-blocking requirement of the 3.1 Growth Foundation release (`product/05-growth-foundation.md`).

## Approach
- Firebase **Analytics + Crashlytics** via `google-services.json` in `app/` (the architecture's own plan). No other trackers.
- Build-type-scoped config — debug builds never report to the production project (mirrors the `AdUnits` debug/release split).
- Play **Data safety** form updated in the same release.

## Core events
`first_open` · `permission_result {granted, type}` · `library_loaded {video_count}` · `session_start {source: launcher|notification|external|pip}` · `playback_start {source}` · `playback_complete` · `codec_error {codec_name, container}` · `ad_impression {placement}` · `paywall_view {source}` · `pro_purchase {price_micros, currency}` · `review_prompt_shown` · `pulse_*` (see `notifications.md`).

## Targets (day 90)
- North star — Weekly Active Viewers (≥1 completed playback ≥60s/week): baseline +20%.
- Activation (install → first completed playback): ≥ 70%.
- D1 / D7 / D30 retention: 28% / 12% / 5%.
- Crash-free sessions ≥ 99.5%; ANR < 0.47% (Play vitals).
- Playback success ≥ 98% (`playback_start` vs `codec_error`) — this figure sizes the FFmpeg decision.

## Edge cases
- No content payloads in events — counters, durations, and error codes only.
- `codec_error {codec_name}` fires from the existing player error overlay; it is the data that quantifies the real "plays everything" gap before FFmpeg work is scheduled.

## Acceptance criteria
- [ ] Events visible in Firebase DebugView; debug builds isolated from the production project.
- [ ] Crash-free ≥ 99.5% and ANR < 0.47% dashboards reviewed as a release-exit criterion.
- [ ] Data-safety form updated and submitted in the same release.
