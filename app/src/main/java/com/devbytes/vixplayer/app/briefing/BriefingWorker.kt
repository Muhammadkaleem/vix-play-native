package com.devbytes.vixplayer.app.briefing

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZonedDateTime

/**
 * Generates and posts the morning brief, then schedules the next day's fire. Woken by
 * [BriefingAlarmReceiver] at the alarm time. If the device was Doze-delayed past noon, the
 * brief is skipped (FR-16) rather than delivered stale — but the next fire is still registered.
 */
@HiltWorker
class BriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: BriefingRepository,
    private val generator: BriefingGenerator,
    private val notifier: BriefingNotifier,
    private val scheduler: BriefingScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = repository.current()
        if (config.isLive) {
            val zone = config.effectiveZone()
            val nowHour = ZonedDateTime.now(zone).hour
            if (nowHour < 12 && notifier.notificationsEnabled()) {
                notifier.post(generator.generate(zone), config.lockScreenDetail)
            }
            // Register the next enabled day regardless of whether we posted.
            scheduler.schedule(config)
        }
        return Result.success()
    }
}
