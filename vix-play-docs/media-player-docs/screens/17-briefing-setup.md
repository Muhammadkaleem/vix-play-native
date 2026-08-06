# Briefing Setup Wizard — PRD

**Route:** `/briefing/setup` · **Priority:** P1
**Entry:** Post-onboarding suggestion card ("Get a morning briefing") · Settings → Daily Briefing (when not yet set up) · home-surface promo card (dismissible, max twice).

## Purpose
A guided, conversational setup that clarifies sources, confirms time zone and delivery time, connects accounts, and shows a real sample — going live only after the user explicitly approves it. Feature spec: `features/daily-briefing.md`.

## Layout (full-screen, step-by-step; every step skippable except schedule; state persists per step across process death)
- **Step 0 — Intent.** "Every morning, before you start, I can put together a short brief. Takes about two minutes."
- **Step 1 — Sources.** Multi-select cards (Calendar / Urgent email / Mentions / News), each with a one-line scope statement; ≥1 required. Selecting News expands an inline topic picker (free text + suggested chips, 1–10 topics).
- **Step 2 — Time zone & schedule.** Detected IANA zone shown explicitly for confirm/override; time picker (default 08:00); day selector (default Mon–Fri); travel behavior `FOLLOW_DEVICE` (default) / `FIXED_ZONE`. Android 13+ notification-permission rationale → system dialog here.
- **Step 3 — Connections.** Per selected source needing an account: plain-language scope recap *before* OAuth, read-only minimal scopes, Custom Tab OAuth; each connection independently skippable (skip drops the source with an inline "add later in settings" note).
- **Step 4 — Sample.** Generate a real sample from connected data, rendered exactly as the daily brief, watermarked "SAMPLE — not yet live." Actions: **[Go live ✓]** · [Adjust sources] → Step 1 · [Adjust time] → Step 2, plus per-section "too much / just right / missing." Schedule registers **only** on Go live.
- **Step 5 — Confirmation.** "Goes live tomorrow at 08:00 (Asia/Karachi), Mon–Fri." + [View sample again] [Settings].

## States
In-progress (per-step, resumable) · Connecting (OAuth) · Connection skipped · Sample generating · Sample partial (a source in its error state — the truthful worst case, not a mock) · Sample failed (honest failure + retry, activation blocked) · Activated.

## Edge cases
- Sample generation fails entirely → honest failure + retry; never go live sight-unseen (FR-6).
- A connection skipped → source silently dropped from the sample and future briefs until added later.
- Exact-alarm permission denied (API 31+) → disclose "delivery may vary by a few minutes" with a grant link.
- Abandon + return → resume at last step with prior selections intact.

## Acceptance criteria
- [ ] Wizard asks sources, confirms detected zone with override, captures time/days/travel mode.
- [ ] Each selected account shows a pre-OAuth scope recap and is individually skippable.
- [ ] A real-data sample is shown and explicit "Go live" is required; no schedule exists before approval (verified: kill app after sample → no fire).
- [ ] Wizard state survives process death and resumes in place.
- [ ] Sample renders per-source error states truthfully rather than an idealized mock.
