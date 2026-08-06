# Library Pulse (Re-engagement Notifications)

## Capability
Bring lapsed users back with a small number of respectful, locally-generated notifications built from the user's own library and watch history — never spam, never server-pushed content. Re-engagement is the fourth growth gap: the app has no reason to reach a user who hasn't opened it (`product/05-growth-foundation.md`, Workstream C).

## Triggers (all local, all opt-in)
- **Resume nudge** — one unfinished video in Continue Watching, fired after N days idle: "You're 20 min from the end of <title>." Deep-links straight into the player at the resume position.
- **New in library** — a scan found videos added since the last session: "12 new videos in your library."
- **Weekly digest (optional)** — a quiet weekly "what you watched / what's waiting" summary.

All content is derived on-device from Room (resume positions, history) + the MediaStore scan. No network, no third-party content.

## Controls & scheduling
- Dedicated `pulse` notification channel, tunable independently of the `briefing` and playback channels.
- WorkManager-scheduled, frequency-capped (at most ~2/week), quiet-hours aware, and globally toggleable in Settings; Android 13+ runtime notification permission requested with rationale (splash/onboarding flow).
- Every notification carries a one-tap "turn these off" path; respect it immediately.

## Analytics
`pulse_scheduled {type}` · `pulse_shown {type}` · `pulse_opened {type}` · `pulse_disabled {source}` — counters only, no titles (`analytics.md`).

## Edge cases
- Notification permission denied/revoked — schedule nothing; surface a fix-it row in Settings, don't nag.
- Empty library / no history — no nudge has content, so nothing fires (silence over manufactured content).
- Private-folder items never appear in any notification.
- Frequency cap and quiet hours are enforced even across trigger types (a user never gets two in a day).

## Acceptance criteria
- [ ] Resume nudge deep-links into the player at the correct resume position.
- [ ] Frequency cap (~2/week) and quiet hours hold across all trigger types.
- [ ] A single global toggle and per-notification "turn off" both work and are honored immediately.
- [ ] Nothing fires without library/history content; private-folder content is never referenced.
- [ ] Android 13+ permission requested with rationale; denial is a calm Settings state, not repeated prompts.
