package com.devbytes.vixplayer.app.briefing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one live briefing source: today's events from the on-device Calendar Provider. No OAuth,
 * no backend — just the `READ_CALENDAR` runtime permission. This is the concrete stand-in for
 * the future `Connector` interface the account-based sources will implement.
 */
@Singleton
class CalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Today's events in [zone]. Empty if permission is missing (the screen shows a connect prompt). */
    suspend fun eventsForToday(zone: ZoneId): List<BriefEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()

        val today = LocalDate.now(zone)
        val begin = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
        )

        val events = mutableListOf<BriefEvent>()
        runCatching {
            CalendarContract.Instances.query(context.contentResolver, projection, begin, end)
        }.getOrNull()?.use { c ->
            val titleCol = c.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginCol = c.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endCol = c.getColumnIndex(CalendarContract.Instances.END)
            val allDayCol = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val locCol = c.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
            val descCol = c.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            while (c.moveToNext()) {
                val location = c.getStringOrNull(locCol)
                val description = c.getStringOrNull(descCol)
                events += BriefEvent(
                    title = c.getStringOrNull(titleCol)?.takeIf { it.isNotBlank() } ?: "(No title)",
                    start = Instant.ofEpochMilli(c.getLong(beginCol)),
                    end = Instant.ofEpochMilli(c.getLong(endCol)),
                    isAllDay = c.getInt(allDayCol) == 1,
                    location = location?.takeIf { it.isNotBlank() },
                    hasConferenceLink = looksLikeConference(location) || looksLikeConference(description),
                    attendeeCount = 0, // Attendees need a per-event query; deferred.
                )
            }
        }
        events
    }

    private fun looksLikeConference(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val t = text.lowercase()
        return "http" in t || "meet.google" in t || "zoom.us" in t || "teams.microsoft" in t
    }

    private fun android.database.Cursor.getStringOrNull(col: Int): String? =
        if (col >= 0 && !isNull(col)) getString(col) else null
}
