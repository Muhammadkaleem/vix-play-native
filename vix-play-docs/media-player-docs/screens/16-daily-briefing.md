# Daily Briefing — PRD

**Route:** `/briefing` · **Priority:** P1
**Entry:** Scheduled notification (`briefing` channel) · home-surface briefing card (until noon local) · Settings → Daily Briefing → "View today's brief."

## Purpose
The morning brief itself — a single scrollable, dark-first surface that answers "what do I need to know before I start?" in under two minutes, then ends. Feature spec: `features/daily-briefing.md`.

## Layout (fixed section order, each collapsible, rendered only with content or an error)
1. **Header** — date, weekday, "Good morning" + one-sentence day shape ("3 meetings, first at 10:00 — clear until then."); generation timestamp.
2. **📅 Today** — event timeline (time, title, location/link, attendee count); callouts pinned on top (conflicts, unusually-early first event, missing join link, >3h back-to-back); all-day items in a compact strip.
3. **✉️ Needs attention** — ≤5 urgent emails: sender, subject, one-line gist, *why flagged*; each row [Open] + [Not urgent].
4. **💬 You were mentioned** — ≤10 mentions grouped by channel/thread: who, where, gist, [Open].
5. **📰 Your topics** — ≤5 headlines: headline, source, topic tag, [Read].
6. **Footer** — "That's everything." + [Adjust briefing]. No pull-to-refresh, nothing below.

## Capabilities
- Every item deep-links to its source (calendar event, mail thread, message permalink, article) via provider app else browser.
- Progressive render: sections appear as fetches complete, per-section skeleton loaders.
- Per-section feedback ("not urgent" / section tuning), stored locally.

## States
Loading (per-section skeletons) · Complete · Partial (one+ section in error row with one-tap fix) · All-clear (calm "nothing urgent" / whole-brief "all clear") · Sample (watermarked "SAMPLE — not yet live", from the wizard).

## Edge cases
- Section with nothing → single calm line, not hidden anxiety.
- Oversized day → caps + "and N more…" deep-link; never paginate.
- Token expired → section error row + reconnect; connection also flagged in Settings.
- Opened mid-generation → skeletons fill in; never a blank screen.
- Cached brief always viewable offline.

## Acceptance criteria
- [ ] Sections render in fixed order, caps enforced, every item deep-links.
- [ ] Every urgent email shows a human-readable flag reason.
- [ ] Empty day shows an "all clear" brief and the notification still fired.
- [ ] Single-source failure shows a partial brief with a one-tap fix; total failure is an honest error, never silent.
- [ ] TalkBack labels include flag reasons; WCAG AA contrast in both themes; cold-open <800ms from cache.
