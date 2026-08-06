# Briefing Settings — PRD

**Route:** `/briefing/settings` · **Priority:** P1
**Entry:** Settings → Daily Briefing (when set up) · briefing footer "Adjust briefing" link.

## Purpose
Manage a live briefing without re-running setup: sources, accounts, schedule, and lifecycle (pause, purge, turn off). Feature spec: `features/daily-briefing.md`.

## Layout
- **Sources** — per-source on/off toggles.
- **Accounts** — per source: connection state (Connected / Needs attention / Disconnected), Reconnect, and Disconnect + purge.
- **News topics** — add/remove/reorder editor (1–10).
- **Schedule** — delivery time, days, time-zone mode (`FOLLOW_DEVICE` / `FIXED_ZONE`), and the detected/fixed zone.
- **Privacy** — lock-screen detail toggle (off by default); link to the plain-language privacy sheet.
- **This morning** — "View today's brief," Regenerate today's brief (replaces in place).
- **Lifecycle** — Pause / Resume; Delete briefing history (last 14 briefs); **Turn off entirely** (confirmation stating: cancels schedules, revokes all tokens, purges briefing data).

## States
Active · Paused · Needs attention (one+ source in `NEEDS_ATTENTION`, surfaced at top) · Off (feature disabled).

## Edge cases
- Reconnect uses the same OAuth path as setup; never requires full re-setup (FR-18).
- Disconnecting a source revokes its token (best-effort) and removes its section from future briefs immediately.
- Notifications permission revoked later → a fix-it row here; brief still generates and the home card becomes primary.
- Turn-off confirmation must state the purge explicitly before proceeding.

## Acceptance criteria
- [ ] Source toggles, schedule changes, and pause/resume take effect from the next scheduled fire with no reconnection.
- [ ] Disconnect purges that source's cached data and revokes its token (DB-verified).
- [ ] Turn off cancels the schedule, revokes all briefing tokens, and purges all briefing data, behind an explicit confirmation.
- [ ] `NEEDS_ATTENTION` sources are surfaced here and offer one-tap reconnect.
- [ ] Delete history clears the local `BriefingRun`/`BriefingItem` cache.
