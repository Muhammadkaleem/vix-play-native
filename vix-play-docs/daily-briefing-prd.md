# Daily Briefing — PRD

**Feature:** AI Daily Briefing (morning brief before start-of-day)
**Module:** `:feature:briefing` (new) · **Route:** `/briefing`, `/briefing/setup`, `/briefing/settings`
**Priority:** P1 (differentiator) · **Status:** Draft v1.0 · **Date:** 2026-08-05
**Owner:** Muhammad Kaleem (UserWise) · **Doc convention:** follows `vix-play-docs` PRD template (Purpose · Entry points · Layout · States · Interactions · Edge cases · Acceptance criteria), extended to a full engineering-ready spec.

---

## 1. Overview

A **Daily Briefing** that reaches the user every morning *before they start their day*: a single, scannable brief assembled from the sources they choose — today's calendar, anything urgent in email, messages they've been tagged in, and news topics they follow — delivered on a schedule they control, in their own time zone.

Setup is a guided, conversational wizard. The assistant asks what the sources should be, confirms the user's time zone and delivery time, connects the accounts it needs (OAuth), generates a **sample briefing for review**, and only goes live after the user explicitly approves the sample.

### One-line pitch
> "Open your phone at 8 AM and the day is already summarized — your meetings, the two emails that actually matter, where you were mentioned overnight, and the three headlines you care about."

### Why now
The briefing is a daily-habit anchor: it gives the app a guaranteed, valuable touch-point every morning, independent of whether the user has content to play. Daily-habit surfaces are the strongest known driver of D30 retention (a stated success metric in `01-vision-scope.md`).

---

## 2. Background & Problem Statement

Mornings are fragmented. To answer "what does my day look like?" a user opens four apps — calendar, email, chat, and a news feed — and each one is optimized to *hold* attention, not to *release* it. The result:

- **Calendar**: shows events but not conflicts, prep gaps, or travel time.
- **Email**: 40 unread; 2 actually urgent; no triage.
- **Chat / mentions**: overnight tags buried in channel noise.
- **News**: infinite feed engineered against "get in, get out."

There is no single surface that answers, in under two minutes, *"What do I need to know before I start?"* — assembled from the user's own accounts, on the user's schedule, with nothing designed to keep them scrolling.

### Product principles (inherited from Vision & Scope)
1. **Respectful** — the briefing gets the user out, not in. No infinite feed, no engagement bait.
2. **User-controlled** — every source is opt-in; nothing is read that wasn't explicitly connected.
3. **Preview before live** — the user sees exactly what a briefing looks like with their real data before the first scheduled delivery.

---

## 3. Goals & Non-Goals

### Goals (v1)
- G1. Deliver a briefing **reliably at the user's chosen local time**, every scheduled day, ≥ 99% of days.
- G2. Support four source types at launch: **Calendar**, **Email (urgent only)**, **Mentions/tags**, **Followed news**.
- G3. A **setup wizard** that clarifies sources, confirms time zone and delivery time, connects accounts, and shows a **sample briefing** before activation.
- G4. Briefing consumable in **under 2 minutes**; every item links out to its source of truth (open the event, the email thread, the message, the article).
- G5. Graceful degradation: a partially available briefing (e.g., email token expired) still delivers, with a clear "source unavailable" note and a one-tap fix.

### Non-Goals (v1)
- NG1. No evening / multiple-per-day briefings (P2).
- NG2. No reply/compose actions from the briefing (open-in-source-app only; quick actions are P2).
- NG3. No shared or team briefings.
- NG4. No general-purpose news reader — headlines are capped, topic-scoped, and link out.
- NG5. No always-listening / ambient assistant behavior; the briefing runs once per scheduled fire.
- NG6. No web/desktop surface (Android first, consistent with the codebase).

---

## 4. Target Users & Personas

