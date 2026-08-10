package com.devbytes.vixplayer.app.briefing

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the next morning fire as an exact alarm (so delivery lands within ~±5 min of the
 * chosen local time), falling back to an inexact allow-while-idle alarm when the platform
 * withholds exact-alarm permission (API 33+ default). The alarm wakes [BriefingAlarmReceiver],
 * which hands off to [BriefingWorker] for the actual generation.
 */
@Singleton
class BriefingScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    /** True if the OS lets us schedule exact alarms; false ⇒ delivery time may drift a few minutes. */
    fun canDeliverExactly(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** (Re)schedules the next fire, or cancels if the briefing isn't live / has no upcoming day. */
    fun schedule(config: BriefingConfig) {
        val fireAt = nextFireMillis(config)
        if (fireAt == null) {
            cancel()
            return
        }
        val pi = pendingIntent()
        if (canDeliverExactly()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } else {
            // Inexact window — disclosed to the user in settings.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    /** Epoch millis of the next enabled delivery time strictly after now, or null. */
    fun nextFireMillis(config: BriefingConfig, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        if (!config.isLive) return null
        val zone = config.effectiveZone()
        val nowZ = now.withZoneSameInstant(zone)
        val today = nowZ.toLocalDate()
        for (i in 0..7) {
            val day = today.plusDays(i.toLong())
            if (!config.days.contains(day.dayOfWeek)) continue
            val fire = day.atTime(config.deliveryTime).atZone(zone)
            if (fire.isAfter(nowZ)) return fire.toInstant().toEpochMilli()
        }
        return null
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BriefingAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 7301
    }
}
