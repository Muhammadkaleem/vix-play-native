package com.devbytes.vixplayer.app.briefing

import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds a [Brief] from the calendar with purely local heuristics — no summarization backend.
 * The "day shape" sentence and callouts (conflicts, long back-to-back stretches) are computed,
 * not LLM-generated, which is exactly why the calendar slice needs no external service.
 */
@Singleton
class BriefingGenerator @Inject constructor(
    private val calendar: CalendarSource,
) {
    suspend fun generate(zone: ZoneId): Brief {
        val all = calendar.eventsForToday(zone)
        val allDay = all.filter { it.isAllDay }
        val timed = all.filterNot { it.isAllDay }.sortedBy { it.start }
        val fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(zone)

        val callouts = buildList {
            // Conflicts: overlapping timed events (cap at 3 lines).
            for (i in timed.indices) {
                for (j in i + 1 until timed.size) {
                    val a = timed[i]; val b = timed[j]
                    if (b.start < a.end && a.start < b.end) {
                        add(BriefCallout("Conflict: \"${a.title}\" and \"${b.title}\" overlap ${fmt.format(maxOf(a.start, b.start))}–${fmt.format(minOf(a.end, b.end))}."))
                    }
                }
            }
        }.take(3) + longestBackToBackCallout(timed, fmt)

        return Brief(
            daySummary = daySummary(timed, allDay, callouts.isNotEmpty(), fmt),
            callouts = callouts,
            timedEvents = timed,
            allDayEvents = allDay,
        )
    }

    /** Flags a continuous stretch of meetings (gaps < 15 min) longer than 3 hours. */
    private fun longestBackToBackCallout(timed: List<BriefEvent>, fmt: DateTimeFormatter): List<BriefCallout> {
        if (timed.size < 2) return emptyList()
        var runStart = timed.first().start
        var runEnd = timed.first().end
        var best: Pair<java.time.Instant, java.time.Instant>? = null
        for (e in timed.drop(1)) {
            if (Duration.between(runEnd, e.start).toMinutes() <= 15) {
                if (e.end > runEnd) runEnd = e.end
            } else {
                if (Duration.between(runStart, runEnd).toHours() >= 3) best = runStart to runEnd
                runStart = e.start; runEnd = e.end
            }
        }
        if (Duration.between(runStart, runEnd).toHours() >= 3) best = runStart to runEnd
        return best?.let { listOf(BriefCallout("Back-to-back from ${fmt.format(it.first)} to ${fmt.format(it.second)} — plan a break.")) } ?: emptyList()
    }

    private fun daySummary(
        timed: List<BriefEvent>,
        allDay: List<BriefEvent>,
        hasConflict: Boolean,
        fmt: DateTimeFormatter,
    ): String {
        if (timed.isEmpty() && allDay.isEmpty()) return "All clear today — nothing on your calendar."
        if (timed.isEmpty()) return "No timed meetings today. ${allDay.size} all-day item${plural(allDay.size)}."
        val count = timed.size
        val first = fmt.format(timed.first().start)
        val base = "$count meeting${plural(count)} today, first at $first."
        return if (hasConflict) "$base One conflict to sort out." else base
    }

    private fun plural(n: Int) = if (n == 1) "" else "s"
}