| Persona | Description | What the briefing must nail |
|---|---|---|
| **The Back-to-Back PM** ("Sana") | 6–9 meetings/day, two email accounts, active in Slack. Starts at 9:00, checks phone ~8:00. | Calendar conflicts + prep gaps; urgent-email precision (false urgency destroys trust); mention triage. |
| **The Maker** ("Hamza") | Engineer, 1–2 meetings/day, hates email, tagged in code reviews and threads. | Mentions first; a *short* brief — most days "nothing urgent" is the right answer, and the brief must be comfortable saying so. |
| **The Founder/Generalist** ("Muhammad") | Everything matters a little; follows industry news; timezone-hopping travel. | Timezone correctness while traveling; news topics that stay on-topic; a single trust-worthy morning ritual. |

---

## 5. Success Metrics

| Metric | Target (90 days post-launch) | Instrumentation |
|---|---|---|
| Delivery reliability (briefing generated + notified within ±5 min of schedule) | ≥ 99.0% of scheduled fires | `briefing_fire_scheduled` vs `briefing_delivered` |
| Setup completion (started wizard → activated) | ≥ 60% | funnel events, per step |
| Sample→activate conversion | ≥ 80% of users who see a sample | `briefing_sample_shown` → `briefing_activated` |
| D7 open rate (opened briefing ≥ 5 of first 7 days) | ≥ 45% of activated users | `briefing_opened` |
| Median time-in-brief | 45–120 s (below 30 s or above 4 min are both failure signals) | session timing |
| Urgent-email precision (user marks "not urgent" feedback) | ≤ 10% of urgent items flagged wrong | inline feedback control |
| Source-connection health (auto-recovered or user-fixed within 48 h) | ≥ 90% of token expiries | connection-state events |
| Retention lift | +X pp D30 vs matched non-briefing cohort | A/B holdout |

---

## 6. User Stories

**Setup**
- US-1: As a new user, I want to be *asked* which sources my briefing should draw from — today's calendar, urgent email, messages I've been tagged in, news I follow — so the brief reflects what I actually care about.
- US-2: As a user, I want the app to confirm my time zone and ask when my morning starts, so the brief arrives *before* I begin, not during.
- US-3: As a user, I want to connect only the accounts I choose, understand exactly what will be read from each, and be able to skip any of them.
- US-4: As a user, I want to see a **sample briefing built from my real data** and approve it before anything goes live.

**Daily use**
- US-5: As a user, I want a notification at my chosen time that opens a briefing I can read in under two minutes.
- US-6: As a user, I want conflicts and unusual items (first meeting earlier than normal, double-booking, red-eye flight) called out, not just listed.
- US-7: As a user, I want each item to deep-link to its source (calendar event, email thread, message, article).
- US-8: As a user with a quiet day, I want the brief to say "nothing urgent" instead of manufacturing content.

**Management**
- US-9: As a user, I want to change sources, time, days, or time zone behavior at any time from settings.
- US-10: As a user, I want to pause the briefing (vacation) and resume without re-setup.
- US-11: As a user, I want to disconnect a source and have its data purged.
- US-12: As a traveling user, I want to choose whether the brief follows my device's time zone or stays fixed to home.

---

## 7. User Experience

### 7.1 Entry points
- **Setup:** post-onboarding suggestion card ("Get a morning briefing") · Settings → Daily Briefing · home-surface promo card (dismissible, shown max twice).
- **Daily:** scheduled notification → `/briefing` · Briefing card on home surface (until noon local) · Settings → Daily Briefing → "View today's brief."

### 7.2 Setup wizard — conversational flow

The wizard is a full-screen, step-by-step conversational flow (Compose). Every step is skippable except schedule confirmation. Progress is persisted per step; abandoning and returning resumes in place.

**Step 0 — Intent.**
"Every morning, before you start, I can put together a short brief. Let's set it up — it takes about two minutes."

**Step 1 — Sources (the ask).**
"What should your briefing pull from?" — multi-select cards, each with a one-line scope statement:

| Source card | Scope statement shown to user |
|---|---|
| 📅 **Today's calendar** | "Today's events, conflicts, and gaps — read-only." |
| ✉️ **Urgent email** | "Only messages that look urgent or time-sensitive. I won't summarize your whole inbox." |
| 💬 **Mentions & tags** | "Messages where you were @-mentioned or tagged since yesterday evening." |
| 📰 **News you follow** | "A few headlines on topics you pick — capped at 5, no feed." |

