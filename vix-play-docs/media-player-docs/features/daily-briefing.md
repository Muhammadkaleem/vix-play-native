# Daily Briefing

## Capability
A once-a-morning brief, assembled from the sources the user chooses — today's calendar, urgent email, mentions/tags, and followed news — delivered at a local time they control, consumable in under two minutes, with every item deep-linking to its source of truth. A daily-habit anchor that gives the app a valuable morning touch-point independent of playback (P1 differentiator). New module `:feature:briefing`; screens `screens/16–18`.

## Principles
- **Respectful** — gets the user out, not in. No infinite feed, no engagement bait; a quiet day says "all clear."
- **User-controlled** — every source is opt-in; nothing is read that wasn't explicitly connected.
- **Preview before live** — a real-data sample must be approved before any schedule fires.

## Sources (v1)
| Source | Scope | Cap |
|---|---|---|
| 📅 Calendar | Today's events; flags conflicts, unusually-early first event (rolling 4-wk median), missing join links, back-to-back >3h, all-day strip | timeline |
| ✉️ Urgent email | Messages since last brief classified urgent (deadline language, VIP sender, direct ask, active thread, meeting <24h) — never the whole inbox; each carries a human flag reason | 5 |
| 💬 Mentions | @-mentions/tags since last brief, grouped by conversation | 10 |
| 📰 News | Headlines for 1–10 user topics, ranked by relevance+recency, link out only | 5 |

Every item deep-links via the provider app when installed, else browser.

## Scheduling & delivery
- Deliver within **±5 min** of the chosen local time on chosen days (default Mon–Fri, 08:00). Generation begins at **T−10 min** (prefetch); content staleness ≤15 min.
- `AlarmManager.setExactAndAllowWhileIdle` → expedited WorkManager generation job → notification (`briefing` channel). Inexact-window fallback if exact-alarm permission is denied (disclosed).
- Zone modes: `FOLLOW_DEVICE` (default) reschedules on device zone change; `FIXED_ZONE` stays anchored. Re-register on `TIMEZONE_CHANGED` / `TIME_SET` / `BOOT_COMPLETED`.
- Offline/Doze: deliver late (labeled "Generated at HH:MM") up to **12:00 local**; after that skip silently (`briefing_skipped_late`). Exactly one brief per day; regeneration replaces in place.

## Generation
- `BriefingGenerator` fans out connector `fetch()` calls in a `supervisorScope` (one failure never cancels siblings), applies caps/ranking, calls the summarization service for gists (minimum excerpt, not retained server-side), and persists a `BriefingRun` + `BriefingItem`s.
- Per-source failure → partial brief with a per-source error row + one-tap fix; never abort the whole brief.
- "Not urgent" feedback demotes similar future items (sender/pattern signal, local) within 2 briefs.

## Privacy & lifecycle
- OAuth tokens Keystore-wrapped (the `NetworkServer.encryptedPassword` precedent); read-only minimal scopes; silent refresh; refresh failure → source `NEEDS_ATTENTION`, surfaced, never silent.
- Source cache in encrypted Room, rolling **14 days**. Analytics carry counters/timings/error codes only — never content, senders, titles, or topics-as-text. Lock-screen redaction on by default. Briefing data excluded from auto-backup and honors Privacy "clear history."
- Disconnect → revoke token (best-effort) + purge that source's data. Feature-off → cancel schedules + revoke all tokens + purge everything, behind a confirmation stating exactly that.
- Google restricted-scope (Gmail/Calendar) verification is a launch gate; lead time owned by eng, started in M1.

## Edge cases
- DST: `ZonedDateTime` scheduling; nonexistent local time → next valid instant; ambiguous → earlier offset (unit-tested).
- Token expired overnight → section error row + one-tap reconnect; nothing scary in the push.
- Zero content everywhere → short "all clear" brief; notification still fires (silence is information).
- Oversized day → caps + "and N more…" deep-link; never paginate the brief.
- Two calendars → dedupe events by fuzzy (title, start, end).
- Backup/restore to a new device → Keystore tokens non-restorable by design; all sources `NEEDS_ATTENTION`, schedule paused until one reconnects.
- Opened mid-generation → progressive per-section render with skeletons.

## Acceptance criteria
- [ ] A real-data sample is shown and explicit **Go live** is required — no schedule exists before approval (verified: kill app after sample, no fire).
- [ ] Delivers within ±5 min on scheduled days, ≥99% across a 14-day Doze/reboot/airplane matrix.
- [ ] `FOLLOW_DEVICE` reschedules on zone change; `FIXED_ZONE` doesn't; DST unit tests green.
- [ ] Caps enforced (5 email / 10 mentions / 5 news); every item deep-links; every urgent email shows a flag reason.
- [ ] Single-source failure → partial brief with one-tap fix; nothing fails silently.
- [ ] Disconnect purges source data + revokes token; feature-off purges everything (DB-verified).
- [ ] No source content in logs or analytics (audited); lock-screen redaction default verified; TalkBack pass.
- [ ] Google restricted-scope verification approved before production rollout.
