package com.devbytes.vixplayer.app.briefing

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** The sources a briefing can draw from. Only CALENDAR is live in v1; the rest need OAuth
 *  accounts + backends (summarization / news) and are shown as "coming soon" in setup. */
enum class SourceType { CALENDAR, EMAIL, MENTIONS, NEWS }

/** How the delivery time tracks the clock while traveling. */
enum class ZoneMode { FOLLOW_DEVICE, FIXED_ZONE }

/**
 * Persisted briefing configuration. The calendar is a local, always-re-readable provider, so
 * the brief itself is regenerated on demand — only this config is stored (see BriefingRepository).
 *
 * [sampleApprovedAt] == 0 ⇒ the user has not approved a sample, so **no schedule may ever fire**
 * (FR-6). It is set exactly once, when the wizard's "Go live" is tapped.
 */
data class BriefingConfig(
    val enabled: Boolean = false,
    val deliveryMinuteOfDay: Int = 8 * 60,       // 08:00 default
    val daysMask: Int = MON_TO_FRI,
    val zoneMode: ZoneMode = ZoneMode.FOLLOW_DEVICE,
    val fixedZoneId: String? = null,
    val lockScreenDetail: Boolean = false,
    val sampleApprovedAt: Long = 0L,
) {
    val deliveryTime: LocalTime get() = LocalTime.of(deliveryMinuteOfDay / 60, deliveryMinuteOfDay % 60)

    val days: Set<DayOfWeek>
        get() = DayOfWeek.entries.filter { (daysMask shr (it.value - 1)) and 1 == 1 }.toSet()

    val isLive: Boolean get() = enabled && sampleApprovedAt > 0L

    /** The zone the delivery time is interpreted in, honoring the travel mode. */
    fun effectiveZone(): ZoneId = when (zoneMode) {
        ZoneMode.FIXED_ZONE -> fixedZoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        ZoneMode.FOLLOW_DEVICE -> ZoneId.systemDefault()
    }

    companion object {
        // Bit i (0-based) = DayOfWeek.of(i+1): Monday=bit0 … Sunday=bit6.
        const val MON_TO_FRI = 0b0011111
        fun maskOf(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    }
}

/** A single calendar event as the brief renders it. */
data class BriefEvent(
    val title: String,
    val start: Instant,
    val end: Instant,
    val isAllDay: Boolean,
    val location: String?,
    val hasConferenceLink: Boolean,
    val attendeeCount: Int,
)

/** A pinned "worth noticing" line (conflict, unusually-early first event, long back-to-back). */
data class BriefCallout(val text: String)

/** A generated briefing. Built live from the calendar; never persisted in v1. */
data class Brief(
    val daySummary: String,
    val callouts: List<BriefCallout>,
    val timedEvents: List<BriefEvent>,
    val allDayEvents: List<BriefEvent>,
) {
    val isEmpty: Boolean get() = timedEvents.isEmpty() && allDayEvents.isEmpty()
}