At least one source must be selected to proceed. Selecting **News** expands an inline topic picker (free-text topics + suggested chips; 1–10 topics).

**Step 2 — Time zone & schedule (the confirm).**
- Detected zone shown explicitly: "You're in **Asia/Karachi (UTC+5)** — right?" [Yes / Change]
- "When should the brief reach you?" — time picker, default **08:00**, plus day-of-week selector (default Mon–Fri).
- Travel behavior: "If you travel, should the brief follow your phone's time zone or stay fixed?" [Follow my phone (default) / Stay fixed]

**Step 3 — Connections (connect what you need).**
For each selected source that requires an account, a connection row: provider icons (Google / Microsoft for calendar+email; Slack for mentions; news needs no account), OAuth in a Custom Tab, minimal read-only scopes, per-provider plain-language scope recap *before* the OAuth screen. Each connection is independently skippable — skipping drops that source with an inline note ("You can add this later in settings").

**Step 4 — Sample briefing (show before it goes live).**
Immediately generate a **real sample** from the connected sources, rendered exactly as the daily brief will look, watermarked "SAMPLE — not yet live." Beneath it:
- "Look right?" → **[Go live ✓]** · **[Adjust sources]** (back to Step 1) · **[Adjust time]** (back to Step 2)
- Per-section feedback affordance ("too much / just right / missing something") to tune section caps.
**The schedule is only registered after the user taps Go live.** If sample generation partially fails (one source erroring), the sample renders with that section in its error state — the user sees the truthful worst case, not an idealized mock.

**Step 5 — Confirmation.**
"Your briefing goes live tomorrow at 08:00 (Asia/Karachi), Mon–Fri." + [View sample again] [Settings].

### 7.3 The briefing itself — layout

Single scrollable screen, dark-first (indigo→violet design system), sections in fixed order, each section collapsible and only rendered if it has content or an error:

1. **Header** — date, weekday, "Good morning" + one-sentence day shape ("3 meetings, first at 10:00 — your morning is clear until then."). Generation timestamp.
2. **📅 Today** — timeline of events (time, title, location/link, attendee count). Callouts pinned on top: conflicts/double-bookings, first event earlier than the user's typical first event, events with no join link, back-to-back stretches > 3 h, all-day items.
3. **✉️ Needs attention** — max **5** urgent emails: sender, subject, one-line gist, *why it was flagged* ("deadline mentioned: today", "reply requested by your manager"). Each row: [Open] + [Not urgent] feedback.
4. **💬 You were mentioned** — max **10** mentions since the previous brief (or last 18 h on day one), grouped by channel/thread: who, where, one-line gist, [Open].
5. **📰 Your topics** — max **5** headlines across followed topics: headline, source, topic tag, [Read].
6. **Footer** — "That's everything. Have a good one." + [Adjust briefing] link. No pull-to-refresh feed. Nothing below the footer.

**Empty behaviors** (respect the user's time): section with nothing → single calm line ("No urgent email 🎉" / "No mentions overnight"). Entire brief empty → short "All clear today — first event at 14:00" brief; the notification still fires (silence is information).

### 7.4 Notification
- Fires at scheduled time; title "Your morning brief"; body = the one-sentence day shape (no sensitive detail on the lock screen by default; a "show details on lock screen" toggle exists in settings).
- Channel: `briefing` (own notification channel so the user can tune importance independently of playback controls).
- Tapping opens `/briefing`. Notification auto-dismisses at 14:00 local if untouched.

### 7.5 Settings (`/briefing/settings`)
Sources on/off · per-source account management (reconnect, disconnect+purge) · news topics editor · time, days, time zone mode · lock-screen detail toggle · pause/resume · regenerate today's brief · delete briefing history · turn off entirely (with data-purge confirmation).

---

## 8. Functional Requirements

### 8.1 Setup & activation
- **FR-1** The wizard MUST ask the user to select sources (calendar / urgent email / mentions / news) via explicit multi-select; no source is on by default.
- **FR-2** The wizard MUST display the auto-detected IANA time zone and require explicit confirmation or manual override.
- **FR-3** The wizard MUST capture delivery time (minute granularity) and delivery days (any subset of weekdays; default Mon–Fri).
- **FR-4** The wizard MUST capture travel behavior: `FOLLOW_DEVICE` (default) or `FIXED_ZONE`.
- **FR-5** Each account connection MUST show a plain-language scope recap before launching OAuth, request read-only minimal scopes, and be individually skippable.
- **FR-6** The wizard MUST generate a sample briefing from real connected data and require explicit "Go live" approval before registering the schedule. No scheduled brief may ever fire without an approved sample.
- **FR-7** Wizard state MUST persist across process death; returning users resume at their last step.

### 8.2 Sources
- **FR-8 Calendar** — read today's events (00:00–23:59 local) from all connected calendars the user left enabled; detect and flag: overlapping events, first-event-earlier-than-typical (rolling 4-week median), missing conferencing link on virtual-looking meetings, back-to-back stretches > 3 h.
- **FR-9 Email (urgent only)** — scan messages received since the last brief (first run: last 24 h); classify urgency from deadline language, sender importance (frequency + manual VIP list), direct questions/asks, thread the user recently replied on, and calendar proximity (email referencing a meeting < 24 h away). Hard cap 5 items; each item MUST carry a human-readable flag reason. The full inbox MUST NOT be summarized.
- **FR-10 Mentions** — fetch @-mentions/tags of the user since the last brief from connected chat workspaces; group by conversation; cap 10; each item deep-links to the message.
- **FR-11 News** — fetch headlines for the user's topics (1–10 topics) from the news backend; rank by topical relevance and recency; global cap 5; every headline links out; no engagement-optimized ordering.
- **FR-12** Every rendered item MUST deep-link to its source of truth (calendar event URI, mail thread, chat message permalink, article URL) via the provider app when installed, else browser.

### 8.3 Scheduling & delivery
- **FR-13** The brief MUST be generated and the notification posted within **±5 minutes** of the scheduled local time on scheduled days.
- **FR-14** Generation SHOULD begin at T−10 min (prefetch window) so content is ready at T; content staleness at delivery MUST be ≤ 15 min per source.
- **FR-15** In `FOLLOW_DEVICE` mode, a device time-zone change MUST reschedule the next fire to the new zone's local time; in `FIXED_ZONE` mode the fire time stays anchored to the configured zone. `TIMEZONE_CHANGED` / `TIME_SET` / `BOOT_COMPLETED` broadcasts MUST trigger re-registration.
- **FR-16** If the device is offline/Doze-delayed at T, the system MUST deliver as soon as constraints allow, marked "Generated at HH:MM" if > 15 min late; a brief MUST NOT be delivered after 12:00 local (skip with a silent `briefing_skipped_late` event instead).
- **FR-17** Exactly one brief per scheduled day; regeneration via settings replaces the day's brief in place.
- **FR-18** Pause/resume MUST take effect from the next scheduled fire and never require reconnection.

### 8.4 Content generation
- **FR-19** Summarization (day-shape sentence, email gists, mention gists) runs on the summarization service; requests carry only the minimum excerpt needed and are not retained server-side beyond request lifetime (see §10).
- **FR-20** Per-source failure MUST NOT abort the brief: render available sections + a per-source error row with a one-tap fix (reconnect / retry).
- **FR-21** "Not urgent" feedback MUST demote similar future items (sender/pattern-level signal, stored locally) and be reflected within the next 2 briefs.
- **FR-22** Briefing history: keep the last **14** briefs locally, viewable from settings; older briefs auto-purge.

### 8.5 Connections & lifecycle
- **FR-23** OAuth tokens MUST be stored encrypted (Android Keystore–wrapped; consistent with `NetworkServer.encryptedPassword` precedent); refresh silently; on refresh failure mark the source `NEEDS_ATTENTION` and surface it in the next brief's section header and in settings — never fail silently.
- **FR-24** Disconnecting a source MUST revoke the token (best-effort provider revocation), delete cached source data, and remove its section from future briefs immediately.
- **FR-25** Turning the feature off MUST cancel schedules, revoke all briefing tokens, and purge briefing data, with a confirmation dialog stating exactly that.

---

## 9. Non-Functional Requirements

| Area | Requirement |
|---|---|
| **Reliability** | ≥ 99% on-time fire rate. Scheduling via `AlarmManager.setExactAndAllowWhileIdle` (with `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` handling per API level) driving a WorkManager-executed generation job; fallback inexact window if exact-alarm permission is denied, disclosed to the user. |
| **Performance** | Brief screen cold-open < 800 ms from cached content; generation job ≤ 60 s p95; screen scroll at 60 fps. |
| **Battery / data** | One generation job per day + T−10 prefetch; total network < 2 MB/day typical; no background polling outside the generation window; WorkManager constraints `NETWORK_CONNECTED`. |
| **Privacy** | Read-only scopes; on-device caching encrypted at rest; summarization excerpts minimized; no source content in analytics events (metadata/counters only); lock-screen redaction by default. See §10. |
| **Security** | Tokens in Keystore-wrapped storage; TLS everywhere; no tokens/PII in logs; briefing content excluded from auto-backup (`fullBackupContent` rules). |
| **Offline** | Last generated brief always viewable offline; generation retries with backoff (max 3, then late-skip rule FR-16). |
| **Accessibility** | Full TalkBack labeling incl. flag reasons; min 48 dp touch targets; dynamic type up to 200%; section landmarks for screen-reader navigation; WCAG AA contrast in both themes. |
| **Localization** | All strings externalized; brief renders in app language; RTL-safe layout; date/time via `java.time` with the user's zone and locale. Urgency classification quality is EN-first at launch, other languages best-effort (disclosed). |
| **Compatibility** | minSdk/targetSdk aligned with app; Android 13+ notification runtime permission flow handled in wizard Step 2 (rationale → system dialog, consistent with `00-splash-onboarding.md`). |

---

## 10. Privacy & Data Handling (explicit)

| Data | Where it lives | Retention |
|---|---|---|
| OAuth tokens | Keystore-encrypted local storage | Until disconnect / feature-off (then revoked + deleted) |
| Fetched source items (events, email metadata + excerpts, mentions, headlines) | Local Room cache, encrypted at rest | Rolling 14 days (FR-22) |
| Summarization excerpts | Transient request to summarization service | Not retained beyond request |
| Feedback signals ("not urgent", section tuning) | Local only | Until feature-off |
| Analytics | Counters + timings + error codes only; never content, senders, titles, or topics-as-text | Standard analytics retention |

Plain-language privacy sheet linked from wizard Step 3 and settings. Google API Services User Data Policy (Limited Use) compliance is a launch gate for Gmail/Calendar scopes — restricted-scope verification lead time is owned by the eng lead and MUST be started in M1 (see §13, §15).

---

## 11. Technical Design (summary)

### 11.1 Module & architecture
New Gradle module `:feature:briefing`, consistent with existing structure:

```
:feature:briefing
├── setup/        SetupWizardScreen, SetupViewModel, per-step state machine
├── connections/  ConnectorRegistry, CalendarConnector, EmailConnector,
│                 MentionsConnector, NewsConnector (common Connector interface),
│                 OAuthManager (Custom Tabs + AppAuth), TokenStore (Keystore)
├── engine/       BriefingGenerator (fan-out to connectors → aggregate → summarize),
│                 UrgencyClassifier, CalendarInsights, Ranker/Caps
├── schedule/     BriefingScheduler (exact alarm → WorkManager job),
│                 TimezoneMonitor (TIMEZONE_CHANGED/TIME_SET/BOOT receivers)
├── ui/           BriefingScreen, section composables, sample watermark,
│                 BriefingNotification (channel `briefing`)
└── settings/     BriefingSettingsScreen
```

MVVM + StateFlow, Hilt DI, Coroutines/Flow — identical data-flow pattern to the player (`UI → ViewModel → Repository → Room/DataStore/network`).

**Connector interface (sketch):**
```kotlin
interface Connector {
    val type: SourceType
    suspend fun healthCheck(): ConnectionState          // CONNECTED | NEEDS_ATTENTION | DISCONNECTED
    suspend fun fetch(window: FetchWindow): SourceResult // items or typed failure
}
```
`BriefingGenerator` fans out `fetch()` calls in parallel (`supervisorScope` — one failure never cancels siblings), applies caps/ranking, calls the summarization service for gists, and persists a `BriefingRun`.

### 11.2 Data models (Room / DataStore)

```kotlin
// DataStore (versioned JSON, kotlinx.serialization — matches existing config conventions)
data class BriefingConfig(
  val enabled: Boolean, val paused: Boolean,
  val deliveryTime: LocalTime, val days: Set<DayOfWeek>,
  val zoneMode: ZoneMode,                 // FOLLOW_DEVICE | FIXED_ZONE
  val fixedZone: ZoneId?,
  val sources: Set<SourceType>,           // CALENDAR | EMAIL | MENTIONS | NEWS
  val newsTopics: List<String>,           // 1..10
  val lockScreenDetail: Boolean = false,
  val sampleApprovedAt: Instant?,         // null ⇒ never fire (FR-6)
  val schemaVersion: Int,
)

@Entity data class ConnectedAccount(
  @PrimaryKey val id: String, val sourceType: SourceType, val provider: String,
  val accountLabel: String, val state: ConnectionState, val connectedAt: Long,
)

@Entity data class BriefingRun(
  @PrimaryKey val id: String, val forDate: LocalDate, val generatedAt: Instant,
  val status: RunStatus,                  // COMPLETE | PARTIAL | FAILED | SKIPPED_LATE
  val daySummary: String?,                // the one-sentence day shape
)

@Entity data class BriefingItem(
  @PrimaryKey val id: String, val runId: String, val sourceType: SourceType,
  val orderInSection: Int, val title: String, val gist: String?,
  val flagReason: String?,                // urgency "why" (FR-9)
  val deepLink: String, val encryptedPayload: ByteArray?, // cached detail, encrypted
)
```

### 11.3 Scheduling flow
1. `Go live` → `BriefingScheduler.register(config)` → exact alarm at next fire − 10 min.
2. Alarm → expedited WorkManager job: fan-out fetch → classify → summarize → persist `BriefingRun` → post notification at T (or immediately if already past T).
3. Job completion (or terminal failure) → schedule next fire.
4. `TimezoneMonitor` re-registers on zone/time/boot changes per FR-15.
5. Late path per FR-16.

### 11.4 Dependencies & open technical choices
- OAuth: AppAuth + Custom Tabs; provider SDKs only where REST is impractical.
- Summarization: existing backend LLM endpoint (**OQ-2**: confirm endpoint, quota, and on-device fallback ambition).
- News: backend news API with topic search (**OQ-3**: provider selection — licensing cost vs. topic quality).
- Urgency v1 is heuristic + LLM-assist; pure on-device model is P2.

---

## 12. Edge Cases

| # | Case | Behavior |
|---|---|---|
| E1 | Time-zone change while traveling | Per `zoneMode` (FR-15). On first fire in a new zone, header notes "Adjusted to your current time zone." |
| E2 | DST transitions | `ZonedDateTime` scheduling; nonexistent local time → fire at next valid instant; ambiguous time → earlier offset. Unit-tested explicitly. |
| E3 | Device off / Doze at fire time | Deliver late up to 12:00 local, labeled; else skip silently (FR-16). |
| E4 | Exact-alarm permission denied (API 31+) | Fall back to inexact window; settings shows "delivery may vary by a few minutes" + link to grant. |
| E5 | Token expired / revoked overnight | Section renders error row + one-tap reconnect; push nothing scary; connection also flagged in settings (FR-23). |
| E6 | Provider API outage / rate limit | Retry ×3 with backoff inside job budget; then partial brief (FR-20) with "couldn't reach X." |
| E7 | Zero content everywhere | "All clear" brief; notification still fires (silence is information). |
| E8 | Oversized day (30 events, 200 mentions) | Caps + "and N more…" row deep-linking to source app. Never paginate the brief itself. |
| E9 | Two calendars, duplicated events | Dedupe by (title, start, end) fuzzy match across accounts. |
| E10 | All-day events | Rendered in a compact strip above the timeline, never as conflicts. |
| E11 | Notifications permission revoked later | Brief still generates; home-surface card becomes primary; settings shows a fix-it row. |
| E12 | User opens brief mid-generation | Progressive render: sections appear as their fetches complete; skeleton loaders per section. |
| E13 | Sample generation fails entirely | Wizard shows honest failure + retry; activation blocked (never go live sight-unseen, FR-6). |
| E14 | Locale/language change | Next brief renders in new locale; cached briefs stay as generated. |
| E15 | Account deleted at provider | `NEEDS_ATTENTION` → user prompt to disconnect or re-auth; auto-purge after 30 days unresolved. |
| E16 | App update changes schema | Versioned config JSON + Room migrations (existing convention); a failed migration must never drop tokens silently. |
| E17 | Backup/restore to new device | Tokens are Keystore-bound ⇒ non-restorable by design; restored config marks all sources `NEEDS_ATTENTION` and prompts reconnection; schedule stays paused until one source reconnects. |
| E18 | Private/incognito expectations | Briefing data honors "clear history" in Privacy settings (purges `BriefingRun`/`BriefingItem`). |

---

## 13. Analytics & Instrumentation

Events (no content payloads — counters, durations, error codes only):

`briefing_setup_started/step_completed{step}/abandoned{step}` · `briefing_source_selected{type}` · `briefing_connection_started/succeeded/failed{provider,error}` · `briefing_sample_shown/regenerated` · `briefing_activated` · `briefing_fire_scheduled/delivered{latency_bucket}/skipped_late` · `briefing_generation{duration_bucket,status,sources_ok,sources_failed}` · `briefing_opened{via}` · `briefing_item_opened{source_type}` · `briefing_feedback{type}` · `briefing_paused/resumed/disabled` · `briefing_connection_state_changed{state}`

Dashboards: delivery reliability (G1), setup funnel (G3), engagement (G4), source health. Alerts: fire-rate < 97% daily; connection-failure spike per provider; generation p95 > 60 s.

---

## 14. Acceptance Criteria (launch gate)

- [ ] Wizard asks sources, confirms detected time zone with override, captures time/days/travel mode, and connects each selected account with a pre-OAuth scope recap; every connection individually skippable.
- [ ] A real-data sample briefing is shown and **explicit "Go live" approval is required**; no schedule exists before approval (verified: kill app after sample, no fire occurs).
- [ ] Brief delivers within ±5 min of scheduled local time on scheduled days, ≥ 99% in a 14-day dogfood across Doze/reboot/airplane-mode matrix.
- [ ] Time-zone matrix passes: FOLLOW_DEVICE reschedules on zone change; FIXED_ZONE doesn't; DST spring-forward/fall-back unit tests green.
- [ ] Each source renders correctly, caps enforced (5 email / 10 mentions / 5 news), every item deep-links, every urgent email shows a flag reason.
- [ ] Single-source failure produces a partial brief with one-tap fix; total failure produces honest error state; nothing fails silently.
- [ ] Disconnect purges source data + revokes token; feature-off cancels schedule + purges everything (verified via DB inspection).
- [ ] Empty day renders "all clear" brief; notification still fires.
- [ ] No source content appears in logs or analytics (audited).
- [ ] TalkBack pass on wizard + brief; lock-screen redaction default verified.
- [ ] Google restricted-scope (Gmail) verification approved before production rollout.

---

## 15. Rollout & Milestones

| Phase | Scope | Exit criteria |
|---|---|---|
| **M1 — Foundations** (2 wk) | Module scaffold, `BriefingConfig`/Room schema, scheduler + timezone monitor with full unit-test matrix, notification channel. **Start Google OAuth verification.** | Scheduler test matrix green incl. DST + reboot. |
| **M2 — Connectors** (3 wk) | Connector interface; Calendar (Google/MS), Email urgency v1 (Gmail first), Mentions (Slack), News backend; token store + health states. | Each connector fetches + degrades correctly against fault-injection tests. |
| **M3 — Generation & UI** (3 wk) | `BriefingGenerator` fan-out, caps/ranking, summarization integration, brief screen, progressive render, deep links. | End-to-end brief from real accounts in dogfood. |
| **M4 — Wizard & sample** (2 wk) | Full setup flow, scope recaps, sample generation + approval gate, settings screen. | Setup funnel instrumented; sample-blocks-schedule verified. |
| **M5 — Hardening & beta** (2 wk) | Doze/OEM-killer matrix (incl. aggressive OEMs: Xiaomi/Samsung battery optimizers), a11y pass, analytics dashboards, closed beta. | ≥ 99% delivery over 14 dogfood days; beta NPS ≥ target. |
| **GA** | Staged rollout 5→20→100% behind remote flag `briefing_enabled`; holdout cohort for retention measurement. | Alert thresholds quiet for 7 days at 20%. |

Kill-switch: remote flag can disable scheduling globally (existing briefs remain viewable).

---

## 16. Open Questions

| # | Question | Owner | Needed by |
|---|---|---|---|
| OQ-1 | Provider launch set: Google-only first (Calendar+Gmail) with MS/Slack fast-follow, or all four at GA? | Product | M2 start |
| OQ-2 | Summarization backend: endpoint, cost/user/day budget, and whether an on-device fallback is a v1 requirement or P2. | Eng lead | M1 |
| OQ-3 | News provider (licensing cost vs topic quality vs building on an existing backend aggregator). | Product + BD | M2 |
| OQ-4 | Is Daily Briefing part of **Pro** (differentiator bundle per `monetization.md`), free with caps, or fully free as a retention play? Recommendation: free during beta, decide from retention-lift data. | Product | GA − 4 wk |
| OQ-5 | Weekend behavior default: off (Mon–Fri) confirmed, or ship a lighter "weekend edition" toggle at GA? | Design | M4 |
| OQ-6 | "Typical first meeting" baseline (FR-8) cold-start: skip the callout for the first 2 weeks, or seed from work-hours question in wizard? | Design + Eng | M3 |
| OQ-7 | Mentions source breadth: Slack-only v1, or generic via a second provider (Teams) — affects connector abstraction depth. | Eng | M2 |

---

## 17. Appendix — Sample Briefing (illustrative)

> **Wednesday, Aug 5 — Good morning ☀️**
> *3 meetings today, first at 10:00. One conflict this afternoon. Two emails need a look before standup.*
>
> **📅 Today**
> ⚠️ **Conflict:** "Design review" and "1:1 with Areeba" overlap 15:00–15:30.
> 10:00–10:30 · Standup · Meet link ↗
> 13:00–14:00 · Sprint planning · Room 2 · 8 people ↗
> 15:00–16:00 · Design review · Meet link ↗
>
> **✉️ Needs attention**
> **Fatima (Client X)** — "Contract redlines — need signature *today*" — *flagged: deadline is today* → Open ↗
> **Play Console** — "Policy update requires action by Aug 8" — *flagged: dated action required* → Open ↗
> *[Not urgent?]*
>
> **💬 You were mentioned**
> **#vix-play-release** — Areeba: "@kaleem can you confirm the 0.5 beta cut includes the EQ fix?" → Open ↗
> **PR #214** — Bilal tagged you for review on the subtitle-styling PR → Open ↗
>
> **📰 Your topics**
> *Android* — "Android 17 beta 2 adds new media performance class" — Android Police ↗
> *AI* — "On-device summarization models cross a quality threshold" — The Verge ↗
>
> *That's everything. Have a good one.* · Adjust briefing

---

*Template lineage: `vix-play-docs/media-player-docs/screens/README.md` (Purpose · Entry · Layout · States · Interactions · Edge cases · Acceptance criteria), extended with goals, metrics, FR/NFR numbering, privacy, analytics, rollout, and open questions for a full engineering-ready spec.*
